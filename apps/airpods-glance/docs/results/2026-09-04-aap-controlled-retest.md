# AAP protocol 순서 통제 재시험 — 독립 연결 3회 성공

## 목적과 조건

첫 hidden L2CAP 시험은 socket 연결 약 5초 뒤 ACL/A2DP/HFP를 끊었다. transport 자체와
잘못된 초기 message 순서를 분리하기 위해 MaterialPods를 제거하고 우리 앱만 설치된
SM-F971N(Android 17)과 `테스트용 AirPods Pro`에서 다시 시험했다.

자동 ACL receiver는 BLE만 시작한다. 앱 화면의 `직접 배터리 1회 테스트`만 experiment extra를
붙이고, AAP client는 30초 뒤 자동 종료된다. 재연결 broadcast에는 이 extra가 없으므로 실패해도
두 번째 hidden socket이 자동으로 열리지 않는다. 이전 안전 APK SHA-256은
`120b11f4aad9b54e35f7b257536fcedbf5aea68e607b8d97261c342acf710c39`로 별도 보관했다.

## protocol 변경

1. type 3, PSM `0x1001` socket을 하나 연다.
2. handshake 하나만 write+flush한다.
3. connect response status 0을 확인한다.
4. notification enable 두 packet과 InitExt를 각각 write+flush한다.
5. 추정으로 추가했던 battery request `04 00 04 00 03 00`은 보내지 않는다.
6. push battery만 해석하며 raw packet과 Bluetooth 주소는 저장·표시하지 않는다.

## 실측

- 시험 시작: 2026-09-04 18:18:40 KST
- system type 3 socket connected: 18:18:41.265
- v2 전용 단계: experiment start 1, handshake send 1, post-handshake send 1,
  experiment window close 1
- 수신 화면: 왼쪽 98%, 오른쪽 92%, 케이스 unknown
- source 설명: `연결된 AirPods에서 직접 확인한 1% 단위 값이에요.`
- 앱과 홈 위젯 snapshot 일치: `왼쪽 98% · 오른쪽 92% · 케이스 —`
- ACL/A2DP 표본: t=1,2,3,4,5,6,8,10,12,15,20,25,30초 모두 connected
- Bluetooth event history: 시험 구간에 disconnect/disconnection 없음
- 30초 종료 뒤 ACL, A2DP, active device 모두 계속 AirPods
- 코어 검사: 47 assertions 통과
- APK: v3 signature와 금지 권한 정책 검사 통과

## 두 번째 독립 연결·음악 재생 시험

- 이전 연결 종료: 18:23:47, remote user terminated
- 새 ACL 성공: 18:27:30, A2DP connected: 18:27:31
- 시험 시작: 18:28:38
- 시험 전 media playback: `started`, output `bt_a2dp`, device AirPods
- t=1,2,3,4,5,6,8,10,12,15,20,25,30초: connection `CONNECTED`, audio `PLAYING`
- 새 수신값: 왼쪽 95%, 오른쪽 92%, 케이스 unknown
- v2 누적: experiment 2, handshake send 2, activation 2, window close 2
- 시험 종료 뒤 playback과 AirPods A2DP가 계속 유지됨

두 번째 시험은 같은 ACL에서 버튼만 반복한 것이 아니라 실제 disconnect 뒤 만들어진 별도
연결이므로 3회 기준의 두 번째 표본으로 센다.

## 세 번째 독립 연결·한쪽 case 시험

- 이전 연결 종료: 18:31:24, remote user terminated
- 새 ACL 성공: 18:32:02, A2DP connected: 18:32:03
- 물리 조건: 왼쪽 착용, 오른쪽은 열린 case 안, 왼쪽으로 음악 재생
- 시험 시작: 18:32:45
- t=1,2,3,4,5,6,8,10,12,15,20,25,30초: connection `CONNECTED`, audio `PLAYING`
- 수신값: 왼쪽 92%, 오른쪽 92% charging, case 84%
- 앱·위젯 일치: `왼쪽 92% · 오른쪽 92%⚡ · 케이스 84%`
- v2 누적: experiment 3, handshake send 3, activation 3, window close 3
- 시험 종료 뒤 playback과 AirPods A2DP가 계속 유지되고 새 disconnect가 없음

오른쪽을 case에 둔 물리 조작과 wire type `0x02`의 charging 표시가 일치했다. 따라서 현재
decoder의 `0x04=left`, `0x02=right`, `0x08=case` mapping 가설을 실기기에서 지지한다.

## 피드백 분석

첫 실패와 결과가 달라진 것은 hidden transport가 아니라 wire 순서다. 따라서 “hidden socket은
항상 오디오를 끊는다”는 해석은 약해졌고, handshake 응답 전에 session message와 근거 없는
battery request를 한꺼번에 보낸 것이 원인이었을 가능성이 커졌다. 그러나 여러 변경을 동시에
제거했으므로 특정 packet 하나를 원인으로 확정하지 않는다.

세 독립 연결, 그중 두 번의 실제 음악 재생, 한쪽 case 좌우·충전·case 시험이 모두 성공했다.
자동 경로 승격 기준은 충족됐지만 설치된 검증 APK는 여전히 사용자가 화면에서 누른 한 번만
실행한다. 다음 구현은 새 ACL당 한 번, 최대 30초, 같은 session 무재시도라는 검증 경계를
유지한 채 자동화해야 한다.

## 자동판 구현·설치

검증 뒤 session-persistent automatic claim을 추가했다. 선택된 AirPods의 verified ACL start만
claim할 수 있고 새 session에서 한 번 성공하면 중복 broadcast나 service recreation은 자동
socket을 다시 열 수 없다. 수동 앱 버튼은 `배터리 지금 확인`으로 남기되 별도 manual extra를
사용한다. 두 경로 모두 30초 close를 공유한다.

47개 core assertion과 APK signature/policy 검사를 통과한 SHA-256
`7a1090615fc19823460037a2901e763c8c8e4579c9e99f262d259620e013e04c` 빌드를 설치했다. 설치 중
AirPods A2DP와 media playback은 유지됐다. 이미 열린 ACL에는 연결 broadcast가 다시 오지
않으므로 무버튼 자동 시작의 최종 실측은 다음 실제 reconnect에서 수행한다.

## 자동 시작 최종 실측

사용자가 앱 버튼을 누르지 않고 Bluetooth 설정에서 AirPods를 연결했다. system history의 새 ACL
성공은 18:43:15.280, 우리 type 3 socket 연결은 18:43:15.706이었다. 진단 누적에서 새 자동판
전용 값은 automatic 1, manual 0, direct window close 1이었다. 따라서 UI 동작 없이 receiver
경로가 한 번 실행되고 30초에 닫힌 것을 확인했다.

battery message 3건 뒤 widget은 왼쪽 90%, 오른쪽 98%, case unknown을 `방금 확인`으로
표시했다. 음악은 AirPods A2DP에서 계속 `started`였고 시험 뒤에도 ACL/A2DP disconnect가
없었다. 이 결과로 9.3.2의 bounded 자동 battery 경로를 완료로 판정한다.

## Mac host 경쟁 분리

iPhone과 iPad Bluetooth는 꺼져 있었고 Mac Bluetooth만 켜진 조건에서 AirPods가 Galaxy에
먼저 붙지 않았다. Mac Bluetooth를 끄자 18:51:36과 18:52:04에 AirPods가 Galaxy로 connection
request를 보내 두 ACL을 모두 remote-initiated로 열었다. 이는 앞선 18:43의 System UI local
connect와 명확히 다른 경로다.

우리/설정 동작을 혼동하지 않기 위해 automatic counter도 확인했다. 두 새 ACL 뒤 누적 값은
automatic 3, manual 0이었고 최신 battery는 왼쪽 88%, 오른쪽 96%, case unknown이었다. 정확한
Apple 내부 switching 판단은 직접 관측할 수 없지만, Mac Bluetooth만 바꾼 반복 결과는 Mac이
선행 연결 경쟁자였다는 해석을 강하게 지지한다.
