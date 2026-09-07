# iPhone to Galaxy

> **English summary:** An open-source, problem-first record of translating a long-time iPhone
> user's familiar interactions to Samsung Galaxy with the smallest reversible solution—settings,
> Good Lock, automation, or a custom app only when necessary. Current device validation is limited
> to Galaxy Z Fold8 (`SM-F971N`), Android 17, One UI 9.0.

아이폰 장기 사용자의 습관을 Galaxy에서 안전하고 되돌릴 수 있는 방식으로 번역하는
오픈소스 사용성 실험 프로젝트입니다.

이 저장소의 중심은 앱의 개수가 아닙니다. 아이폰에서 익숙했던 행동이 Galaxy에서는 어떻게
다른지 관찰하고, 기본 설정 → Samsung 공식 도구 → 기존 앱 → ADB·자동화 → 직접 앱 순서로
가장 좁고 적절한 해결책을 찾는 과정입니다.

개별 기능의 최초성을 주장하지 않습니다. AirPods companion과 tap-to-scroll 앱은 이미 여러 공개
구현이 있습니다. 이 저장소의 차이는 기존 대안을 포함한 해결 수준을 실제 전환 문제에 연결하고,
실패와 설계 수정을 숨기지 않는 데 있습니다. 확인한 프로젝트와 겹치는 범위는
[기존 프로젝트 조사](docs/LANDSCAPE.md)에 기록합니다.

## 익숙함의 기준

- iPhone 사용 시작: iPhone 3GS
- 마지막 사용 기종: iPhone 14 Pro
- 마지막 사용 OS: iOS 26.6.1

여기서 말하는 ‘아이폰에서 익숙했던 사용성’은 모든 iPhone과 iOS를 대표하지 않습니다.
iPhone 14 Pro와 iOS 26.6.1까지 이어진 개인적인 사용 경험이 비교의 출발점입니다.

## 현재 검증 범위

- 기기: Samsung Galaxy Z Fold8
- 모델: `SM-F971N`
- Android: 17
- One UI: 9.0
- 지역 및 언어: 대한민국 / 한국어

현재 해결책은 위 환경에서만 직접 검증했습니다. 다른 Galaxy 기기, 화면 비율, fold 상태,
Android 및 One UI 버전에서는 레이아웃과 시스템 동작이 다를 수 있습니다. 각 해결책에는 실제로
확인한 화면 상태와 미검증 범위를 별도로 기록합니다.

## 해결 원칙

1. Galaxy 기본 설정으로 해결합니다.
2. Good Lock과 Samsung 공식 앱으로 해결합니다.
3. 신뢰할 수 있는 기존 앱이 있으면 검토합니다.
4. 필요한 경우에만 ADB와 자동화를 사용합니다.
5. 앞의 방법으로 해결되지 않을 때만 직접 앱을 만듭니다.

모든 해결책은 필요한 권한, 데이터 처리, 알려진 한계, 원상복구 방법과 실제 검증 환경을 함께
공개합니다.

## 문제 지도

| iPhone에서 익숙했던 것 | Galaxy에서 찾은 해법 | 수준 | 상태 |
|---|---|---:|---|
| 상태 표시줄을 탭해 위로 이동 | [Tap to Top](problems/tap-status-bar-to-scroll-top.md) | 직접 앱 | 단일 기기 검증 |
| 주말·공휴일에는 더 늦게 끝나는 수면 | [공휴일 수면 연장](problems/holiday-aware-sleep.md) | 직접 앱 + 모드 | 단일 기기 검증 |
| AirPods 배터리와 큰 연결 카드 | [AirPods on Galaxy](problems/airpods-on-galaxy.md) | 직접 앱 | 실험적·단일 기기 검증 |
| 회의 시작을 한 번에 전환 | [회의 모드](problems/meeting-mode.md) | Samsung 모드·루틴 | 단일 기기 구성 |
| 오른손 중심의 가장자리 제스처 | [한손 제스처](problems/one-hand-gestures.md) | Good Lock | 단일 기기 구성 |
| 집·외출·저전력 자동 전환 | [생활 자동화](problems/contextual-routines.md) | Samsung 루틴 | 단일 기기 구성 |

완성도는 해결 수준과 별도로 기록합니다. `아이디어 → 재현 → 구현 → 단일 기기 검증 → 다기기
검증` 순이며, 숨은 API나 기종 의존성이 있으면 `실험적`을 함께 표시합니다.

## 현재 사례

- 상태 표시줄을 탭해 한 번의 자연스러운 동작으로 위로 이동하는 `Tap to Top`
- 한국 법정공휴일을 판별해 수면 시간을 연장하는 `Holiday Sleep`
- 광고와 네트워크 없이 AirPods 배터리·위젯·연결 카드를 제공하는 `AirPods Glance`
- 회의, 수면, 귀가, 외출, 배터리와 한손 제스처를 위한 Galaxy 설정·루틴

## 빠른 시작

설정만으로 해결되는 문제는 [`recipes/`](recipes/)에서 시작합니다. 직접 만든 앱의 소스를
검토하거나 빌드하려면 [`apps/`](apps/)를 봅니다. Android SDK Platform 36과 Build Tools
36.0.0, JDK가 필요하며 각 앱의 `./build.sh`가 공개용 debug APK를 만듭니다. 이것은 배포용
서명이 아닙니다.

## 공개 준비 상태

이 디렉터리는 공개 저장소의 정리본을 만들기 위한 staging 영역입니다. 기존 작업 폴더를 통째로
복사하지 않습니다. 개인정보, 서명 키, 기기 로그, 화면 녹화와 라이선스가 불명확한 참고 자료를
제거하고 재현 가능한 소스만 앱별로 옮긴 뒤 Git 저장소를 초기화합니다.

## 데이터와 상표

앱에는 광고·분석 SDK가 없으며 인터넷을 사용하는 앱 권한도 두지 않습니다. 빌드 중 AirPods
Glance가 공개 Maven 저장소에서 고정 checksum의 의존성 하나를 내려받는 것은 앱 실행 중의
네트워크 통신과 다릅니다. 각 앱의 실제 권한과 로컬 데이터는 앱 README에 적습니다.

이 프로젝트는 Apple 또는 Samsung과 제휴하거나 승인받은 공식 프로젝트가 아닙니다. iPhone,
AirPods, Galaxy와 One UI 등의 명칭은 비교 대상과 호환성 범위를 설명하기 위해서만 사용합니다.

## 라이선스

공개 전 최종 확인할 제안은 코드 Apache-2.0, 문서 CC BY 4.0입니다. 제3자 자산은 각 원저작자의
라이선스를 유지합니다. 현재 staging에는 아직 프로젝트 전체 라이선스 승인을 적용하지 않았습니다.
자세한 범위는 [라이선스 제안](LICENSE-PROPOSAL.md)을 봅니다.

공개 순서, 첫 release 범위와 community 운영은 [GitHub 공개 전략](docs/PUBLISHING.md), 다음 검증은
[Roadmap](ROADMAP.md)에 있습니다.
