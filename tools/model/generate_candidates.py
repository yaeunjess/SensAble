from __future__ import annotations

import argparse
import json
import re
import time
import unicodedata

import torch
from transformers import AutoModelForCausalLM, AutoTokenizer


DEFAULT_MODEL = "Qwen/Qwen2.5-0.5B-Instruct"
CONTEXT_DESCRIPTIONS = {
    "general": "일반적인 한국어 단어 또는 고유명사",
    "bank_name": "은행 또는 금융기관 이름",
    "person_name": "사람 이름",
    "organization_name": "기관 또는 단체 이름",
    "address": "대한민국 주소 또는 행정구역",
    "product_name": "상품 이름",
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate context-aware Korean completions.")
    parser.add_argument("--model", default=DEFAULT_MODEL)
    parser.add_argument("--prefix", default="국민")
    parser.add_argument("--context", choices=tuple(CONTEXT_DESCRIPTIONS), default="bank_name")
    parser.add_argument("--count", type=int, default=5)
    parser.add_argument("--device", choices=("auto", "cpu", "cuda"), default="auto")
    return parser.parse_args()


def normalize(value: str) -> str:
    return unicodedata.normalize("NFC", value.strip())


def parse_json_array(raw: str) -> list[str]:
    match = re.search(r"\[[\s\S]*?\]", raw)
    if match is None:
        return []
    try:
        values = json.loads(match.group(0))
    except json.JSONDecodeError:
        return []
    return [normalize(value) for value in values if isinstance(value, str)]


def main() -> None:
    args = parse_args()
    device = "cuda" if args.device == "auto" and torch.cuda.is_available() else args.device
    if device == "auto":
        device = "cpu"

    load_started = time.perf_counter()
    tokenizer = AutoTokenizer.from_pretrained(args.model, use_fast=True)
    model = AutoModelForCausalLM.from_pretrained(
        args.model,
        torch_dtype="auto",
        low_cpu_mem_usage=True,
    ).to(device)
    model.eval()
    print(f"model={args.model}")
    print(f"device={device}")
    print(f"load_seconds={time.perf_counter() - load_started:.3f}")

    prefix = normalize(args.prefix)
    description = CONTEXT_DESCRIPTIONS[args.context]
    prompt = (
        f"분야: {description}\n"
        f"접두어: {prefix}\n"
        f"후보 수: {args.count}\n"
        "규칙: 각 문자열의 첫 글자부터 접두어와 정확히 같아야 한다. "
        "설명, 객체, 문장 없이 문자열 JSON 배열만 출력한다."
    )
    messages = [
        {
            "role": "system",
            "content": "너는 지시된 접두어를 절대 변경하지 않는 한국어 자동완성기다.",
        },
        {
            "role": "user",
            "content": (
                "분야: 은행 또는 금융기관 이름\n접두어: 신\n후보 수: 2\n"
                "규칙: 문자열 JSON 배열만 출력한다."
            ),
        },
        {"role": "assistant", "content": '["신한은행", "신협"]'},
        {"role": "user", "content": prompt},
    ]
    rendered = tokenizer.apply_chat_template(
        messages,
        tokenize=False,
        add_generation_prompt=True,
    )
    inputs = tokenizer(rendered, return_tensors="pt").to(device)

    generation_started = time.perf_counter()
    with torch.inference_mode():
        generated = model.generate(
            **inputs,
            max_new_tokens=64,
            do_sample=False,
            eos_token_id=tokenizer.eos_token_id,
        )
    generated_tokens = generated[0, inputs["input_ids"].shape[1] :]
    raw = tokenizer.decode(generated_tokens, skip_special_tokens=True).strip()
    elapsed = time.perf_counter() - generation_started
    candidates = [
        value
        for value in parse_json_array(raw)
        if value.startswith(prefix) and value != prefix
    ]

    print(f"context={args.context}")
    print(f"prefix={prefix}")
    print(f"generation_seconds={elapsed:.3f}")
    print(f"raw={raw}")
    print("accepted_candidates=")
    for index, candidate in enumerate(dict.fromkeys(candidates), start=1):
        print(f"{index:>2}. {candidate}")


if __name__ == "__main__":
    main()
