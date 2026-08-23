# Sensable — KakaoBank Braille Interface Mockup

## Project Overview
Sensable is an Android mockup app that replicates the **KakaoBank UI design** and overlays a custom **Braille Interface SaaS** for visually impaired users. The goal is a functional UX demo — not a real banking app.

Package: `com.sensable.app` | minSdk: 24 | targetSdk: 36 | Language: Kotlin + Jetpack Compose

---

## Core Concept: Braille Interface (점자 인터페이스)
- A **3-row × 2-column** grid of oversized touch buttons, always showing digits 1–6
- Appears as a near-full-screen **ModalBottomSheet**, auto-triggered **1 second** after landing on the account-number or amount-entry screen (see flow below) — this replaces the OS on-screen keyboard for those inputs (the underlying `TextField`/keypad is `readOnly`/decorative)
- Android **TextToSpeech (TTS)** provides Korean voice guidance at every step
- Two independent entry modes only — `BrailleMode.TRANSFER_RECIPIENT` (account number entry) and `BrailleMode.TRANSFER_AMOUNT` (amount entry). There is no more service-select / recipient-name-search / typo-correction / in-sheet confirm step — each mode is a standalone, self-contained input widget started directly by the screen that shows it (`BrailleViewModel.startAccountNumberEntry()` / `startAmountEntry()`)

---

## Main User Flow: Transfer (송금)

The whole flow is a chain of regular (sighted) KakaoBank-style screens; the braille bottom sheet only appears twice, to capture the account number and the amount, then hands control straight back to the screen flow.

| Step | Screen | What happens |
|------|--------|---------------|
| 1 | `KakaoBankHomeScreen` | User taps **이체** on the account card |
| 2 | `TransferRecipientScreen` | Recipient search / recent-transfer list (mock data only); user taps **+ 계좌번호 직접입력** |
| 3 | `TransferAccountInputScreen` | 1s after landing, Braille bottom sheet opens in `TRANSFER_RECIPIENT` mode → user types the account number with the 1–6 grid → double-tap confirms (recipient name is always the mock `"이지영"`) |
| 4 | `TransferAmountInputScreen` | 1s after landing, Braille bottom sheet opens in `TRANSFER_AMOUNT` mode → user types the amount with the 1–6 grid → double-tap confirms |
| 5 | `TransferMemoScreen` | Shows the amount + recipient/account entered via the bottom sheet; **다음** opens a confirm `ModalBottomSheet` ("OOO님에게 OOO원 이체하시겠습니까?") |
| 6 | Confirm sheet → **이체하기** | Shows `FingerprintAuthOverlay` (`core/designsystem/component/FingerprintAuthOverlay.kt`) — tap the fingerprint icon → simulated scan → success |
| 7 | `TransferCompleteScreen` | Shows recipient/amount/account actually entered; deducts the amount from `AccountBalanceStore` (session-only balance, resets to `MOCK_BALANCE` on app restart) |

Swipe-left on the braille grid deletes the last entered digit (or clears the current unsent cell); with nothing left to undo it dismisses the sheet.

Note: `TransferAmountInputScreen` also has its own manual on-screen numeric keypad with a **다음** button that navigates straight to `TransferCompleteScreen`, bypassing `TransferMemoScreen` — this is a second, keypad-driven path that exists alongside the braille-sheet path described above (both are live; the braille sheet is the one that leads through `TransferMemoScreen`).

---

## Current mock behavior
- No real banking APIs, AI recommendation APIs, or bank/auth backend calls anywhere — everything below is local/simulated:
  - Recipient name is always hardcoded to `"이지영"` once an account number is entered (no name lookup)
  - `TransferRecipientScreen`'s "최근 이체" list is static mock data (fake account numbers — never use a real account number here)
  - `FingerprintAuthOverlay` is a pure UI animation (tap → delay → success), no real biometric/FIDO or backend verification
  - `AccountBalanceStore` (`core/common/AccountBalanceStore.kt`) is an in-memory Hilt `@Singleton` — it deducts on `TransferCompleteViewModel` init and lives only for the app process; killing and reopening the app resets it to `MOCK_BALANCE`
- `domain/`, `data/di/DataModule.kt`, `data/repository/TransferRepositoryImpl.kt` contain a `TransferUseCase`/`TransferRepository` scaffold that is **not currently wired into the transfer flow** (no feature code calls it) — treat it as unused scaffolding, not the source of truth for how transfers work today

---

## Architecture: MVVM + Clean Architecture

### Layers
```
Presentation (feature/)  →  Domain (domain/)  →  Data (data/)
     ↑                           ↑
 ViewModels                 UseCases / Repository interfaces
 Composables                Domain Models
```
Note: the `domain`/`data` layers exist but are currently unused scaffolding (see "Current mock behavior" above) — the live transfer flow passes mock data directly between Composables/ViewModels and via Navigation route arguments instead.

### Package Structure
```
com.sensable.app/
├── MainActivity.kt              # @AndroidEntryPoint, NavHost entry
├── SensableApplication.kt       # @HiltAndroidApp
├── core/
│   ├── braille/
│   │   └── BrailleDecoder.kt    # Braille cell → digit decoding (numbers only)
│   ├── common/
│   │   ├── AccountBalanceStore.kt  # Session-only mock balance (Hilt singleton)
│   │   ├── MockData.kt          # MOCK_BALANCE
│   │   └── extension/           # Kotlin + Compose extensions
│   ├── designsystem/
│   │   └── component/           # BrailleGrid, FingerprintAuthOverlay
│   ├── navigation/
│   │   ├── Screen.kt            # Sealed class: navigation routes
│   │   └── AppNavGraph.kt       # NavHost definition
│   └── tts/
│       └── TtsManager.kt
├── data/
│   ├── di/                      # Hilt DataModule (binds interfaces) — unused scaffolding
│   └── repository/              # TransferRepositoryImpl — unused scaffolding
├── domain/                      # Unused scaffolding (see note above)
│   ├── model/
│   ├── repository/
│   └── usecase/transfer/
├── feature/
│   ├── kakaobank/                        # KakaoBank home screen mockup
│   │   ├── ui/                           # KakaoBankHomeScreen.kt
│   │   └── viewmodel/                    # KakaoBankViewModel.kt (exposes balance)
│   ├── braille/                          # Braille Interface BottomSheet
│   │   ├── ui/                           # BrailleBottomSheet.kt
│   │   └── viewmodel/                    # BrailleViewModel.kt
│   └── transfer/                         # Full transfer screen flow
│       ├── ui/
│       │   ├── TransferRecipientScreen.kt      # 받는사람 검색 / 최근 이체
│       │   ├── TransferAccountInputScreen.kt   # 계좌번호 직접입력 (+ braille sheet)
│       │   ├── TransferAmountInputScreen.kt    # 송금액 입력 (+ braille sheet)
│       │   ├── TransferMemoScreen.kt           # 메모 + 확인 바텀시트 + 지문인증
│       │   └── TransferCompleteScreen.kt       # 이체 완료
│       └── viewmodel/
│           └── TransferCompleteViewModel.kt    # TTS + balance deduction on completion
└── ui/
    └── theme/                   # SensableTheme, Color, Typography (Compose)
```

---

## Tech Stack
| Category | Library |
|----------|---------|
| UI | Jetpack Compose + Material3 |
| DI | Hilt |
| Navigation | Navigation Compose |
| ViewModel | lifecycle-viewmodel-compose |
| Async | Kotlin Coroutines + StateFlow |
| TTS | `android.speech.tts.TextToSpeech` |
| Architecture | MVVM + Clean Architecture |

---

## Key Components

### BrailleGrid (`core/designsystem/component/BrailleGrid.kt`)
- 3 rows × 2 cols of `BrailleButton` Composables, always showing digits 1–6 (no other layout modes)
- Callback: `onButtonClick(dot: Int)`, plus `onSwipeRight` (confirm cell), `onSwipeLeft` (undo/dismiss), `onDoubleTap` (submit)
- Button numbering: [0,0]=1, [0,1]=2, [1,0]=3, [1,1]=4, [2,0]=5, [2,1]=6
- Idle buttons use `SensableDarkButtonIdle`; active/selected use `SensableBlue` — both drawn at `alpha = 0.9f` (slightly translucent, intentionally) with elevation so they read as solid against the translucent sheet background

### BrailleBottomSheet (`feature/braille/ui/BrailleBottomSheet.kt`)
- `ModalBottomSheet` with `fillMaxHeight(0.95f)`, translucent dark background (`SensableDarkSurface.copy(alpha = 0.5f)`) so the screen behind is visible
- Started in one of two modes via params: `startInAccountNumberMode` (calls `viewModel.startAccountNumberEntry()`) or `startInAmountMode` (calls `viewModel.startAmountEntry(recipientName)`)
- Emits results back to the caller via `BrailleUiState.accountEntryCompleted` / `amountEntryCompleted`, which trigger `LaunchedEffect`s that dismiss the sheet and `navController.navigate(...)` to the next screen
- `onBackPress` (optional): wire this when the hosting screen wants system back-press to navigate away instead of being silently swallowed by the sheet's own back handling (used by `TransferAccountInputScreen`)

### BrailleViewModel (`feature/braille/viewmodel/BrailleViewModel.kt`)
- `BrailleUiState.mode`: `TRANSFER_RECIPIENT` (account number) or `TRANSFER_AMOUNT`
- `onSwipeRight()` decodes the currently-selected dots as a digit (`BrailleDecoder.decodeNumber`) and appends it
- `onDoubleTap()` finalizes input: `TRANSFER_RECIPIENT` → `accountEntryCompleted`, `TRANSFER_AMOUNT` → `amountEntryCompleted`
- `onSwipeLeft()` returns `true` when the caller should dismiss the sheet (nothing left to undo)

### FingerprintAuthOverlay (`core/designsystem/component/FingerprintAuthOverlay.kt`)
- Full-screen overlay Composable, shared design-system component; currently used by `TransferMemoScreen`'s confirm bottom sheet
- Pure UI simulation: tap fingerprint icon → `SCANNING` → delay → `SUCCESS` → `onAuthSuccess()`

### AccountBalanceStore (`core/common/AccountBalanceStore.kt`)
- `@Singleton`, `MutableStateFlow<Long>` seeded with `MOCK_BALANCE`
- `deduct(amount)` called once from `TransferCompleteViewModel.init`
- Session-only: resets to `MOCK_BALANCE` on app process restart (no persistence layer)

### TTS
- Initialize in Activity or via `CompositionLocal`
- Always call `tts.shutdown()` in `onDestroy()`
- Language: `Locale.KOREAN`

---

## Conventions
- **Screen Composable:** `XxxScreen.kt`, accepts `navController`; only add `viewModel = hiltViewModel()` when the screen actually needs state/logic beyond static mock display (several transfer screens are static enough to take mock values as plain function params instead)
- **ViewModel:** `@HiltViewModel`, `StateFlow<XxxUiState>`, `UiState` data class in same file
- **UseCase:** one class, one `suspend operator fun invoke(...)`, named `XxxUseCase` (currently unused scaffolding, see above)
- **Domain models:** pure Kotlin data classes, zero Android/framework imports
- **Repository:** interface in `domain/repository/`, impl in `data/repository/`, bound via `@Binds` in `data/di/DataModule.kt` (currently unused scaffolding, see above)
- No business logic inside Composables — only state observation + event dispatch

---

## What NOT to Do
- This is a **mockup** — no real banking APIs, no real money movement
- No real KakaoBank account data or credentials — use randomly-generated fake account numbers everywhere, never a real one
- Don't put `viewModelScope` logic directly in a Composable
- Don't call `tts.speak()` without checking initialization status
- Don't skip `tts.shutdown()` on Activity destroy — it leaks resources
- Don't reintroduce the old service-select / recipient-name-search / typo-correction / in-sheet-confirm braille flow — it was intentionally removed in favor of the two-mode (`TRANSFER_RECIPIENT` account number, `TRANSFER_AMOUNT`) direct-entry design
