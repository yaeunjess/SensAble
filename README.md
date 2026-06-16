# SensAble

> 시각장애인을 위한 점자 인터페이스 SaaS 목업 — KakaoBank UI 기반

---

## 소개

SensAble은 카카오뱅크 UI 위에 **점자 인터페이스**를 오버레이하는 안드로이드 앱 목업입니다.
시각장애인 사용자가 3×2 점자 버튼과 한국어 TTS 음성 안내만으로 송금 등 금융 서비스를 이용할 수 있는 UX를 시연합니다.

---

## 주요 기능

- **점자 인터페이스 BottomSheet** — 홈 화면에서 위로 스와이프하면 점자 입력 패널 등장
- **한국어 TTS 음성 안내** — 각 단계마다 음성으로 사용자를 안내
- **송금 플로우** — 수신인 입력 → 금액 입력 → 확인의 3단계 목업 구현

---

## 스크린샷

<!-- 추후 추가 -->

---

## 기술 스택

| 분류 | 사용 기술 |
|------|----------|
| UI | Jetpack Compose + Material3 |
| 아키텍처 | MVVM + Clean Architecture |
| DI | Hilt |
| 내비게이션 | Navigation Compose |
| 비동기 | Kotlin Coroutines + StateFlow |
| TTS | Android TextToSpeech (한국어) |

---

## 송금 플로우

```
홈 화면 → 위로 스와이프
  → 점자 BottomSheet 등장 ("어떤 서비스를 이용하시겠습니까?")
    → 버튼 1 (송금) 선택
      → 수신인 입력 ("누구에게 보낼까요?")
        → 금액 입력 ("얼마를 보낼까요?")
          → 최종 확인 ("송금하는게 맞습니까?")
```

---

## 프로젝트 구조

```
com.sensable.app/
├── core/           # 공통 컴포넌트, 내비게이션, 디자인 시스템
├── data/           # Repository 구현체, DI 모듈
├── domain/         # 도메인 모델, UseCase, Repository 인터페이스
└── feature/
    ├── kakaobank/  # 카카오뱅크 홈 화면 목업
    ├── braille/    # 점자 인터페이스 BottomSheet
    └── transfer/   # 송금 플로우
```

---

## 실행 환경

- minSdk: 24 (Android 7.0)
- targetSdk: 36
- Language: Kotlin

---

## 주의사항

이 앱은 **UX 데모 목업**입니다. 실제 금융 거래나 카카오뱅크 계정 정보와 무관합니다.
