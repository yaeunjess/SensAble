# SensAble

> 시각장애인을 위한 점자 인터페이스 SaaS 목업 — KakaoBank UI 기반

---

## 소개

SensAble은 카카오뱅크 UI 위에 **점자 인터페이스**를 오버레이하는 안드로이드 앱 목업입니다.
시각장애인 사용자가 3×2 점자 버튼, 스와이프 제스처, 한국어 TTS 음성 안내만으로 송금을 완료할 수 있는 UX를 시연합니다.

화면을 보지 않고도 조작할 수 있도록 **모든 상태 변화는 음성으로 안내**되며, 버튼 입력마다 **햅틱 피드백**이 발생합니다.

---

## 주요 기능

- **점자 인터페이스 BottomSheet** — 홈 화면에서 위로 스와이프하면 화면 95%를 덮는 점자 입력 패널 등장
- **한글 점자 조합** — 초성·중성·종성 상태 머신으로 점자 셀을 완성된 한글 음절로 조합
  - 된소리 초성(ㄲ/ㄸ/ㅃ/ㅆ/ㅉ) 및 된소리 받침(ㄲ/ㅆ) 지원
  - 2셀 복합 모음(ㅟ/ㅒ/ㅙ/ㅞ) 지원
- **숫자 모드** — 수표(數符, 점 {3,4,5,6}) 입력 시 계좌번호·금액 숫자 모드로 전환
- **자동완성 / 오타교정** — 최근 이체 내역 기반 수취인 추천 (현재 목업 데이터)
- **한국어 TTS 음성 안내** — 입력한 점 번호, 조합 중인 글자, 각 단계 안내를 실시간 음성 출력
- **햅틱 피드백** — 점자 버튼 터치 시 80ms 진동
- **지문 인증 오버레이** — 송금 확정 시 바텀시트 위에 인증 UI 표시 (목업 애니메이션)
- **다크 테마 바텀시트** — 카카오뱅크 화면(라이트)과 시각적으로 구분되는 오버레이 레이어

---

## 조작 방식

점자 인터페이스는 **점자 버튼 6개 + 4가지 제스처**만으로 모든 조작이 이루어집니다.

| 동작 | 기능 |
|------|------|
| 점자 버튼 터치 | 해당 점 토글 (다시 누르면 취소) — 조합 중인 셀에 점 추가/제거 |
| **오른쪽 스와이프** | 현재 셀 확정 → 다음 글자로 이동 / 선택지 순환(송금하기·잔액조회, 예·아니요) |
| **왼쪽 스와이프** | 취소 · 뒤로 — 셀 초기화 → 마지막 글자 삭제 → 이전 단계 복귀 순으로 동작 |
| **위쪽 스와이프** | 자동완성 · 오타교정 후보 순환 |
| **더블탭** | 현재 단계 확정 (다음 단계로 진행) |

### 버튼 배치 — 쓰기 방향

점자는 종이 뒤에서 찍고 앞에서 읽으므로, 입력 UI는 **쓰기 방향(좌우 반전)** 으로 배치되어 있습니다.

```
  화면 (쓰기 방향)      표준 점자 (읽기 방향)
    [4] [1]                [1] [4]
    [5] [2]       vs       [2] [5]
    [6] [3]                [3] [6]
```

버튼을 누르면 **표준 점 번호(1~6)** 가 그대로 전달되므로 디코더는 표준 번호로 처리합니다.
가운데 넓은 여백은 더블탭 영역입니다.

---

## 기술 스택

| 분류 | 사용 기술 |
|------|----------|
| UI | Jetpack Compose + Material3 |
| 아키텍처 | MVVM + Clean Architecture |
| DI | Hilt |
| 내비게이션 | Navigation Compose |
| 비동기 | Kotlin Coroutines + StateFlow |
| TTS | Android TextToSpeech (한국어, Hilt `@Singleton` `TtsManager`) |
| 햅틱 | `VibratorManager` / `Vibrator` (VIBRATE 권한) | 

---

## 송금 플로우

점자 인터페이스는 `BrailleMode` 5단계로 동작합니다.

```
카카오뱅크 홈 → 위로 스와이프
  │
  ▼
[SERVICE_SELECT] "어떤 서비스를 이용하시겠습니까?"
  오른쪽 스와이프로 송금하기 ↔ 잔액조회 순환 → 더블탭으로 확정
  │  ("잔액조회"는 준비 중 안내만 출력)
  ▼
[TRANSFER_RECIPIENT] "누구에게 보낼까요?"
  점자로 이름 입력 (위 스와이프 → 자동완성 후보)
  수표({3,4,5,6}) 입력 시 → 계좌번호 모드 (오타교정 건너뜀)
  │
  ▼
[TYPO_CORRECTION] "오타 교정을 하시겠습니까?"
  위 스와이프로 유사 이름 후보 순환 / 더블탭으로 건너뛰기
  │
  ▼
[TRANSFER_AMOUNT] "OOO님에게 얼마를 보낼까요?"
  숫자 모드로 자동 전환, 입력값을 "50,000원" 형태로 실시간 표시·음성 안내
  │
  ▼
[TRANSFER_CONFIRM] "OOO님에게 50,000원을 보내시겠습니까?
                    이체 후 잔액은 450,000원입니다."
  오른쪽 스와이프로 예 ↔ 아니요 순환 → 더블탭으로 확정
  │
  ▼
지문 인증 오버레이 (바텀시트 위에 표시)
  지문 아이콘 터치 → 인식 중(1.6초) → 인증 완료(0.8초)
  │
  ▼
TransferCompleteScreen — "OOO님에게 50,000원을 보냈어요"
```

> 잔액은 목업 상수(`MOCK_BALANCE` = 500,000원)이며, 이체 후 잔액은 입력 금액을 차감해 계산합니다.

---

## 데이터 & API 호출 흐름

```
[점자 입력 - 수취인]
      ↓
[AI 팀 API] 한글/숫자 판별
  ├── 한글 → 최근 이체 내역에서 수취인 추천
  └── 숫자 → 계좌번호로 새 연락처 생성
      ↓
[TTS 목록 읽어줌 → 사용자 선택]
      ↓
[점자 입력 - 금액]
      ↓
[본인인증] 지문(FIDO) 우선 → PIN 차선
  앱: 인증 UI 담당
  백엔드: 공공 은행 API 검증
      ↓ (인증 성공)
[백엔드 → 송금 API 호출]
      ↓
[완료 화면]
```

### 현재 목업 상태

| 항목 | 상태 |
|------|------|
| AI 자동완성 API | 미연동 — `BrailleViewModel` 내 이름 풀(김봄, 김보미, 김별, 김봄비, 김보람) 사용 |
| 오타교정 API | 미연동 — 같은 이름 풀에서 입력값 제외 후 3개 반환 |
| 공공 은행 인증 API | 미연동 — `FingerprintAuthOverlay`가 타이머 기반 애니메이션으로 성공 처리 |
| 송금 API | 미연동 — `TransferRepositoryImpl`이 항상 `Result.success` 반환 |
| 계좌 잔액 | 하드코딩 (`MOCK_BALANCE` = 500,000원) |
| 계좌번호 입력 시 수취인 | 하드코딩 ("이지영") |

---

## 프로젝트 구조

```
com.sensable.app/
├── MainActivity.kt              # AppCompatActivity + @AndroidEntryPoint, NavHost 진입점
├── SensableApplication.kt       # @HiltAndroidApp
├── core/
│   ├── braille/
│   │   ├── BrailleDecoder.kt            # 점형 → 숫자·초성·중성·종성 디코딩 테이블
│   │   └── KoreanBrailleStateMachine.kt # 점자 셀 → 한글 음절 조합 상태 머신
│   ├── common/
│   │   ├── MockData.kt                  # 목업 잔액 상수 및 이체 후 잔액 계산
│   │   └── extension/
│   ├── designsystem/component/
│   │   └── BrailleGrid.kt               # 3×2 점자 버튼 + 제스처 감지 + 햅틱
│   ├── navigation/
│   │   ├── Screen.kt                    # 라우트 정의 (KakaoBankHome, TransferComplete)
│   │   └── AppNavGraph.kt
│   └── tts/
│       └── TtsManager.kt                # @Singleton TTS 래퍼 (speak / speakQueued / speakWithCompletion)
├── data/
│   ├── di/DataModule.kt
│   └── repository/TransferRepositoryImpl.kt
├── domain/
│   ├── model/TransferInfo.kt
│   ├── repository/TransferRepository.kt
│   └── usecase/transfer/TransferUseCase.kt
├── feature/
│   ├── kakaobank/               # 카카오뱅크 홈 화면 목업 + 위 스와이프 트리거
│   ├── braille/                 # 점자 BottomSheet — 입력·자동완성·확인·지문인증 전 단계 담당
│   │   ├── ui/BrailleBottomSheet.kt
│   │   └── viewmodel/BrailleViewModel.kt
│   └── transfer/
│       ├── ui/TransferCompleteScreen.kt
│       └── viewmodel/TransferCompleteViewModel.kt
└── ui/theme/                    # SensableTheme, Color, Typography
```

> 수취인·금액 입력부터 송금 확인, 지문 인증까지 **모든 단계가 `BrailleBottomSheet` 내부에서 처리**되며,
> 별도 화면으로 이동하는 것은 송금 완료 화면뿐입니다.

---

## 핵심 컴포넌트

### `KoreanBrailleStateMachine`
점자 셀 시퀀스를 받아 한글 음절로 조합하는 상태 머신입니다. 5개 상태(`EXPECT_INITIAL`, `EXPECT_VOWEL`, `EXPECT_COMPOUND_VOWEL_CONTINUATION`, `EXPECT_TENSE_CONSONANT`, `EXPECT_FINAL_OR_NEXT_INITIAL`)로 전이하며, 표준 점자의 점형 충돌을 상태로 해소합니다.

| 충돌 | 해결 방식 |
|------|----------|
| ㄷ{2,4} ↔ ㅡ{2,4} | 초성/중성 기대 상태에 따라 조회 테이블 분리 |
| ㅎ{2,4,5} ↔ ㅚ{2,4,5} | 동일 |
| ㅅ{6} ↔ 된소리표{6} | `EXPECT_TENSE_CONSONANT`로 진입 후 **다음 셀**로 구분 |

- `process(dots)` — 음절 완성 시에만 문자 반환, 미완성이면 빈 문자열
- `getPendingDisplay()` — 조합 중인 글자 미리보기 (초성만 있으면 자모, 초성+중성이면 결합 음절)
- `flush()` — 입력 종료 시 마지막 미완성 음절 강제 완성

**미구현**: 겹받침(ㄳ/ㄵ/ㄺ/ㄻ/ㄼ/ㄽ/ㄾ/ㄿ/ㅀ/ㅄ) — 점형만 `finalConsonantTable`에 추가하면 상태 머신 수정 없이 동작합니다.

### `BrailleViewModel`
`BrailleMode` 5단계 전이, TTS 안내, 자동완성/오타교정 후보 관리를 담당합니다.
왼쪽 스와이프 되돌리기는 확정된 셀 목록(`confirmedCells`)을 **처음부터 재연산**(`replayCells`)해 상태를 복원합니다.

### `TtsManager`
Hilt `@Singleton`. `speak()`는 이전 발화를 끊고(QUEUE_FLUSH), `speakQueued()`는 뒤에 이어 붙입니다(QUEUE_ADD). TTS 엔진 초기화 전 호출된 텍스트는 보관했다가 준비되면 재생합니다. `MainActivity.onDestroy()`에서 `isFinishing`일 때만 `shutdown()` 합니다.

---

## 실행 환경

- minSdk: 24 (Android 7.0)
- compileSdk / targetSdk: 35
- Language: Kotlin
- JVM Target: 11

---

## 주의사항

이 앱은 **UX 데모 목업**입니다. 실제 금융 거래나 카카오뱅크 계정 정보와 무관합니다.
