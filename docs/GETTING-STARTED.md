# PC 준비부터 Galaxy에 앱 설치하기

Galaxy 설정이나 Good Lock만 따라 한다면 [설정 가이드](../recipes/)부터 읽으면 됩니다.
여기는 PC에서 ADB로 폰을 연결하거나, 이 저장소의 앱을 직접 빌드해 설치하려는 사람을 위한 안내입니다.
바로 내려받아 설치하는 공식 APK는 아직 배포하지 않았습니다.

## 1. PC에서 ADB 준비하기

[Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools)를
자신의 운영체제에 맞게 내려받아 압축을 풉니다. Android Studio를 사용한다면 SDK Manager에서
Platform-Tools를 설치할 수도 있습니다.

`platform-tools` 폴더에서 터미널을 열고 다음 명령으로 실행을 확인합니다.

- macOS·Linux: `./adb version`
- Windows PowerShell: `.\adb.exe version`

이 폴더를 PATH에 추가하면 다른 폴더에서도 `adb`를 실행할 수 있습니다. 이후 명령 예시는 PATH에
등록한 상태를 기준으로 합니다. 등록하지 않았다면 `adb`를 실제 실행 파일의 경로로 바꿔 주세요.

macOS는 보통 별도 USB 드라이버가 필요하지 않습니다. Windows에서 Galaxy를 인식하지 못하면
[Samsung USB 드라이버](https://developer.samsung.com/android-usb-driver)를 확인합니다.
Ubuntu에서는 USB 접근 권한 설정이 필요할 수 있으므로 [Android 연결 안내](https://developer.android.com/studio/run/device)를
참고합니다.

## 2. 폰에서 개발자 옵션과 USB 디버깅 켜기

1. 설정 → 휴대전화 정보 → 소프트웨어 정보 → 빌드번호를 일곱 번 누릅니다.
2. 잠금 인증을 요청하면 입력하고, 설정 첫 화면에서 개발자 옵션을 엽니다.
3. USB 디버깅을 켠 뒤 데이터 전송이 가능한 케이블로 PC와 연결합니다.
4. 폰의 잠금을 풀고 PC에서 `adb devices`를 실행합니다.
5. 폰의 USB 디버깅 승인 창에서 연결한 PC를 허용합니다.
6. `adb devices`를 다시 실행해 기기 옆에 `device`가 표시되는지 확인합니다.

이 순서는 [Android 개발자 옵션 안내](https://developer.android.com/studio/debug/dev-options)의
Galaxy 메뉴 경로와 연결 승인 절차를 참고했습니다. One UI 버전과 관리 정책에 따라 메뉴가 다를 수
있습니다. 루팅, 부트로더/OEM 잠금 해제와 무선 디버깅은 이 USB 연결 절차에 필요하지 않습니다.

## 3. 연결이 안 될 때

| 보이는 상태 | 확인할 곳 |
|---|---|
| `adb`를 찾을 수 없음 | Platform-Tools 설치 위치와 PATH를 확인하거나 실행 파일 경로로 실행합니다. |
| 기기 목록이 비어 있음 | 데이터 케이블·USB 포트·폰 잠금·USB 디버깅과 PC 드라이버를 확인합니다. |
| `unauthorized` | 폰을 잠금 해제해 디버깅 승인을 확인합니다. 창이 안 뜨면 케이블을 다시 연결합니다. |
| `offline` | 케이블을 다시 연결합니다. 계속되면 ADB 서버를 재시작하고 상태를 확인합니다. |
| USB 명령·설치가 보안 기능으로 차단됐다는 안내 | 아래의 보안 차단 항목을 확인합니다. |
| 기기가 여러 대 표시됨 | 설치할 폰 한 대만 USB로 연결하거나 `adb -s DEVICE_ID …`로 대상을 지정합니다. |

ADB 서버 재시작이 필요하면 `adb kill-server` 다음 `adb start-server`를 실행합니다. 이때 같은
PC에서 진행 중인 다른 ADB 연결도 잠시 끊깁니다. 계속 승인되지 않으면 폰에서 ‘USB 디버깅 승인
취소’를 누르고 다시 연결해 승인할 수 있습니다. 기존에 승인한 다른 PC도 다시 승인해야 합니다.

Galaxy에 ‘보안 위험 자동 차단’ 또는 USB 명령 차단 안내가 나타나면 **설정 → 보안 및 개인정보
보호**에서 해당 기능의 설명과 차단 이유를 확인합니다. 차단 설정은 One UI 버전에 따라 다릅니다.
실제 차단 안내가 있을 때 필요한 범위의 임시 해제를 판단하고, 작업 뒤 원래 설정으로 되돌립니다.

회사에서 관리하는 폰에서 개발자 옵션이나 설치가 제한됐다면 관리자 정책을 먼저 확인합니다.
일반적인 연결 준비를 위해 보안 기능을 일괄 해제할 필요는 없습니다.

## 4. 소스를 빌드하고 설치하기

ADB는 설치와 연결 도구이고, 소스를 APK로 만드는 데에는 추가 도구가 필요합니다. 현재 빌드는
macOS에서 확인했고 Ubuntu의 GitHub Actions에서도 검사합니다. Windows 네이티브 빌드는 아직
검증하지 않았습니다. WSL을 쓰더라도 USB 연결에는 별도 설정이 필요할 수 있습니다.

빌드를 하려면 Git, JDK 17, Android SDK Platform 36, Build Tools 36.0.0, 셸, zip·unzip·curl을
준비합니다. Android Studio의 SDK Manager에서 필요한 SDK를 설치할 수 있습니다. Android Studio
설치만으로 터미널의 `java`·`javac`가 준비됐다고 가정하지 말고 버전을 확인해 주세요.

SDK가 기본 위치에 없다면 실제 SDK 경로를 `ANDROID_SDK_ROOT`로 지정합니다.
Platform 36은 빌드에 쓰는 SDK 버전이며, 이 저장소의 실기기 시험 환경은 Android 17·One UI 9.0입니다.

```sh
git clone https://github.com/chochoq/iphone-to-galaxy.git
cd iphone-to-galaxy
java -version
javac -version
adb devices
```

설치하려는 앱 하나를 골라 저장소 루트에서 해당 명령을 실행합니다.
`adb -d`는 USB로 연결된 기기를 대상으로 합니다. USB 기기를 여러 대 연결했다면 대상을 지정해야 합니다.

맨 위로 톡:

```sh
./apps/tap-to-top/build.sh
adb -d install apps/tap-to-top/build/tap-to-top.apk
```

공휴일 수면 연장:

```sh
./apps/holiday-sleep/build.sh
adb -d install apps/holiday-sleep/build/holiday-sleep.apk
```

에어팟 한눈에:

```sh
./apps/airpods-glance/test-core.sh
./apps/airpods-glance/build.sh
./apps/airpods-glance/verify-apk.sh
adb -d install apps/airpods-glance/build/android/airpods-glance.apk
```

이미 같은 앱이 설치돼 있다면 같은 서명 키로 빌드한 경우에만 `adb -d install -r APK_PATH`로
업데이트할 수 있습니다. 빌드할 때 생기는 각 앱의 `.local/` 키는 보관하되 Git에 올리지 마세요.
다른 PC나 다른 사람이 빌드한 APK와는 서명이 다를 수 있습니다.

서명 불일치 오류가 나면 바로 앱을 삭제하지 말고 기존 설치 경로와 키를 확인합니다. 삭제 후 재설치는
앱 설정과 데이터가 지워질 수 있습니다.

## 5. 앱을 열어 권한 허용하기

설치 후 앱을 직접 열어 [README의 앱별 권한 표](../README.md#usb-디버깅-다음에는-어떤-권한을-열어야-하나요)에
있는 항목을 허용합니다. 캘린더 권한을 켜는 것과 대한민국 공휴일 캘린더를 동기화하는 것도 별개입니다.
공휴일 수면 연장은 [수면 설정 가이드](../recipes/holiday-extension.md)를 함께 따라가야 합니다.

맨 위로 톡의 접근성 설정이 ‘제한된 설정’으로 막히면 해당 앱의 정보 화면에서 제한된 설정 허용
여부를 확인합니다. 이 항목은 설치 방법과 OS에 따라 나타나지 않을 수도 있습니다. 검토한 소스를
직접 빌드한 앱인지 확인한 뒤 허용하고 접근성 설정으로 돌아갑니다.

ADB로 설치하는 것과 폰의 파일 관리자에서 APK를 여는 것은 다른 경로입니다. 파일 관리자로 설치할
때는 해당 앱에 ‘출처를 알 수 없는 앱 설치’ 허용이 필요할 수 있지만, USB 디버깅의 필수 설정으로
모든 앱에 이 권한을 줄 필요는 없습니다.

설정과 시험을 마쳤다면 USB 디버깅을 끄고 케이블을 분리합니다. 임시로 바꾼 USB 차단 설정도
원래대로 돌립니다. 앱의 일상 기능은 PC 연결에 의존하지 않으며, 각 앱의 권한은 계속 필요합니다.

## 확인 범위

2026-09-07 작성했습니다. 개발자 옵션·연결 승인은 Android 공식 문서와 대조했고, 앱 권한과 APK
경로는 저장소의 코드·빌드 스크립트로 확인했습니다. 새 사용자 폰에서 이 안내 전체를 처음부터
따라 하는 시험과 Windows 빌드는 아직 하지 않았습니다.
