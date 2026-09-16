# 비슷한 프로젝트

2026년 9월 7일 각 저장소의 README와 라이선스를 살펴본 목록입니다.
최신 기능·가격·설치 조건은 각 프로젝트에서 확인해 주세요. 전체 앱 시장을 조사한 목록은 아닙니다.

## AirPods

| 프로젝트 | 살펴본 기능 |
| --- | --- |
| [LibrePods](https://github.com/librepods-org/librepods) | 배터리, 소음 제어, 착용 감지와 통신 규약 |
| [CAPod](https://github.com/d4rken-org/capod) | 배터리, 팝업, 위젯과 자동 연결 |
| [OpenPods](https://github.com/adolfintel/OpenPods) | Bluetooth 저전력 광고를 통한 상태 확인 |
| [Podsify](https://github.com/Tanexc/Podsify) | 배터리, 위젯과 백그라운드 감시 |
| [GreenPods](https://github.com/andrewkomkov/GreenPods) | BLE·AAP 통신과 Android API 제약 |

이미 원하는 기능이 있다면 먼저 비교해 볼 만합니다.
에어팟 한눈에는 배터리 표시를 처음 만든 프로젝트가 아니라, 한 사용자의 Galaxy에서 필요한 화면과
조작을 직접 조절해 온 앱입니다. 현재는 Fold8과 사용 중인 AirPods 중심으로 시험했습니다.

외부 구현을 그대로 복사하지 않고 통신 설명과 관찰을 참고했습니다.
프로토콜 참고 자료와 구현 한계는 [개발 안내](../apps/airpods-glance/docs/DEVELOPMENT.md)에 있습니다.
다른 프로젝트의 코드를 가져오는 경우에는 해당 라이선스를 별도로 확인해야 합니다.

## 상태 표시줄을 눌러 위로 이동하기

[twelvehouse/TapToTop](https://github.com/twelvehouse/TapToTop)은 상태 표시줄의 터치 영역과 접근성 서비스를
사용합니다. 조사 당시 README는 정해진 횟수와 속도로 위쪽 스와이프를 반복하는 방식으로 설명했습니다.
당시 라이선스를 확인하지 못했으므로 코드를 재사용해도 된다고 가정하지 않았습니다.

맨 위로 톡은 반복 입력 때 재가속과 중간 개입 문제가 있어 한 번의 움직임을 선택했습니다.
아주 긴 피드의 처음까지 한 번에 도착하지 않을 수 있다는 한계가 있습니다.
[선택 이유와 실패한 방법](../apps/tap-to-top/docs/DEVELOPMENT.md)을 참고하세요.

## 데이터 이전과 사용 습관

Samsung Smart Switch는 사진·연락처·메시지 같은 데이터를 옮기고, Good Lock은 Galaxy의 화면과 조작을 바꿉니다.
이 저장소는 그 뒤에 남는 “아이폰에서 하던 이 행동은 어떻게 하지?”를
[문제별 해결책](../problems/README.md)과 [설정 방법](../recipes/README.md)으로 연결합니다.
개별 기능의 최초성보다 선택 이유와 실제 확인 범위를 기록합니다.
