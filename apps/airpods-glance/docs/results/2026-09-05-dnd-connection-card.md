# 수면 DND 연결 카드 무표시 진단과 수정 — 2026-09-05

## 1. 사용자 장면과 물리 상태

자정 무렵 수면 모드가 자동으로 켜진 Galaxy에서 사용자가 AirPods를 착용·연결했지만, 직전에 완성한
큰 3D 연결 카드가 나타나지 않았다. AirPods는 연결된 상태였고 앱은 최신 상태를 읽었다.

## 2. 수정 전 예측

연결 감지 자체가 실패했다면 AAP socket과 연결 notification 모두 없어야 한다. overlay 권한이나
화면 상태가 문제라면 해당 값이 false여야 한다. 반대로 DND 정책만 원인이라면 service와 fallback
notification은 생기지만 type-2038 window는 없고 notification은 DND에 intercepted될 것이다.

## 3. 실제 진단값

- 00:01:14와 00:01:51에 앱 PID가 Bluetooth type-3 socket을 열어 연결 경로가 실행됐다.
- 앱 UI는 AirPods를 `연결됨 · 방금 확인`으로 표시했다.
- `zen_mode=1`, `mWakefulness=Awake`, `SYSTEM_ALERT_WINDOW: allow`였다.
- 앱 소유 type-2038 overlay window는 0개였다.
- connection notification ID 2102가 두 번 생성됐지만 둘 다 `intercepted ... new:!priority`였다.
- 순수 정책은 automatic + DND일 때만 false를 반환하고 explicit preview만 우회하도록 구현돼 있었다.

## 4. 피드백 분석과 설계 수정

배터리, Bluetooth, 3D renderer 실패가 아니라 DND를 두 번 적용한 표시 정책 실패다. application
overlay는 본래 무음인데 DND로 억제했고, 그 fallback notification도 시스템 DND가 억제했다. 따라서
화면이 켜지고 잠금이 풀린 경우 DND를 표시 금지 조건에서 제거했다. 화면 꺼짐, 잠금, overlay 권한,
reading 조건은 그대로다. 수면 모드나 통화·알람 설정 자체에는 손대지 않았다.

## 5. 자동 검사와 설치 결과

- DND active + active/unlocked 자동 카드는 true로 정정했다.
- DND active + locked는 false로 새 fixture를 추가했다.
- JVM core 65 assertions 통과.
- APK v3 signature와 Internet·위치·microphone·accessibility 부재 policy 통과.
- SM-F971N에 00:06:50 덮어 설치 성공.
- 설치 후에도 `zen_mode=1`, overlay app-op `allow`가 유지됐다.

## 6. 새 연결과 DND 시각 경로의 실제 결과

사용자가 AirPods를 case에 넣어 00:13:04 기존 ACL을 완전히 끝낸 뒤 다시 착용했다. 00:16:19 새
ACL broadcast가 들어왔고 service와 type-3 socket이 자동 시작됐다. 다만 이 첫 socket은 연결과
handshake 전송 뒤 응답 없이 10초 timeout이 나 battery snapshot을 만들지 못했다. 값이 없으므로
이 시점에 카드가 없었던 것은 표시 policy 실패가 아니며 notification fallback도 생성되지 않았다.

같은 연결을 유지한 채 앱의 `배터리 지금 확인`을 한 번 실행하자 handshake 응답과 activation,
battery 3개 packet이 들어왔다. 00:18:13 DND `zen_mode=1`인 상태에서 다음을 확인했다.

- package 소유 `ty=APPLICATION_OVERLAY`, token type 2038 창 정확히 1개
- window frame `[31,1157]-[1216,1907]`, on-screen/visible true
- 카드에 왼쪽 98%, 오른쪽 100%, case unknown을 실제 렌더
- 진단 `popup_overlay_shown` 8→9, `popup_notification_fallback` 2→2
- Android가 자동으로 알리는 overlay 상태 notification 외 앱 connection notification 2102 증가 없음
- screenshot에서 DND 아이콘과 큰 3D 카드가 동시에 보임

처음 사용한 window 검색식은 package와 `type=2038`이 한 줄에 함께 있을 것이라 가정해 실제 열린
창을 0으로 잘못 셌다. WindowManager block에서 package 줄과 `ty=APPLICATION_OVERLAY`/token 줄이
나뉜 것을 직접 읽고 진단 counter와 screenshot으로 교차 확인해 이 측정 오류를 정정했다.

## 7. 판정과 남은 경계

DND 중 active-unlocked 무음 카드 허용이라는 수정은 실기기에서 통과했다. 자동 연결 첫 AAP가 이번
한 번 timeout 난 탓에 `새 ACL → 자동 battery → 카드`의 전 구간은 한 번에 통과하지 않았다. 곧바로
수동 재시도는 성공했고 누적 automatic 15회 중 handshake timeout은 1회다. 오디오 안전을 위해 같은
ACL의 자동 재시도를 섣불리 추가하지 않고, 다음 자연 재연결에서 반복되는지 관찰한다. DND 원인과
일시적 AAP 응답 실패를 하나로 요약하지 않는다. 카드 본문/X 닫기 1→0은 이전 renderer 회귀에서
통과했으며 이번에는 사용자가 현재 화면을 직접 볼 수 있도록 열린 채 두었다.
