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

## Data & API Flow (데이터 및 API 호출 흐름)

### 전체 흐름 다이어그램

```
[사용자] 점자 버튼 입력 (수취인)
         ↓
[앱] BrailleViewModel / TransferViewModel
         ↓
[AI 팀 API] 점자 입력 분석
    ├── 한글 판별 → 최근 이체 내역에서 수취인 추천 목록 반환
    └── 숫자 판별 → 계좌번호로 새 연락처 생성
         ↓
[앱] TTS로 추천 목록 읽어줌 → 사용자가 선택
         ↓
[사용자] 점자 버튼 입력 (금액)
         ↓
[본인인증 단계] 우선순위: 지문(FIDO) → PIN
    앱: 생체인증 / PIN UI 담당
    백엔드: 공공 은행 API 호출하여 인증 검증
         ↓ (인증 성공)
[백엔드] 송금 API 호출
         ↓
[앱] TransferCompleteScreen 표시
```

### 각 단계 상세

#### 1. AI 팀 API — 수취인 자동완성
- **입력**: 사용자가 점자 버튼으로 입력한 문자열
- **처리**: 한글이면 수취인 이름으로 판단 → 최근 이체 내역에서 유사 수취인 추천  
         숫자이면 계좌번호로 판단 → 새 주소록 항목 생성
- **출력**: 추천 수취인 목록 (앱이 TTS로 읽어줌)
- **❓ 미확정**: 입력을 실시간(버튼 누를 때마다)으로 전송하는지, 글자 완성 단위로 전송하는지, 확인 버튼 후 배치로 전송하는지 — AI 팀 확인 필요

#### 2. 공공 은행 API — 본인인증
- **우선순위**: 지문(생체인증) → PIN
- **역할 분담 (추정)**:
  - 앱: 지문/PIN 입력 UI 제공, 인증 결과를 백엔드에 전달
  - 백엔드: 공공 은행 API를 직접 호출하여 인증 검증
- **❓ 미확정**: 앱이 공공 은행 API를 직접 호출하는지, 백엔드를 경유하는지 — 백엔드 팀 확인 필요

#### 3. 송금 API
- 본인인증 성공 후 **백엔드**가 직접 호출
- 앱은 결과(성공/실패)만 수신하여 완료 화면 표시

### 현재 목업에서의 처리
- AI API, 공공 은행 API, 송금 API 모두 **실제 호출 없음**
- `TransferRepositoryImpl`이 항상 성공을 반환하는 mock으로 대체
- 수취인·금액은 하드코딩("이지영", "50,000원")

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