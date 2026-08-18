# FIN:CLUE model validation

This directory validates and prepares the pretrained language model for Android/JNI integration.
It does not train a model. The selected Q4_K_M GGUF is copied into the SDK assets and shipped
inside the Android AAR; generated working models under `tools/model/models` remain local-only.

The current design does not expose unconstrained model text. A licensed on-device
autocomplete lexicon supplies valid candidates, and the causal language model scores how
likely each candidate suffix is after the current prefix. Personal history candidates are
merged and re-ranked later in the Android SDK.

## Setup (PowerShell)

Python 3.12 is available in the current development environment.

```powershell
cd tools\model
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
python -m pip install -r requirements.txt
```

Installing dependencies and loading the model require network access on the development
machine. Runtime inference in the Android SDK will remain fully offline.

## AAR packaging

The release model is stored at
`finclue-sdk/src/main/assets/finclue/prediction/model/polyglot-ko-1.3b-q4_k_m.gguf`
and tracked with Git LFS. At first prediction use, the SDK verifies and copies the model to
the app-private `filesDir/finclue/prediction/model` directory. llama.cpp then memory-maps that
real file path. Later uses skip the copy when the recorded model version, size, and SHA-256 match.

## Run the first validation

```powershell
python score_candidates.py
```

The first run downloads `Qwen/Qwen2.5-0.5B` into the local Hugging Face cache. The script
prints model loading time, candidate scoring time, and rankings for several completions of
`국민`. By default it compares no semantic context, a bank-name context, and an organization
context. It intentionally includes `국민과 함께하는 체조` so we can observe whether context
alone suppresses it; candidate constraints can be added after the MVP if context is not
sufficient.

All candidates for one context are padded and scored in a single model batch. This mirrors
the intended SDK optimization: calculate the shared context once and avoid one full model
invocation per candidate.

Custom inputs can be compared without editing the script:

```powershell
python score_candidates.py `
  --prefix 서울 `
  --candidate 서울특별시 `
  --candidate 서울대학교 `
  --candidate "서울에서 만나요"
```

To validate actual LM candidate generation rather than scoring a supplied candidate list:

```powershell
python generate_candidates.py --prefix 국민 --context bank_name
```

This uses the instruction-tuned 0.5B model and applies strict JSON and prefix validation to
its output. Candidate scoring and candidate generation are deliberately measured separately.

## Decision gate

This check answers only whether a pretrained causal model produces useful relative scores
for known Korean completions. Android latency must be measured separately after conversion
to GGUF and llama.cpp integration on a real device.

## GGUF conversion

The tested converter revision and output checksum are recorded in `model-manifest.json`.
Keep generated model files under `models/`; the directory is intentionally excluded from
Git.

```powershell
git clone --depth 1 https://github.com/ggml-org/llama.cpp.git third_party\llama.cpp
python -m venv .convert-venv
.\.convert-venv\Scripts\Activate.ps1
python -m pip install `
  -r third_party\llama.cpp\requirements\requirements-convert_hf_to_gguf.txt

python third_party\llama.cpp\convert_hf_to_gguf.py <hugging-face-model-directory> `
  --outfile models\qwen2.5-0.5b-f16.gguf `
  --outtype f16

third_party\llama-bin-b10483\llama-quantize.exe `
  models\qwen2.5-0.5b-f16.gguf `
  models\qwen2.5-0.5b-q4_k_m.gguf `
  Q4_K_M
```

The local Windows CPU smoke test loaded the Q4 model successfully and measured roughly
175 prompt tokens/second and 42 generated tokens/second. These desktop figures do not
predict Android performance; the AAR integration must be benchmarked on a physical arm64
device.
