# Sensable 프로젝트 초기 환경 세팅 가이드

## 📋 사전 요구사항

### 시스템 요구사항
- **Windows 10/11** (Mac 또는 Linux도 가능)
- **RAM**: 최소 8GB (권장 16GB 이상)
- **디스크 공간**: 최소 20GB (안드로이드 SDK 포함)
- **인터넷**: 필수 (SDK 다운로드)

### 이 프로젝트의 기술 스택
| 항목 | 버전 | 설명 |
|------|------|------|
| **Java/JDK** | 11 이상 | Kotlin 컴파일에 필수 |
| **Kotlin** | 1.9.0+ | 안드로이드 공식 언어 |
| **Android SDK** | API 35 (targetSdk) | 최신 안드로이드 지원 |
| **Gradle** | 8.0+ | 빌드 도구 (자동 설치됨) |

---

## 🚀 단계별 설치 가이드

### 1️⃣ Java Development Kit (JDK 11) 설치

#### Windows 환경

**Option A: Oracle JDK 설치 (권장)**
1. [Oracle JDK 다운로드](https://www.oracle.com/java/technologies/downloads/#java11) 이동
2. **Windows x64 Installer** 다운로드 (`jdk-11.x.x_windows-x64_bin.exe`)
3. 다운로드한 파일 실행 후 기본 설정으로 설치
   - 설치 경로: `C:\Program Files\Java\jdk-11.x.x\` (기본값)

**Option B: Eclipse Temurin JDK 설치 (무료)**
1. [Eclipse Temurin 다운로드](https://adoptium.net/temurin/releases/) 이동
2. **Java 11 LTS** 버전의 **Windows x64 MSI Installer** 다운로드
3. 다운로드한 파일 실행 후 기본 설정으로 설치

#### 설치 확인
PowerShell 또는 cmd를 열고 다음 명령어 실행:
```bash
java -version
```

출력 예시:
```
java version "11.0.x"
Java(TM) SE Runtime Environment (build 11.0.x+x)
Java HotSpot(TM) 64-Bit Server VM (build 11.0.x+x)
```

> ❌ 버전이 11 이상이 아니면 설치가 잘못된 것입니다. 다시 확인하세요.

---

### 2️⃣ Android Studio 설치

1. [Android Studio 공식 다운로드](https://developer.android.com/studio) 이동
2. **Download Android Studio Koala (또는 최신 버전)** 클릭
3. 다운로드한 `.exe` 파일 실행

**설치 옵션 선택 화면에서:**
- ✅ **Android SDK** (필수)
- ✅ **Android SDK Platform** (필수)
- ✅ **Android Virtual Device** (에뮬레이터용, 선택사항)

4. 설치 완료 후 **Android Studio** 실행
5. 첫 시작 시 추가 SDK 다운로드 진행 (네트워크 필요, 10~20분 소요)

> 💡 **설치 중 느려도 기다리세요.** 첫 시작은 항상 느립니다.

---

### 3️⃣ 프로젝트 열기

#### A. 프로젝트 코드 받기

**Git 사용 (권장)**
```bash
git clone <repository-url>
cd Sensable
```

또는 프로젝트 폴더를 직접 받은 경우:
```bash
cd C:\Users\<YourUsername>\AndroidStudioProjects\Sensable
```

#### B. Android Studio에서 프로젝트 열기

1. **Android Studio** 실행
2. **File → Open** 또는 **Open an existing Android Studio project** 클릭
3. Sensable 프로젝트 폴더 선택
4. **Open** 클릭

---

### 4️⃣ Gradle 싱크 (프로젝트 초기화)

프로젝트를 열면 우측 하단에 **Gradle 동기화** 알림이 나타납니다.

**자동 동기화가 시작되지 않으면:**
1. **File → Sync Now** 클릭 또는
2. 상단 메뉴에서 **File → Sync Project with Gradle Files** 선택

**진행 상황:**
- 우측 하단 **Gradle** 탭에서 진행률 확인
- 처음 동기화는 **3~10분** 소요될 수 있습니다
- 진행 중 **인터넷 끊김 방지**, 재시작하지 않기

> ✅ **"Gradle build finished"** 메시지가 나오면 완료!

---

### 5️⃣ 프로젝트 구조 확인

Android Studio 좌측 **Project** 탭에서 다음 폴더 구조를 확인:

```
Sensable/
├── app/                    # 앱 소스 코드
│   └── src/main/
│       ├── java/          # Kotlin 코드
│       └── AndroidManifest.xml
├── gradle/                # Gradle 설정
├── build.gradle.kts       # 프로젝트 설정
├── CLAUDE.md              # 프로젝트 문서
├── SETUP_GUIDE.md         # 이 파일
└── sdk_개발_순서.md       # SDK 개발 순서 가이드
```

---

## 🔨 빌드 및 실행

### 빌드하기

1. **Build → Build Project** 클릭 또는
2. 단축키: **Ctrl + F9** (Windows)

**진행:**
- 하단 **Build** 탭에서 진행률 확인
- 처음 빌드는 **5~10분** 소요 가능

> ✅ **"Build completed successfully"** 메시지가 나오면 성공!

### 앱 실행하기

#### Option A: 안드로이드 에뮬레이터에서 실행 (권장)

**Step 1: 에뮬레이터 생성**
1. **Tools → Device Manager** 클릭
2. **Create Virtual Device** 클릭
3. 기본값 선택 후 **Next → Finish**

**Step 2: 앱 실행**
1. **Run → Run 'app'** 클릭 또는 단축키 **Shift + F10**
2. 기기 선택 창에서 생성한 에뮬레이터 선택
3. **Run** 클릭

> 💡 **첫 실행 시 에뮬레이터 부팅에 2~3분 소요됩니다.**

#### Option B: 실제 안드로이드 폰에서 실행

1. USB 케이블로 폰을 컴퓨터에 연결
2. 폰의 **설정 → 개발자 옵션 → USB 디버깅** 활성화
3. **Run → Run 'app'** 또는 **Shift + F10**
4. 연결된 디바이스 선택 후 **Run**

> ⚠️ **개발자 옵션을 찾을 수 없다면:** 설정 → 정보 → 빌드 번호를 7회 탭

---

## 📱 에뮬레이터 설정 (선택사항)

에뮬레이터가 느린 경우 다음 설정으로 성능 개선:

**Settings → Preferences → Tools → Emulator:**
- ✅ **Launch in the Running Devices dialog** 활성화
- ✅ **Show boot animation** 비활성화 (부팅 시간 단축)

---

## ✅ 환경 세팅 확인 체크리스트

다음을 모두 확인했으면 개발 준비 완료:

- [ ] JDK 11 설치 및 `java -version` 확인
- [ ] Android Studio 설치 및 실행 가능
- [ ] Sensable 프로젝트 열기 완료
- [ ] Gradle 동기화 완료 (`Build 탭에서 "Build completed"` 확인)
- [ ] 프로젝트 빌드 성공 (`Build → Build Project` 성공)
- [ ] 에뮬레이터 또는 실제 디바이스에서 앱 실행 가능

---

## 🐛 자주 발생하는 문제 및 해결

### ❌ "Java version not compatible" 에러

**원인:** JDK 버전이 11 미만

**해결:**
1. JDK 11 이상 설치 확인
2. Android Studio → **File → Settings → Build, Execution, Deployment → Android SDK → SDK Tools**
3. **JDK (Android Studio)** 버전 11 이상 확인

---

### ❌ "Gradle sync failed"

**원인:** 인터넷 연결 불안정 또는 저장소 접근 불가

**해결:**
1. 인터넷 연결 확인
2. **File → Invalidate Caches → Invalidate and Restart**
3. 다시 **Sync Now**

---

### ❌ "Emulator launch failed"

**원인:** 가상화 기능 미활성화 또는 메모리 부족

**해결:**
- Windows: BIOS에서 **Virtualization (Intel VT-x 또는 AMD-V)** 활성화
- RAM 부족: 에뮬레이터 메모리 설정 감소 (AVD Manager → Edit → RAM 설정)

---

### ❌ "Cannot connect real device"

**원인:** USB 디버깅 미활성화 또는 드라이버 미설치

**해결:**
1. 폰 설정 → 개발자 옵션 → **USB 디버깅** 활성화
2. 컴퓨터에서 폰 연결 승인 팝업 **허용** 클릭
3. Android Studio → **Tools → Device Manager**에서 디바이스 확인

---
