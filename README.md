# iPhone to Galaxy

> **English:** A field guide by a long-time iPhone user rebuilding familiar habits on a
> Galaxy Z Fold8. Every solution starts with Samsung's own settings; custom apps come last.

아이폰 3GS부터 아이폰만 사용하다가 Galaxy Z Fold8으로 바꿨습니다. 사진과 연락처는 옮길 수
있었지만, 손에 밴 사용법까지 따라오지는 않았습니다. 상태 표시줄을 눌러 맨 위로 가는 동작도,
공휴일 아침에는 수면 모드를 늦게 끝내는 설정도 Galaxy에서는 다시 찾아야 했습니다.

이 저장소에는 그때부터 하나씩 해결해 온 과정을 기록합니다. 불편하다고 바로 앱을 만들지는
않았습니다. Galaxy 설정, Good Lock과 Samsung 앱, 이미 나와 있는 앱, 자동화와 ADB를 차례로
확인했습니다. 직접 만든 앱은 앞선 방법으로 원하는 동작을 만들 수 없을 때만 사용했습니다.

비슷한 앱이 이미 있다면 함께 소개합니다. 우리가 처음 만들었다거나 모든 Galaxy에서 더 잘
동작한다고 주장하지 않습니다. 실제로 시도했다가 실패한 방법과 생각을 바꾼 이유도 설계 문서에
남깁니다. 지금까지 확인한 기존 프로젝트는 [이 문서](docs/LANDSCAPE.md)에 정리했습니다.

## 이 기록의 기준

아이폰 쪽 경험은 제가 마지막으로 사용한 환경을 기준으로 합니다.

- iPhone 사용 시작: iPhone 3GS
- 마지막 기종: iPhone 14 Pro
- 마지막 운영체제: iOS 26.6.1

Galaxy 쪽 결과는 다음 환경에서 직접 확인했습니다.

- 기기: Samsung Galaxy Z Fold8 (`SM-F971N`)
- 운영체제: Android 17
- One UI: 9.0
- 지역과 언어: 대한민국, 한국어

이 정보는 지원 기기 목록이 아닙니다. 현재 시험한 기기가 한 대뿐이라는 뜻입니다.

다른 Galaxy나 다른 One UI에서는 메뉴 이름과 동작이 달라질 수 있습니다. Fold의 커버 화면과
펼친 화면에서도 레이아웃이 달라질 수 있습니다. 확인하지 않은 환경은
[호환성 표](compatibility/)에 지원된다고 적지 않습니다.

## 지금까지 해결한 것

| 불편했던 점 | 사용한 방법 | 확인 상태 |
|---|---|---|
| 상태 표시줄을 눌러 맨 위로 가고 싶다 | [맨 위로 톡](problems/tap-status-bar-to-scroll-top.md) | 직접 만든 앱, 한 기기에서 확인 |
| 주말과 공휴일에는 수면 모드를 정오까지 유지하고 싶다 | [공휴일 수면 연장](problems/holiday-aware-sleep.md) | Samsung 모드와 직접 만든 앱, 한 기기에서 확인 |
| Galaxy에서도 AirPods 배터리와 큰 연결 화면을 보고 싶다 | [Galaxy에서 AirPods 쓰기](problems/airpods-on-galaxy.md) | 실험용 앱, 한 기기에서 확인 |
| 회의 전에 Bluetooth를 끄고 녹음 앱을 한 번에 열고 싶다 | [회의 모드](problems/meeting-mode.md) | Samsung 모드·루틴으로 구성 |
| 큰 화면을 오른손으로 편하게 조작하고 싶다 | [한손 제스처](problems/one-hand-gestures.md) | Good Lock으로 구성 |
| 귀가·외출·배터리 상태에 맞춰 설정을 바꾸고 싶다 | [생활 자동화](problems/contextual-routines.md) | Samsung 루틴으로 구성 |

`한 기기에서 확인`과 `지원`은 다릅니다. 숨은 Android API나 기종별 동작에 기대는 기능에는
`실험용`이라고 따로 표시합니다.

## 어디서 시작하면 되나요?

앱을 설치하지 않고 따라 할 수 있는 설정은 [`recipes/`](recipes/)에 있습니다. 수면 모드,
재난문자, 회의 모드, 집과 외출 루틴, One Hand Operation+ 설정부터 살펴볼 수 있습니다.

직접 만든 앱의 소스와 빌드 방법은 [`apps/`](apps/)에 있습니다. Android SDK Platform 36,
Build Tools 36.0.0과 JDK가 필요합니다. 각 앱의 `./build.sh`가 만드는 APK는 개발 확인용으로
서명되며 배포용 설치 파일이 아닙니다.

## 권한과 개인정보

앱에는 광고와 분석 도구를 넣지 않았고, 실행 중 서버와 통신하지 않습니다. AirPods Glance를
빌드할 때 공개 Maven 저장소에서 의존성 하나를 내려받는 과정은 앱이 실행 중 인터넷을 사용하는
것과 다릅니다. 앱마다 요청하는 권한, 저장하는 정보와 삭제 방법을 README에 적었습니다.

공개할 소스에서는 집과 회사 위치, Bluetooth 주소, 개인 기기 이름, 로그, 서명 키와 화면 녹화를
제외했습니다. 자세한 기준은 [보안과 개인정보](SECURITY.md)에서 확인할 수 있습니다.

Apple이나 Samsung이 만들거나 승인한 프로젝트는 아닙니다. iPhone, AirPods, Galaxy와 One UI라는
이름은 비교한 경험과 시험 환경을 설명하기 위해 사용합니다.

## 라이선스와 공개 상태

현재 저장소는 아직 비공개 상태입니다. 직접 만든 코드는 `GPL-3.0-or-later`, 직접 쓴 문서는
`CC-BY-SA-4.0`으로 정했습니다. AirPods 렌더와 외부 의존성에는 각각의 원래 라이선스가 적용됩니다.
정확한 파일별 범위는 [라이선스 안내](LICENSES/README.md)에서 확인할 수 있습니다.

라이선스를 정한 것과 GitHub 저장소를 공개하는 것은 별개의 단계입니다. 공개 전환은 마지막 검사와
소유자의 명시적인 확인 뒤에 진행합니다. 처음 검토한 안과 선택이 바뀐 이유는
[라이선스 결정 기록](LICENSE-PROPOSAL.md)에 남겼습니다.

공개 순서와 첫 버전의 범위는 [공개 계획](docs/PUBLISHING.md), 다음 시험 항목은
[Roadmap](ROADMAP.md)에 기록합니다.
