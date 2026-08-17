# FIN:CLUE Android SDK (MVP)

FIN:CLUE is an on-device accessible financial input SDK packaged as an Android `.aar`.
The MVP deliberately declares no `INTERNET` permission and contains no network client.

## Public API

```kotlin
val spec = FlowSpec(
    fields = listOf(
        FieldSpec("toAccount", FieldType.ACCOUNT, "받는 계좌", "계좌번호를 입력하세요"),
        FieldSpec("amount", FieldType.AMOUNT, "보낼 금액", "금액을 입력하세요"),
    )
)

Finclue.runFlow(activity, spec) { result ->
    when (result) {
        is FlowResult.Success -> useValues(result.values)
        FlowResult.Cancelled -> Unit
        is FlowResult.Error -> showError(result.message)
    }
}
```

Supported MVP field types are `ACCOUNT`, `AMOUNT`, and `PIN`. The SDK owns the six-dot
input UI, numeric braille decoding, Korean TTS, haptic feedback, field sequencing, and
privacy-preserving Room metrics. The host owns authentication and financial APIs.

## On-device proof

```kotlin
val summary = Finclue.getLocalDataSummary(context)
Finclue.clearLocalData(context)
```

The Room database stores session state and aggregate field metrics only. It does not store
raw account numbers, amounts, PINs, or reconstructable braille cells. App uninstall or app
data clearing removes this installation's database.

## Build artifact

```powershell
.\gradlew.bat :finclue-sdk:assembleRelease
```

Output: `finclue-sdk/build/outputs/aar/finclue-sdk-release.aar`

Because this repository's demo app consumes the local AAR directly, it declares the SDK's
Room and coroutine runtime dependencies itself. A later Maven publication should publish
these dependencies through its POM automatically.
