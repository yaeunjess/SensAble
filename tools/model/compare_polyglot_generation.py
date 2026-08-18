from __future__ import annotations

import argparse
import time
import unicodedata

import torch
from transformers import AutoModelForCausalLM, AutoTokenizer


DEFAULT_MODEL = "EleutherAI/polyglot-ko-1.3b"
DEFAULT_CASES = (
    ("person_name", "현"),
    ("person_name", "김보"),
    ("bank_name", "국민"),
    ("address", "서울"),
    ("general", "오늘 회"),
)
CONTEXT_LABELS = {
    "general": "일반 텍스트",
    "bank_name": "은행명",
    "person_name": "사람 이름",
    "organization_name": "기관명",
    "address": "주소",
    "product_name": "상품명",
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Compare raw Base-LM Korean autocomplete generation before Android packaging."
    )
    parser.add_argument("--model", default=DEFAULT_MODEL)
    parser.add_argument("--context", choices=tuple(CONTEXT_LABELS))
    parser.add_argument("--prefix")
    parser.add_argument("--count", type=int, default=8)
    parser.add_argument("--max-new-tokens", type=int, default=8)
    parser.add_argument("--device", choices=("auto", "cpu", "cuda"), default="auto")
    return parser.parse_args()


def normalize(value: str) -> str:
    return unicodedata.normalize("NFC", value.strip())


def first_segment(value: str) -> str:
    return value.splitlines()[0].split(",")[0].split(";")[0].split(":")[0].strip()


def main() -> None:
    args = parse_args()
    if (args.context is None) != (args.prefix is None):
        raise ValueError("--context and --prefix must be supplied together.")
    cases = ((args.context, normalize(args.prefix)),) if args.context else DEFAULT_CASES
    device = "cuda" if args.device == "auto" and torch.cuda.is_available() else args.device
    if device == "auto":
        device = "cpu"

    load_started = time.perf_counter()
    tokenizer = AutoTokenizer.from_pretrained(args.model, use_fast=True)
    model = AutoModelForCausalLM.from_pretrained(
        args.model,
        dtype="auto",
        low_cpu_mem_usage=True,
    ).to(device)
    model.eval()
    print(f"model={args.model}")
    print(f"device={device}")
    print(f"load_seconds={time.perf_counter() - load_started:.3f}")

    for context, prefix in cases:
        prompt = f"입력 유형: {CONTEXT_LABELS[context]}\n자동완성: {prefix}"
        inputs = tokenizer(prompt, return_tensors="pt").to(device)
        started = time.perf_counter()
        with torch.inference_mode():
            generated = model.generate(
                **inputs,
                max_new_tokens=args.max_new_tokens,
                do_sample=True,
                temperature=0.75,
                top_k=40,
                top_p=0.9,
                num_return_sequences=args.count,
                pad_token_id=tokenizer.eos_token_id,
            )
        elapsed = time.perf_counter() - started
        suffixes = tokenizer.batch_decode(
            generated[:, inputs["input_ids"].shape[1] :],
            skip_special_tokens=True,
        )
        candidates = list(
            dict.fromkeys(
                candidate
                for suffix in suffixes
                if (candidate := normalize(first_segment(prefix + suffix))) != prefix
                and candidate.startswith(prefix)
            )
        )
        print()
        print(f"context={context}")
        print(f"prefix={prefix}")
        print(f"generation_seconds={elapsed:.3f}")
        print("raw_suffixes=")
        for index, suffix in enumerate(suffixes, start=1):
            print(f"{index:>2}. {suffix!r}")
        print("accepted_candidates=")
        for index, candidate in enumerate(candidates, start=1):
            print(f"{index:>2}. {candidate}")


if __name__ == "__main__":
    main()
