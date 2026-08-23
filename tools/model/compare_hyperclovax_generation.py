from __future__ import annotations

import argparse
import json
import time

import torch
from transformers import AutoModelForCausalLM, AutoTokenizer


DEFAULT_MODEL = "naver-hyperclovax/HyperCLOVAX-SEED-Text-Instruct-0.5B"
DEFAULT_CASES = (
    ("person_name", "현"),
    ("person_name", "김보"),
    ("bank_name", "국민"),
    ("address", "서울"),
    ("general", "오늘 회"),
)
LABELS = {
    "person_name": "한국 사람 이름",
    "bank_name": "대한민국 은행 이름",
    "address": "대한민국 주소",
    "general": "일반 한국어 문장",
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", default=DEFAULT_MODEL)
    parser.add_argument("--device", choices=("cpu", "cuda"), default="cpu")
    parser.add_argument("--samples", type=int, default=1)
    parser.add_argument("--case", choices=tuple(LABELS), help="Run one context only")
    parser.add_argument("--prefix", help="Override the prefix for a selected context")
    return parser.parse_args()


def prompt_for(context: str, prefix: str) -> str:
    if context == "person_name":
        return (
            f"첫 글자가 '{prefix}'인 자연스러운 한국 사람 이름을 서로 다르게 3개 만드세요. "
            f"정답은 [\"{prefix}OO\",\"{prefix}OO\",\"{prefix}OO\"] 형태의 JSON 문자열 배열입니다."
        )
    return (
        f"입력 유형: {LABELS[context]}\n"
        f"사용자가 입력한 글자: {prefix}\n"
        "입력한 글자로 시작하는 자연스러운 후보 3개를 JSON 문자열 배열 하나로 답하세요."
    )


def parse_json_candidates(text: str, prefix: str) -> list[str]:
    try:
        value = json.loads(text.strip())
    except json.JSONDecodeError:
        return []
    if not isinstance(value, list):
        return []
    return [
        item.strip()
        for item in value
        if isinstance(item, str) and item.strip().startswith(prefix)
    ]


def main() -> None:
    args = parse_args()
    started = time.perf_counter()
    tokenizer = AutoTokenizer.from_pretrained(args.model, use_fast=True)
    model = AutoModelForCausalLM.from_pretrained(args.model, dtype="auto")
    model.to(args.device)
    model.eval()
    print(f"model={args.model}")
    print(f"device={args.device}")
    print(f"load_seconds={time.perf_counter() - started:.3f}")

    cases = DEFAULT_CASES if args.case is None else tuple(
        case for case in DEFAULT_CASES if case[0] == args.case
    )
    if args.case is not None and args.prefix is not None:
        cases = ((args.case, args.prefix),)
    for context, prefix in cases:
        messages = [{"role": "user", "content": prompt_for(context, prefix)}]
        rendered = tokenizer.apply_chat_template(
            messages,
            tokenize=False,
            add_generation_prompt=True,
        )
        inputs = tokenizer(rendered, return_tensors="pt").to(args.device)
        print(f"\ncontext={context}")
        print(f"prefix={prefix}")
        for sample_index in range(args.samples):
            started = time.perf_counter()
            with torch.inference_mode():
                output = model.generate(
                    **inputs,
                    max_new_tokens=24,
                    do_sample=False,
                    repetition_penalty=1.2,
                    pad_token_id=tokenizer.eos_token_id,
                )
            elapsed = time.perf_counter() - started
            generated = tokenizer.decode(
                output[0, inputs.input_ids.shape[1] :],
                skip_special_tokens=True,
            ).strip()
            accepted = parse_json_candidates(generated, prefix)
            print(f"sample={sample_index + 1} seconds={elapsed:.3f}")
            print(f"raw={generated!r}")
            print(f"accepted={accepted!r}")


if __name__ == "__main__":
    main()
