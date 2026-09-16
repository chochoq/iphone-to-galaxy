# 에어팟 한눈에 개발 안내

[사용법](../README.md) · [확인한 환경과 남은 시험](../../../compatibility/README.md)

배터리 숫자를 읽는 것과 소음을 바꾸는 명령은 구분합니다. 알 수 없는 값은 알 수 없는 채로
남기고, 제어는 사용자가 요청한 순간에만 실행합니다. 화면보다 이 규칙을 먼저 검사합니다.

## 상태와 연결

소스는 [`src/com/chocho/airpodsglance/`](../src/com/chocho/airpodsglance/)에 있습니다.

- 패킷 해석: `AirPodsPacketDecoder`, `AapBatteryDecoder`, `ListeningProtocol`. 길이·범위·종류를 검증한 값만 채택합니다.
- 잔량 저장·표시: `BatteryStateStore`, `AirPodsSnapshot`, `BatteryPresentation`. 한 스냅샷으로 저장하고 수신 시각·연결 상태를 함께 표시합니다.
- 연결 공유: `AapHub`, `AapConnectionOwner`, `AapClient`. 배터리와 소음 제어가 연결을 공유하며 최대 30초로 제한합니다.
- 제어 상태: `ListeningController`, `ListeningState`. 명령 전송을 성공으로 보지 않고 기기 응답으로 확인합니다.
- 연결 카드: `ConnectionPopupPolicy`, `ConnectionPopupOverlay`. 화면을 깨우거나 잠금을 우회하지 않습니다.
- 제품 표현: `ProductCompositionPolicy`, `ProductMotionPolicy`. 검증하지 않은 수납·착용 상태를 그림으로 단정하지 않습니다.

BLE 잔량은 10% 단위, AAP 잔량은 1% 단위입니다. 새 AAP 값을 거친 BLE 값으로 덮어쓰지 않습니다.
누락된 부품이나 분리 상태를 0%로 바꾸지 않으며, 연결이 끊긴 뒤 마지막 값은 현재 측정값과 구분합니다.
선택 기기의 Bluetooth 주소는 앱 내부 설정에만 저장합니다. 원시 패킷과 주소를 진단 로그에 넣지 않습니다.

설치는 Android 12 이상에서 가능하지만 AAP 경로는 Android 17 이상에서만 켭니다.
`AapClient`는 공개 소켓 API를 확인한 뒤 필요한 경우 비공개 API를 사용합니다.
숨은 API 접근에는 HiddenApiBypass를 쓰므로, OS 업데이트 뒤에도 작동한다고 보장할 수 없습니다.
연결 감시를 켰을 때만 자동 배터리 읽기를 수행하며, 실패 뒤 빠르게 재연결하는 반복 작업은 하지 않습니다.

## 소음 제어와 위젯

`ListeningProtocol`이 노캔·적응형·주변음의 허용 값과 프레임을 정의합니다.
현재 응답이 없으면 마지막 상태를 현재 상태처럼 표시하지 않습니다. 연결이 끊기거나 작업이 끝나면
이전 변경 요청을 버립니다. 다시 연결됐다는 이유로 자동 재전송하지 않습니다.

`ListeningWidgetService`는 사용자가 위젯을 누른 경우만 처리합니다. 위젯 ID와 요청을 검증하고,
최대 15초 동안 현재 상태 확인 → 필요한 명령 한 번 → 응답 확인 순으로 실행합니다.
연속 탭을 대기열에 쌓거나 재부팅 뒤 복원하지 않습니다. 배터리 위젯의 부품 전환은 이 경로를 쓰지 않습니다.

`ListeningOrderDraft`와 `ListeningOrderEditor`는 사용할 모드와 순서를 편집합니다.
선택한 두세 모드를 앞에, 제외한 모드를 뒤에 둡니다. 재선택한 모드는 사용 순서 끝에 붙입니다.
드래그는 손을 뗄 때 반영하고, 취소·다중 터치로 중단한 조작은 저장하지 않습니다.
저장 전 원본과 초안을 분리하며, 편집 도중 다른 경로에서 설정이 바뀌면 충돌을 확인합니다.
화면 읽기 이동 동작과 키보드 정렬도 제공합니다. 자동 검사는 실제 TalkBack 음성 탐색을 대신하지 않습니다.

## 배터리 위젯과 연결 카드

홈 3×1·2×1은 세 부품을 표시하고, 1×1은 부품을 전환합니다. 크기는 칸 수만 믿지 않고 실제 가용 영역과
글자 크기로 계산합니다. 한 칸에 내용이 들어가지 않으면 앱을 여는 방식으로 바뀝니다.
잠금 위젯은 홈과 별도 선택 상태를 저장하며, 탭은 부품만 바꿉니다. 탭할 때 Bluetooth를 새로 연결하지 않습니다.

연결 카드는 화면이 켜져 있고 잠금이 풀렸으며 표시 권한과 잔량이 있을 때 무음으로 띄웁니다.
방해 금지 자체를 차단 조건으로 쓰지 않습니다. 초기에는 방해 금지 중 카드도 숨기고 알림으로
대체했지만, 대체 알림까지 차단되어 아무것도 보이지 않았습니다. 화면·잠금 조건과 소리·진동 여부를
따로 검사하도록 고쳤습니다. 당시 방해 금지 상태의 카드 표시를 확인했으나 자동 수신의 일시적
시간 초과도 관찰했습니다. 현재 버전의 새 물리 연결 시험과는 구분해야 합니다.

충전 중이라는 정보만으로 케이스에 들어 있다고 판단하지 않습니다. 완충과 케이스 방전이 반례입니다.
`AapPlacementDecoder`의 primary/secondary도 검증 없이 왼쪽/오른쪽으로 고정하지 않습니다.
현재 제품 영상은 [출처가 표시된 3D 모델](../THIRD_PARTY_NOTICES.md)의 렌더입니다.
프레임을 변형해 이어 붙인 초기 영상은 모양이 일그러져 사용하지 않습니다.

## 통신 변경 시 주의할 실패 사례

초기 AAP 실험에서는 배터리 요청 시 음악 연결이 끊겼습니다. 소켓 연결 성공만으로 안전하다고
판단할 수 없었습니다. handshake 응답 이후 활성화 메시지를 보내도록 바꾸고, 근거가 부족한
추가 배터리 요청을 빼고, 연결 시간을 제한했습니다. 이후 세 번의 독립 연결에서 배터리 수신과
30초 동안의 음악 연결 유지를 확인했습니다. 다른 펌웨어까지 안전하다는 뜻은 아닙니다.

통신을 바꿀 때는 패킷 모델 검사 → 시간 초과·중복 소유자 검사 → 별도 기기 시험 순으로 진행합니다.
실기기에서는 연결·수신 여부뿐 아니라 실제 음악의 끊김, 종료 후 연결 유지와 요청 재전송 여부를 확인합니다.
기기 주소·원시 패킷·계정 정보 대신 실패 단계와 필요한 최소 관찰만 남깁니다.

## 검사하기

저장소 루트에서 실행합니다. 빌드 도구 준비는 [시작 안내](../../../docs/GETTING-STARTED.md)를 따릅니다.

```sh
sh shared/android-ui/test-core.sh
sh apps/airpods-glance/test-core.sh
sh apps/airpods-glance/test-listening.sh
sh apps/airpods-glance/test-listening-widget.sh
sh apps/airpods-glance/build.sh
sh apps/airpods-glance/verify-apk.sh
```

모델 검사는 패킷·상태·수명·편집 규칙을 검사하며 실제 Bluetooth나 런처를 시험하지 않습니다.
Android 화면 검사는 [소음 제어 UI](../tests/listening-ui/README.md),
[위젯·편집기 검사](../tests/listening-widget-render/README.md),
[공통 위젯 검사](../../../shared/android-ui/README.md)에 있습니다. 테스트 APK는 별도 시험 기기에서 사용합니다.

앱 안 세 모드 전환과 배터리·잠금 위젯의 확인 결과를, 새 소음 제어 위젯의 실제 전환 결과로 대신하지 않습니다.
미확인 항목은 [호환성 안내](../../../compatibility/README.md)에서 관리합니다.

## 참고 자료

- [LibrePods AAP 설명](https://github.com/kavishdevar/aln/blob/53679cc90222e94ade84e66542d97ace2540e626/docs/AAP%20Definitions.md): 프로토콜 해석의 참고 자료이며 Apple 공식 명세가 아닙니다.
- [비슷한 프로젝트](../../../docs/LANDSCAPE.md): 배터리·제어 방식을 교차 확인한 선행 프로젝트.
- [제3자 고지](../THIRD_PARTY_NOTICES.md): 모델 렌더와 HiddenApiBypass의 출처·조건.

통신 설명을 참고해 작성한 독립 구현입니다. 외부 자료의 추정을 현재 모델의 실측 사실로 취급하지 않습니다.
