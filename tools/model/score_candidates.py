from __future__ import annotations

import argparse
import math
import time
import unicodedata
from dataclasses import dataclass

import torch
from transformers import AutoModelForCausalLM, AutoTokenizer


DEFAULT_MODEL = "Qwen/Qwen2.5-0.5B"
DEFAULT_CANDIDATES = (
    "국민은행",
    "국민연금",
    "국민카드",
    "국민대학교",
    "국민건강보험",
    "국민과 함께하는 체조",
)

CONTEXT_DESCRIPTIONS = {
    "none": None,
    "bank_name": "은행명",
    "organization_name": "기관명",
}


@dataclass(frozen=True)
class CandidateScore:
    text: str
    average_log_probability: float

    @property
    def perplexity(self) -> float:
        return math.exp(-self.average_log_probability)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Score known Korean autocomplete candidates with a pretrained causal LM. "
            "The Android SDK will use this scoring shape instead of exposing free-form generation."
        )
    )
    parser.add_argument("--model", default=DEFAULT_MODEL)
    parser.add_argument("--prefix", default="국민")
    parser.add_argument(
        "--candidate",
        action="append",
        dest="candidates",
        help="Candidate to score. Repeat for multiple candidates.",
    )
    parser.add_argument(
        "--device",
        choices=("auto", "cpu", "cuda"),
        default="auto",
    )
    parser.add_argument(
        "--context",
        action="append",
        choices=tuple(CONTEXT_DESCRIPTIONS),
        dest="contexts",
        help=(
            "Prediction context to compare. Repeat for multiple contexts. "
            "The default compares none, bank_name, and organization_name."
        ),
    )
    return parser.parse_args()


def normalize(text: str) -> str:
    return unicodedata.normalize("NFC", text.strip())


def resolve_device(requested: str) -> str:
    if requested == "auto":
        return "cuda" if torch.cuda.is_available() else "cpu"
    if requested == "cuda" and not torch.cuda.is_available():
        raise RuntimeError("CUDA was requested but is not available.")
    return requested


def score_candidates(
    prefix: str,
    candidates: tuple[str, ...],
    context: str,
    tokenizer,
    model,
    device: str,
) -> list[CandidateScore]:
    description = CONTEXT_DESCRIPTIONS[context]
    if description is None:
        conditioning_text = f"자동완성({prefix}): "
    else:
        conditioning_text = f"{description} 자동완성({prefix}): "
    candidate_start = len(conditioning_text)
    full_texts: list[str] = []
    for candidate in candidates:
        if not candidate.startswith(prefix) or candidate == prefix:
            raise ValueError(f"Candidate must extend the prefix: {candidate!r}")
        full_texts.append(conditioning_text + candidate)

    encoded = tokenizer(
        full_texts,
        return_tensors="pt",
        return_offsets_mapping=True,
        add_special_tokens=False,
        padding=True,
    )
    offsets = encoded.pop("offset_mapping").tolist()
    input_ids = encoded["input_ids"].to(device)
    attention_mask = encoded["attention_mask"].to(device)

    with torch.inference_mode():
        logits = model(input_ids=input_ids, attention_mask=attention_mask).logits
        log_probs = torch.log_softmax(logits[:, :-1, :], dim=-1)

    results: list[CandidateScore] = []
    for batch_index, candidate in enumerate(candidates):
        selected_log_probs: list[float] = []
        for token_index in range(1, input_ids.shape[1]):
            if attention_mask[batch_index, token_index].item() == 0:
                continue
            _, token_end = offsets[batch_index][token_index]
            if token_end <= candidate_start:
                continue
            predicted_token = input_ids[batch_index, token_index]
            value = log_probs[batch_index, token_index - 1, predicted_token].item()
            selected_log_probs.append(value)

        if not selected_log_probs:
            raise RuntimeError(f"No candidate tokens were found for: {candidate!r}")
        results.append(
            CandidateScore(
                text=candidate,
                average_log_probability=sum(selected_log_probs) / len(selected_log_probs),
            )
        )
    return results


def main() -> None:
    args = parse_args()
    prefix = normalize(args.prefix)
    candidates = tuple(normalize(value) for value in (args.candidates or DEFAULT_CANDIDATES))
    contexts = tuple(args.contexts or CONTEXT_DESCRIPTIONS)
    device = resolve_device(args.device)

    print(f"model={args.model}")
    print(f"device={device}")
    print(f"prefix={prefix}")

    load_started = time.perf_counter()
    tokenizer = AutoTokenizer.from_pretrained(args.model, use_fast=True)
    if tokenizer.pad_token_id is None:
        tokenizer.pad_token = tokenizer.eos_token
    model = AutoModelForCausalLM.from_pretrained(
        args.model,
        torch_dtype="auto",
        low_cpu_mem_usage=True,
    ).to(device)
    model.eval()
    print(f"load_seconds={time.perf_counter() - load_started:.3f}")

    for context in contexts:
        score_started = time.perf_counter()
        scores = score_candidates(prefix, candidates, context, tokenizer, model, device)
        scores.sort(key=lambda item: item.average_log_probability, reverse=True)
        print()
        print(f"context={context}")
        print(f"score_seconds={time.perf_counter() - score_started:.3f}")

        for rank, score in enumerate(scores, start=1):
            print(
                f"{rank:>2}. {score.text} "
                f"avg_log_prob={score.average_log_probability:.6f} "
                f"perplexity={score.perplexity:.3f}"
            )


if __name__ == "__main__":
    main()
