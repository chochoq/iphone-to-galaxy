# 기존 GitHub 프로젝트와 겹치는 범위

- 조사일: 2026-09-07
- 방법: GitHub repository search와 각 저장소의 현재 README·license 확인
- 한계: 이름과 공개 설명으로 찾는 검색이므로 전체 GitHub나 비공개·스토어 앱을 망라하지 않는다.

## 결론

개별 기능은 새롭지 않다. AirPods on Android는 이미 성숙하고 큰 프로젝트가 여러 개이며, 상태
표시줄을 탭해 위로 스크롤하는 Android 접근성 앱도 존재한다. 현재 검색에서는 한국 공휴일을 Samsung
수면 DND 종료와 연결하는 앱이나, iPhone 장기 사용자의 Galaxy 전환 문제를 해결 수준별로 묶은
저장소는 뚜렷한 대응물을 찾지 못했다. `없다`는 증명이 아니라 이번 검색에서의 관찰이다.

따라서 이 저장소의 주장은 “처음 만든 기능”이 아니다. 기존 해법을 먼저 보여주고, 한 사용자의 실제
문제에서 왜 설정·공식 도구·기존 앱·자동화·직접 앱 중 하나를 골랐는지, 어떤 반례로 설계를 바꿨는지
기록하는 것이 중심이다.

## AirPods on Android

| 프로젝트 | 공개 범위 | 2026-09-07 관찰 | 우리와의 관계 |
|---|---|---|---|
| [LibrePods](https://github.com/librepods-org/librepods) | Android/Linux, battery·ANC·ear detection·설정 등 광범위 | 약 29.7k stars, GPL-3.0 | 기능 폭과 protocol 연구가 훨씬 큼. 범용 대안으로 먼저 소개 |
| [CAPod](https://github.com/d4rken-org/capod) | battery·charging·popup·widget·auto connect 등 | 약 1.1k stars, GPL-3.0, ad-free이며 일부 기능은 구매 | 일반 사용자의 강한 기존 대안 |
| [OpenPods](https://github.com/adolfintel/OpenPods) | AirPods monitoring | 약 1.2k stars, GPL-3.0 | 공개 BLE monitoring의 선행 프로젝트 |
| [Podsify](https://github.com/Tanexc/Podsify) | battery·widget·background monitoring | Apache-2.0 | 작고 현대적인 UI의 기존 대안 |
| [GreenPods](https://github.com/andrewkomkov/GreenPods) | BLE/AAP, control과 진단 | Android 17 hidden API 경계를 문서화 | 비슷한 최신 Android AAP 연구가 이미 존재 |

AirPods Glance의 battery·widget·popup 자체는 독창성 주장이 될 수 없다. 남길 이유는 사용자가 광고와
반복 안내 없이 자기 기기에서 필요한 최소 표면을 직접 통제하려 했던 과정, DND와 지속 카드, 잘못된
이미지 interpolation, 실제 iOS 상태 재확인으로 제품 구성을 여러 번 고친 history에 있다.

일반 사용자에게는 위 기존 앱을 먼저 비교하도록 안내하고, AirPods Glance는 `실험적·단일 기기
검증` 사례로 둔다. 특히 LibrePods/CAPod/OpenPods의 GPL 코드를 복사하지 않았으며, protocol 동작을
교차 확인한 출처는 앱 문서에 표시한다.

## 상태 표시줄 탭으로 위로 스크롤

[twelvehouse/TapToTop](https://github.com/twelvehouse/TapToTop)은 2026-01-20 생성된 Android
접근성 앱으로, 상단 invisible overlay와 반복 upward swipe, 반복 횟수·속도 설정을 제공한다고 현재
README에 설명한다. GitHub license API에서는 라이선스를 찾지 못했으므로 소스를 재사용할 권리가
있다고 가정하지 않는다.

목적과 overlay/accessibility 구조는 우리 Tap to Top과 겹친다. 핵심 차이는 우리가 실제 피드백에서
반복 swipe의 재가속과 사용자 입력 탈취를 실패로 판정해 제거했다는 점이다. 현재 구현은 절대 top을
덜 보장하더라도 한 번의 interruptible gesture를 우선하며 X의 긴 feed 한계를 숨기지 않는다. 이
차이는 최초성보다 설계 trade-off다.

## iPhone에서 Galaxy로 전환

`iPhone to Galaxy`, `iPhone to Android migration UX` 검색은 연락처 변환기 등 데이터 이동 도구
위주였고, 설정→Good Lock→기존 앱→ADB→직접 앱을 문제별로 연결하는 현재와 같은 공개 여정은 이번
검색에서 찾지 못했다. Samsung Smart Switch와 Good Lock이 각각 데이터 이전과 일반 customization을
담당하므로, 우리의 입구는 그 사이의 “이 iPhone 행동을 Galaxy에서는 어떻게 번역하지?”다.

## 이 조사로 바뀐 공개 전략

1. README에서 앱 개수나 최초성을 성과로 내세우지 않는다.
2. 각 문제 문서에 성숙한 기존 대안을 먼저 연결한다.
3. 더 나은 기존 앱이 있는 사용자에게 직접 앱 설치를 권하지 않는다.
4. 우리의 구현은 단일 기기 실험과 설계 history로 표시한다.
5. 새 기능을 만들기 전 GitHub·F-Droid·Play Store의 기존 해법 확인을 공개 gate에 추가한다.
