# FIN:CLUE SDK 개발 순서

> 기준: `yaeunjess/SensAble` 목업(현재 상태) + FIN:CLUE 아키텍처 로드맵(Layer 1~4, 시퀀스 다이어그램)
> 목표: 목업 앱 1개 → **은행이 붙이는 `.aar` SDK + 데모 은행앱** 2개로 분리

---

## 큰 그림

지금 있는 것과 만들어야 할 것의 차이는 **기능**이 아니라 **경계**다.

점자 디코더, 상태 머신, 6점 그리드, TTS, 햅틱, 송금 플로우 — 이미 다 동작한다.
없는 것은 "여기까지가 SDK고 여기부터는 은행 것"이라는 선 하나다.
그래서 이 계획의 전반부는 신규 개발이 아니라 **절개(絶開)** 작업이다.

```
[현재]  app  ─ 점자 UI + 은행 화면 + 송금 로직 전부 한 덩어리

[목표]  finclue-sdk (.aar) ─ 점자 입력 · TTS/진동 · Assist · Secure DB
        demo-app          ─ 은행 화면 · 송금 로직 · 지문인증
                            └ implementation(project(":finclue-sdk"))
```

---

## Phase 0 — 시작 전에 (최우선)

코드 짜기 전에 이것부터. **여기서 막히면 뒤 계획이 전부 바뀐다.**

### 0-1. TalkBack 충돌 검증 ★ 최우선

지금 목업은 TalkBack을 끈 상태로 테스트했을 가능성이 높다.
TalkBack이 켜지면 explore-by-touch가 터치를 가로채서 6점 탭과 커스텀 스와이프가 앱까지 안 내려온다.

- 실기기에 TalkBack 켜고 현재 목업 실행 → 6점 입력과 4가지 제스처가 그대로 되는지 확인
- 안 되면 대응안 3가지 중 선택:
  1. 점자 모드 진입 시 해당 View를 접근성 트리에서 제외 (`importantForAccessibility=NO_HIDE_DESCENDANTS`)
  2. IME(입력기)로 구현 — 구글 점자 키보드 방식. 확실하지만 사용자가 시스템 설정에서 켜야 함
  3. "점자 모드 중에는 TalkBack을 잠시 꺼주세요" 음성 안내 폴백

> 실사용자는 100% TalkBack을 켜고 산다. 이게 안 풀리면 SDK 자체가 성립 안 하므로 무조건 1번으로 확인.

### 0-2. 개발환경 세팅 문서 

| 항목 | 값 | 비고 |
|---|---|---|
| minSdk | 24 | 은행 앱 호환 하한. 올리지 말 것 |
| compileSdk / targetSdk | 35 | 현 리포 유지 |
| JVM Target | 11 | 현 리포 유지 |
| AGP / Kotlin / Gradle | 현 리포 값 그대로 고정 | 각자 임의 업그레이드 금지 |
| 의존성 버전 | `gradle/libs.versions.toml` 한 곳에서만 관리 | 개별 build.gradle에 직접 버전 쓰지 않기 |

추가로 문서에 넣을 것: JDK 설치 버전, Android Studio 버전, 실기기 필수(에뮬레이터는 햅틱·TTS·TalkBack 검증 불가), TalkBack 켜는 법.

### 0-3. 브랜치 전략

리포는 그대로 쓰고 `feat/sdk-extraction` 브랜치에서 작업.
`main`의 목업은 **시연용으로 살려둔다.** 절개 중에는 데모가 깨지는 구간이 생기므로 발표용 안전판이 필요하다.

---

## Phase 1 — 모듈 분리 + 계약 확정

### 1-1. 빈 모듈 연결

```
SensAble/
├── app/            (→ 나중에 demo-app 역할)
├── finclue-sdk/    (신규, com.android.library)
└── settings.gradle.kts  → include(":finclue-sdk")
```

`app`의 build.gradle에 `implementation(project(":finclue-sdk"))`.

**완료 기준:** `Finclue.runFlow()`가 토스트만 띄우는 껍데기 상태로, `app`에서 호출되고 빌드가 통과한다.

### 1-2. 공개 계약 확정 ★ 되돌리기 어려운 결정

한 번 공개하면 호환성 때문에 못 바꾼다. 여기에 시간을 제일 많이 써야 한다.

**public으로 낼 것 — 이것만:**
- `Finclue` (파사드): `shouldAssist` / `runFlow` / `onFlowSuccess`
- `FlowSpec`, `FieldSpec`, `FieldType`

**나머지 전부 `internal`.** FlowEngine, FieldController, BrailleDecoder, StateMachine, Assist, SecureDB 다 숨긴다.

**설계 구멍 하나 — 반드시 여기서 해결:**
현재 목업의 `SERVICE_SELECT`(송금↔잔액조회)와 `TRANSFER_CONFIRM`(예/아니요)은
"값을 입력받는 필드"가 아니라 **선택지 순환**이라 `FlowSpec`으로 표현이 안 된다.

→ `FieldType.CHOICE`를 추가해서 선택지도 필드로 일반화한다.
확인 문구("○○님에게 5만원… 이체 후 잔액은 45만원")는 **은행이 계산해서 `prompt`로 내려준다.**
SDK는 잔액을 알면 안 된다.

**FieldType별 Assist 정책도 여기서 확정:**

| FieldType | 자동완성 | 오타교정 | 비고 |
|---|---|---|---|
| `NAME` | O | O | 실존 수취인 필터 |
| `ACCOUNT` | 이력 기반만 | **X** | 자유 숫자 교정 = 오송금 위험 |
| `AMOUNT` | **X** | **X** | 22만원→220만원 되면 금융사고. 확정 직전 TTS 복창 강제 |
| `CHOICE` | - | - | 순환 선택 |

### 1-3. 누락 기능 정의

- **한 글자 지우기**가 지금 없다. 왼쪽 스와이프는 취소/뒤로에 이미 쓰이고 있으므로 계좌번호 12자리 중 8번째를 틀렸을 때 복구 경로가 없다. 제스처를 새로 정하거나 왼쪽 스와이프 단계 동작을 재정의할 것.
- **실패 경로**: `runFlow`가 성공 result만 돌려준다. 사용자 취소·타임아웃·중단 콜백이 없으면 은행은 붙이질 못한다.
- **세션 타임아웃**: 점자 입력은 키패드보다 느리다. 가설정의서의 "입력 제한 시간 초과로 처음부터 다시"라는 문제를 우리가 그대로 재현할 수 있다. FlowSpec에 진행 중 콜백이나 예상 소요시간 힌트를 넣어 호스트 앱이 keepalive 하게 만든다.

---

## Phase 2 — 입력 코어 이관

여기부터는 **이미 동작하는 코드를 옮기는 것**이라 난이도가 낮다. 신규 로직 추가 금지.

이동 대상:
- `core/braille/BrailleDecoder.kt`
- `core/braille/KoreanBrailleStateMachine.kt`
- `core/designsystem/component/BrailleGrid.kt`
- `core/tts/TtsManager.kt`

**옮기면서 반드시 처리할 것:**

1. **Hilt 제거.** `TtsManager`가 `@Singleton`인데, 라이브러리가 Hilt를 요구하면 은행 앱도 Hilt를 써야 한다. SDK로서 실격. 내부는 수동 생성이나 `internal object`로 바꾼다. (demo-app은 Hilt 계속 써도 됨)
2. **패키지 변경** `com.sensable.app.core.*` → `com.finclue.sdk.*`
3. **리소스 접두사** build.gradle에 `resourcePrefix = "finclue_"`, 리소스명 전부 개명
4. **의존성은 `implementation`으로.** `api`로 선언하면 은행 앱 클래스패스로 새어나가 버전 충돌
5. **consumer-rules.pro** 작성 — 은행 앱이 난독화해도 공개 API 이름이 살아있게

**완료 기준:** 데모앱에서 점자 입력 → 글자 조합 → TTS·진동까지 예전과 똑같이 동작. **이 시점에 TalkBack 켜고 재검증.**

---

## Phase 3 — 절개 (가장 까다로운 구간)

`BrailleBottomSheet`가 5단계를 전부 처리하고 있다. 이걸 갈라야 한다.

| 현재 단계 | 목적지 |
|---|---|
| SERVICE_SELECT | 은행앱이 `CHOICE` 필드로 요청 |
| TRANSFER_RECIPIENT | **SDK** (NAME/ACCOUNT 필드) |
| TYPO_CORRECTION | **SDK** (Assist 내부 동작) |
| TRANSFER_AMOUNT | **SDK** (AMOUNT 필드) |
| TRANSFER_CONFIRM | 은행앱이 `CHOICE` 필드로 요청, 문구는 은행이 생성 |
| **FingerprintAuthOverlay** | **은행앱 (절대 SDK 아님)** |
| TransferComplete | 은행앱 |
| `MOCK_BALANCE`, TransferRepository | 은행앱 |

> 지문인증을 SDK에 넣으면 안 되는 이유: 실제 은행은 FIDO/생체인증을 자체 보안모듈로 갖고 있다. 서드파티 SDK가 인증 UI를 대신 띄운다고 하면 보안심사에서 반려된다. SDK는 값만 넘기고 빠진다.

**UI 컨테이너 결정:** MVP는 전체화면 `DialogFragment`(내부에 `ComposeView`) 권장.
Activity로 하면 프로세스 사망 시 콜백 람다가 날아가고, DialogFragment는 호스트 매니페스트 수정도 불필요하다.
단 `context`가 `FragmentActivity`여야 한다는 제약을 문서에 명시.

**완료 기준:** 데모앱이 SDK를 "외부 라이브러리처럼" 호출해서 송금 전 과정이 완주된다.
→ **여기가 진짜 MVP.** DB도 AI도 없지만 은행 붙이기 데모가 가능한 최소 단위.

---

## Phase 4 — Secure DB (온디바이스)

Room + SQLCipher, 키는 Android Keystore.

```
suggestion(suggestGroup, label, value, freq, last_used)
confusion_matrix(source_jamo, target_jamo, freq)
```

- 목업의 하드코딩 이름 풀(김봄, 김보미, 김별…) → DB seed 데이터로 이전
- **Room + SQLCipher 버전 궁합이 까다롭다.** 반드시 `libs.versions.toml`에 고정하고 팀에 공유
- `clearUserData()` 파기 API와 저장 opt-out을 **지금** 넣어둘 것. 금융사 보안심사에서 반드시 묻는다
- 계좌번호를 평문 value로 저장하지 않기

---

## Phase 5 — Assist (AI 요소)

### 5-1. 자동완성 — 쉬운 쪽 먼저

Trie + Score(α·freq + β·recency) → Top-3.
**이건 AI 모델이 아니라 자료구조라서 네트워크 없이 100% 성립한다. 정확도 이슈 자체가 없다.**

진짜 문제는 **cold start**다. 신규 사용자는 DB가 비어서 첫 몇 주간 제안이 0개다. 시연에서 제일 티난다.
→ 최초 실행 시 은행 앱이 최근 이체 내역을 `seed`로 넘겨주는 API를 두거나, 튜토리얼에서 자주 쓰는 수취인을 미리 등록하게 한다.

### 5-2. 오타교정 — 여기가 "네트워크 없는 AI" 쟁점

더미데이터 문제는 **합성 데이터로 풀린다.** 점자 오타는 랜덤이 아니라 물리적으로 예측 가능하기 때문이다.

- 인접 점 오탭 (1↔4, 2↔5, 3↔6)
- 점 누락 / 점 추가
- 셀 확정 타이밍 실수

한글 이름·은행명 사전을 점자로 인코딩한 뒤 이 노이즈를 확률적으로 주입하면 학습셋을 오프라인에서 원하는 만큼 생성할 수 있다. 실사용자 데이터 없이 seed confusion matrix가 나오고, 이후 온디바이스 개인 오타 패턴으로 재랭킹되는 게 로드맵 설계와 정확히 맞는다.

**단, LSTM은 나중에.**
실존 이름 필터가 이미 후보 공간을 좁혀주므로 **confusion 가중 편집거리**만으로 상당 부분 해결된다.
먼저 편집거리 버전을 붙여 정확도를 재고, LSTM이 실제로 얼마나 이기는지 수치로 확인한 다음 도입한다.
TFLite를 초반에 넣으면 `.aar` 용량·ABI 문제까지 딸려와서 감당이 안 된다.

**완료 기준:** 비행기 모드에서 전 기능 동작. 이게 우리 아이디어의 핵심 주장이므로 시연 전 반드시 확인.

---

## Phase 6 — 패키징 & 사업화 준비

- `./gradlew :finclue-sdk:assembleRelease` → `.aar` 산출
- 연동 가이드 작성: 3개 메서드 사용법, FlowSpec 작성법, 최소 요구사항(minSdk 24, FragmentActivity)
- **매니페스트 문구 수정**: "manifest INTERNET 미선언"은 병합되면 호스트 앱에 권한이 있으므로 심사에서 반박당한다.
  → **"SDK 내부에 네트워크 호출 경로 없음 (의존성·코드 수준 검증 가능)"** 이 정확하고 더 강한 표현
- 대출·카드·자산조회 확장 로드맵

---

## 예은 / 팀원 병렬 분담 제안

Phase 1의 **계약 확정(1-2)까지는 반드시 둘이 같이** 한다. 여기가 갈리면 나중에 전부 충돌한다.
계약이 확정된 뒤부터 병렬 가능:

| 트랙 A | 트랙 B |
|---|---|
| Phase 2~3 (모듈 이관 · 절개 · UI) | Phase 4~5 (Secure DB · Assist) |
| 인터페이스에만 의존 | 스텁 구현으로 먼저 개발 |

Assist는 `interface Assist { fun suggest(prefix): List<String> }` 정도만 먼저 확정해두면 A는 더미 구현으로, B는 실제 구현으로 각자 진행할 수 있다.

---

## 하지 말아야 할 것

- **처음부터 완벽한 API 만들려고 붙잡고 있기** — 계약은 신중히, 구현은 빠르게
- **LSTM/TFLite 먼저 손대기** — Phase 5-2까지 미룬다
- **지문인증·잔액·송금 로직을 SDK로 끌어오기** — 편해 보여도 보안심사에서 전부 반려
- **TalkBack 검증 미루기** — 나중에 터지면 전체 아키텍처가 바뀐다
- **모듈 분리를 나중에 하기** — 지금이 가장 싸다

---

## 우선순위 한 줄 요약

**TalkBack 검증(0-1) → 계약 확정(1-2) → 절개해서 데모 완주(Phase 3)** 까지가 임계경로다.
DB와 AI는 그 뒤에 붙는 살이고, 없어도 SDK는 이미 SDK다.
