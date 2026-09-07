# 공개 이관 태스크와 판단 이력

## 1. 공개 경계를 먼저 고정한다

### 1.1 개인 room과 공개 저장소를 물리적으로 분리한다

#### 1.1.1 새 `iphone-to-galaxy/` staging만 공개 후보로 삼는다 — 완료

기존 room에는 연속성 메모리, 기기 상태, 설치 APK, keystore와 시험 영상이 함께 있다. `.gitignore`
만 믿고 room 자체를 저장소로 만들면 미래의 강제 추가나 규칙 변경으로 유출될 수 있다. 별도 디렉터리를
만들고 허용한 파일만 복사했다. 공개 검사는 항상 이 디렉터리를 기준으로 한다.

#### 1.1.2 개인 식별 흔적과 배포 금지 바이너리를 검색한다 — 1차 완료

집·회사 위치, 개인 AirPods 이름, macOS 사용자 절대 경로, Bluetooth 주소, serial, 암호·서명 옵션을
검색했다. 앱 소스에는 위치·주소가 없었지만 AirPods 설계·결과 문서에 개인 제품명이, 기존 build
스크립트에는 절대 SDK 경로와 로컬 서명이 있었다. 제품명은 일반 시험명으로 바꾸고 빌드를 공통
도구로 교체했다. Git 추적 직전에 두 번째 검사를 실행한다.

### 1.2 권리와 안전 경계를 고정한다

#### 1.2.1 제3자 코드와 이미지의 출처를 분리한다 — 완료

HiddenApiBypass 6.1은 Maven 원본 URL과 SHA-256을 빌드에 고정하고 binary는 추적하지 않는다.
AirPods turntable은 polyman CC BY 4.0 모델의 파생 렌더임을 소스와 APK notice 양쪽에 유지한다.
Apple 공식 비교 영상·캡처, MaterialPods 및 다른 앱의 APK·자산은 공개본에 복사하지 않았다.

#### 1.2.2 프로젝트 라이선스를 확정한다 — 소유자 확인 대기

코드 Apache-2.0, 문서 CC BY 4.0을 제안했다. 이것은 공개 권리를 부여하는 선택이므로 자동으로
확정하지 않는다. 원격 공개 직전에 소유자의 답을 받아 실제 전문과 NOTICE를 추가한다.

## 2. 공통 의존성을 먼저 재구축한다

### 2.1 세 앱이 사용자 컴퓨터 경로 없이 빌드되게 한다

#### 2.1.1 SDK 탐색과 debug signing을 공통 도구로 옮긴다 — 완료

기존 세 스크립트는 한 Mac의 사용자별 Android SDK 절대 경로와 앱별 keystore에 묶여 있었다. 환경 변수,
macOS/Linux 기본 SDK 위치, Platform/Build Tools 존재를 확인하는 `tools/android-env.sh`를 만들었다.
생성되는 `.local/debug.jks`는 공개 배포 키가 아니고 Git에서 제외된다. 이 구조가 실패하면 개별 앱
UI를 고치기 전에 공통 도구부터 수정한다.

#### 2.1.2 깨끗한 상태에서 앱 세 개와 core/policy 검사를 실행한다 — 완료

Tap to Top, Holiday Sleep, AirPods Glance 순서는 제품 중요도가 아니라 빌드 복잡도 증가 순이다.
마지막 앱은 checksum dependency, 262,649 core assertions와 APK permission gate까지 확인한다.
산출물은 추적하지 않는다.

2026-09-07 공개 staging에서 세 debug APK가 생성·서명 검증됐다. AirPods dependency SHA-256,
262,649 assertions와 forbidden permission/manifest policy도 통과했다. 생성된 `build/`, `.local/`,
`deps/`는 모두 ignored 상태이며 Git index에는 들어가지 않았다.

## 3. 문제 중심 탐색 구조를 만든다

### 3.1 사용자가 앱 이름을 몰라도 입구를 찾게 한다

#### 3.1.1 README 문제 지도와 여섯 문제 문서를 연결한다 — 완료

각 문서는 iPhone 경험, Galaxy 차이, 검토 단계, 선택, 권한·부작용, 되돌리기, 검증 범위와 미확정을
같은 순서로 쓴다. 설정의 메뉴명이 One UI 업데이트로 달라질 수 있으면 정확한 문자열을 꾸며내지
않고 목표 설정과 현재 검증 버전을 함께 쓴다.

#### 3.1.2 설정 레시피를 개인 위치 없이 일반화한다 — 완료, 세부 기기 재확인 표기

귀가·외출 루틴에는 실제 집/회사 주소를 넣지 않는다. 독자는 자기 장소나 신뢰 Wi-Fi를 선택해야
한다. 회의·수면·경보·제스처도 사용자의 안전 선택을 기본값처럼 강요하지 않는다.

## 4. 결과를 예측하고 공개 게이트를 시험한다

### 4.1 예상 결과를 먼저 적는다

#### 4.1.1 저장소와 앱의 예상 상태를 기록한다 — 완료

예상은 세 앱의 로컬 debug APK 생성, AirPods core/policy 통과, 금지 문자열·binary 0건, Git 추적
후 ignored 산출물만 남는 것이다. 예상과 다른 항목은 지우지 않고 결과 문서에서 원인과 설계 반영을
연결한다.

예상과 실제는 일치했다. 139개·약 5 MiB의 공개 파일만 index에 올랐고, 허용한 runtime PNG/WebP
네 개 외에 APK·AAR·JAR·keystore·영상·로그는 추적되지 않았다. Markdown 상대 링크, shell 문법과
GitHub YAML도 별도로 검사했다.

### 4.2 원격 공개 직전 사람의 결정을 받는다

#### 4.2.1 저장소 소유권·URL·라이선스·최초 배포 범위를 확인한다 — 대기

GitHub 계정과 공개 저장소 생성은 외부 상태 변경이며 소유자의 선택이다. 최초 버전은 source-first를
권장하고, 설치 APK release는 다기기 검증과 배포 서명·업데이트 경로를 정한 뒤 별도 결정한다.
