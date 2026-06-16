# Sensable — KakaoBank Braille Interface Mockup

## Project Overview
Sensable is an Android mockup app that replicates the **KakaoBank UI design** and overlays a custom **Braille Interface SaaS** for visually impaired users. The goal is a functional UX demo — not a real banking app.

Package: `com.sensable.app` | minSdk: 24 | targetSdk: 36 | Language: Kotlin + Jetpack Compose

---

## Core Concept: Braille Interface (점자 인터페이스)
- A **3-row × 2-column** grid of oversized touch buttons
- Triggered by an **upward swipe** on the KakaoBank home screen
- Appears as a near-full-screen **ModalBottomSheet**
- Android **TextToSpeech (TTS)** provides Korean voice guidance at every step

---

## Main User Flow: Transfer (송금)

| Step | Trigger | TTS Voice |
|------|---------|-----------|
| 1 | User opens home → swipes up | Braille BottomSheet appears |
| 2 | BottomSheet opens | "어떤 서비스를 이용하시겠습니까?" |
| 3 | User presses Button [row=0, col=0] (top-left = "1") | Transfer mode activated |
| 4 | TransferScreen: RECIPIENT step | "누구에게 보낼까요?" |
| 5 | User enters recipient via braille input | (input echoed) |
| 6 | User confirms → AMOUNT step | "000님께 얼마를 보낼까요?" |
| 7 | User enters amount via braille input | (input echoed) |
| 8 | User confirms → CONFIRM step | "000님께 00000원 송금하는게 맞습니까?" |
| 9 | Final confirm | Transfer complete (mockup) |

---

## Architecture: MVVM + Clean Architecture

### Layers
```
Presentation (feature/)  →  Domain (domain/)  →  Data (data/)
     ↑                           ↑
 ViewModels                 UseCases / Repository interfaces
 Composables                Domain Models
```

### Package Structure
```
com.sensable.app/
├── MainActivity.kt              # @AndroidEntryPoint, NavHost entry
├── SensableApplication.kt       # @HiltAndroidApp
├── core/
│   ├── common/
│   │   └── extension/           # Kotlin + Compose extensions
│   ├── designsystem/
│   │   └── component/           # BrailleGrid and other shared Composables
│   └── navigation/
│       ├── Screen.kt            # Sealed class: navigation routes
│       └── AppNavGraph.kt       # NavHost definition
├── data/
│   ├── di/                      # Hilt DataModule (binds interfaces)
│   └── repository/              # Repository implementations
├── domain/
│   ├── model/                   # Pure data classes (no Android deps)
│   ├── repository/              # Repository interfaces
│   └── usecase/
│       └── transfer/            # TransferUseCase
├── feature/
│   ├── kakaobank/               # KakaoBank home screen mockup
│   │   ├── ui/                  # KakaoBankHomeScreen.kt
│   │   └── viewmodel/           # KakaoBankViewModel.kt
│   ├── braille/                 # Braille Interface BottomSheet
│   │   ├── ui/                  # BrailleBottomSheet.kt
│   │   └── viewmodel/           # BrailleViewModel.kt
│   └── transfer/                # Transfer input flow
│       ├── ui/                  # TransferScreen.kt
│       └── viewmodel/           # TransferViewModel.kt
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
- 3 rows × 2 cols of `BrailleButton` Composables
- Callback: `onButtonClick(row: Int, col: Int)`
- Button numbering: [0,0]=1, [0,1]=2, [1,0]=3, [1,1]=4, [2,0]=5, [2,1]=6

### BrailleBottomSheet (`feature/braille/ui/BrailleBottomSheet.kt`)
- `ModalBottomSheet` with `fillMaxHeight(0.95f)`
- Displays current `guideMessage` from `BrailleViewModel`
- Button [0,0] → Transfer flow → navigate to `TransferScreen`

### TransferViewModel (`feature/transfer/viewmodel/TransferViewModel.kt`)
- `TransferStep`: RECIPIENT → AMOUNT → CONFIRM
- `onConfirm()` advances the step and updates `guideMessage`
- `onBrailleInput(row, col)` maps braille button to characters (to be implemented)

### TTS
- Initialize in Activity or via `CompositionLocal`
- Always call `tts.shutdown()` in `onDestroy()`
- Language: `Locale.KOREAN`

---

## Conventions
- **Screen Composable:** `XxxScreen.kt`, accepts `navController` + `viewModel = hiltViewModel()`
- **ViewModel:** `@HiltViewModel`, `StateFlow<XxxUiState>`, `UiState` data class in same file
- **UseCase:** one class, one `suspend operator fun invoke(...)`, named `XxxUseCase`
- **Domain models:** pure Kotlin data classes, zero Android/framework imports
- **Repository:** interface in `domain/repository/`, impl in `data/repository/`, bound via `@Binds` in `data/di/DataModule.kt`
- No business logic inside Composables — only state observation + event dispatch

---

## What NOT to Do
- This is a **mockup** — no real banking APIs, no real money movement
- No real KakaoBank account data or credentials
- Don't put `viewModelScope` logic directly in a Composable
- Don't call `tts.speak()` without checking initialization status
- Don't skip `tts.shutdown()` on Activity destroy — it leaks resources