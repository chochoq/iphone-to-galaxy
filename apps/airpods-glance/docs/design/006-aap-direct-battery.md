# 006 — 실측 반증 뒤 AAP 직접 배터리 확장

## 1. 확장 이유와 느낌

첫 설계는 공개 BLE proximity frame만으로 충분할 것이라 예측했다. 연결 자동 시작은
정확히 작동했지만 사용자의 실제 AirPods에서는 케이스를 열고 양쪽을 넣은 통제 장면에서도
`0x07`이 한 번도 나오지 않았다. Apple frame 전체는 풍부하게 들어왔으므로 스캐너가 죽은
것도 아니었다. 여기서 주변의 가장 강한 frame을 억지로 battery로 해석했다면 숫자는 빨리
나왔겠지만 사용자의 기기라는 보장이 사라졌을 것이다. 값을 비워 둔 현재 실패가 오히려
제품의 신뢰 경계를 지켰다고 느낀다.

현재 SM-F971N은 Android 17, API 37이며 `BluetoothSocketSettings` 자체는 존재한다. 그러나
구현 뒤 공식 Android 소스와 실기기를 대조하니 public builder는 RFCOMM과 LE만 허용했고
BR/EDR L2CAP type 3을 거부했다. LE PSM도 `0x80..0xFF`만 가능해 AAP의 `0x1001`을 넣을 수
없다. 따라서 아래 AAP 구조는 decoder·상태 계약으로는 유효하지만 public socket을 제품
경로로 쓸 수 있다는 초기 전제는 폐기됐다.

## 2. 사용자 시나리오

### S-AAP-1 — 평소 연결

1. 사용자가 AirPods를 귀에 꽂아 갤럭시에 연결한다.
2. 기존 ACL receiver가 선택 bonded device임을 확인한다.
3. service가 foreground가 된 뒤 AAP socket과 BLE fallback을 병렬 시작한다.
4. AAP handshake와 notification enable이 성공하면 좌·우·케이스 battery를 1% 단위로
   snapshot 하나에 저장한다.
5. 앱, widget, ongoing notification, session popup이 같은 snapshot을 표시한다.
6. AirPods가 끊기면 socket read를 즉시 중단하고 service와 notification을 없앤다.

베스트 프랙티스 검증:

- Bluetooth 주소 선택은 bonded resolver 한 곳에서만 하고 raw 주소를 UI/log에 쓰지 않는다.
- socket 연결은 main thread 밖에서 하며 connect 8초, handshake 10초 watchdog을 따로 둬
  무한 대기를 막고 실패 지점을 구분한다.
- AAP callback도 BLE와 동일한 `BatteryStateStore`를 거쳐 표면 간 숫자 불일치를 막는다.
- 연결 실패가 오디오 A2DP를 끊거나 재페어링을 요구하지 않아야 한다.

### S-AAP-2 — AAP가 없는 모델·일시 실패

1. 공개 API class 또는 socket 생성이 불가능하거나 PSM 연결이 거부된다.
2. 진단에는 availability, socket, handshake 단계 중 어디서 실패했는지만 누적한다.
3. BLE scan은 그대로 유지한다.
4. 사용자는 허위 0% 대신 `배터리 확인 중`을 본다.
5. 같은 세션에서 무한 재연결하지 않고 다음 ACL 연결 또는 명시적 새로고침에서 다시 시도한다.

베스트 프랙티스 검증:

- API 36 이하에서 hidden API reflection/bypass를 시도하지 않는다.
- exception class나 device address를 사용자 로그로 남기지 않는다.
- 실패 뒤 socket, executor task, foreground service가 고아로 남지 않는지 확인한다.

### S-AAP-3 — 배터리 entry가 일부만 있음

1. AAP battery message가 왼쪽/오른쪽/케이스 중 일부만 보낸다.
2. 해당 message에 없는 component를 무조건 0으로 만들지 않는다.
3. 한 세션의 직전 fresh component를 합칠지는 물리 시험으로 검증한다. 1차 구현은 message가
   선언한 component만 새 값으로 두고, disconnected 상태값은 unknown으로 처리한다.
4. charging state 1과 5는 충전, 2는 비충전, 4는 분리로 읽는다.

베스트 프랙티스 검증:

- percent `0..100`만 허용하고 127/255는 unknown으로 거부한다.
- charging state 4(disconnected)의 0%는 실제 방전 0%로 표시하지 않는다.
- physical left/right type은 wire value `0x04/0x02`로 고정하고 한쪽 case 시험으로 확인한다.

## 3. 데이터 계약 변경

```text
AirPodsSnapshot.source
  NONE
  BLE_PUBLIC_DECILE     // 0,10,...100
  AAP_EXACT_PERCENT     // 0...100
```

`BatteryComponent`는 이제 0~100 정수 전체를 허용한다. 10% 배수 제한은 constructor가 아니라
BLE decoder가 보장한다. 이 위치 이동이 중요하다. source가 늘었는데 공통 모델이 옛 source의
정밀도를 강제하면 정확한 AAP 값을 손실하기 때문이다.

앱의 정밀도 설명도 snapshot source에 따라 바꾼다.

- BLE: `공개 Bluetooth 정보라 10% 단위로 표시돼요.`
- AAP: `연결된 AirPods에서 직접 확인한 1% 단위 값이에요.`
- NONE: `연결 후 실제 값을 확인해요.`

## 4. 최소 AAP wire 계약

다음은 Wireshark AACP dissector 계보와 실제 AirPods 캡처가 교차하는 protocol 사실로 취급한다.
GPL 애플리케이션 구현 코드는 복사하지 않고, 이 byte contract에 대한 독립 Java decoder와
fixture를 작성한다.

- BR/EDR L2CAP PSM: `0x1001`
- connect request: packet type `0x0000`, service `0x0004`
- connect response: packet type `0x0001`, status 0이면 성공
- message packet: packet type `0x0004`, service `0x0004`, command little-endian at byte 4~5
- battery command: `0x0004`
- battery payload byte 0: entry count
- entry 크기: 5 bytes
- entry type: single `0x01`, right `0x02`, left `0x04`, case `0x08`
- entry percent: offset +2, `0..100`만 유효
- entry charging: offset +3, charging `0x01`, not charging `0x02`, disconnected `0x04`,
  optimized charging `0x05`

raw AAP bytes, serial, firmware, private key message는 저장하거나 표시하지 않는다. 1차 구현은
battery command만 해석하고 나머지는 command count만 누적한다.

## 5. 수명주기와 동시성

```text
ACL connected
  -> foreground service
      -> BLE scan fallback
      -> single background AAP task
          -> public API socket create
          -> connect (8초 watchdog)
          -> handshake + notification enable (10초 응답 watchdog)
          -> blocking read loop
              -> pure AapBatteryDecoder
              -> main handler
              -> atomic snapshot + surfaces

ACL disconnected / Bluetooth off / service destroy
  -> close socket (read/connect interrupt)
  -> cancel watchdog
  -> stop BLE
  -> shutdown executor
  -> connected=false snapshot
```

같은 service에 start intent가 중복되어도 AAP task는 하나만 존재한다. 첫 실패 뒤 같은 세션에서
빠른 무한 retry를 하지 않는다. 사용자가 widget 새로고침을 명시적으로 누르면 service 새
세션으로 한 번 다시 시도할 수 있다.

구현 중 BLE fallback과 AAP가 동시에 snapshot을 쓸 때의 역행 경로를 발견했다. fresh AAP
snapshot이 있으면 BLE decile은 저장하지 않으며, 회전 BLE 주소 불일치도 confidence를
`NEEDS_CALIBRATION`으로 낮추지 않는다. 이 source 우선순위는 화면 장식이 아니라 공통 store
입구에서 적용해 앱·위젯·알림 모두에 동일하게 작동시킨다.

## 6. 예측

| ID | 예측 | 반증 조건 | 반영 |
|---|---|---|---|
| P-AAP-1 | API 37 공개 socket class와 create method가 SM-F971N에 존재한다. | class/method lookup 실패 | **부분 일치 후 반증:** class/builder는 있으나 BR/EDR type을 거부 |
| P-AAP-2 | 선택 AirPods가 PSM 0x1001 연결과 status 0 handshake를 받는다. | connect 거부/timeout 또는 nonzero status | **미확정:** non-SDK socket connect는 성공, handshake 전/무응답 상태에서 ACL 종료 |
| P-AAP-3 | notification enable 뒤 command 0x0004 battery가 온다. | handshake는 성공하지만 10초간 battery 0건 | battery request 순서 검토 |
| P-AAP-4 | AAP socket이 A2DP 음악 연결을 끊지 않는다. | socket 시작 순간 오디오 profile disconnect | **반증:** 자동 AAP 제거·안전 APK 설치, 설계 재개방 |
| P-AAP-5 | 1% 값이 물리적 좌우와 charging을 정확히 반영한다. | 한쪽 case 시험이 wire type과 불일치 | decoder fixture와 type mapping 정정 |

## 7. 의존성 우선 구현 순서

화면이나 socket부터 만들지 않는다.

1. source가 포함된 snapshot/serialization 계약과 percent 범위 수정
2. Android 없는 AAP battery decoder와 malformed/disconnected fixture
3. 공개 API 37 socket adapter
4. service 병렬 수명주기와 watchdog
5. 공통 store/notification/widget 연결
6. 실제 연결, 한쪽 case, disconnect 회귀

decoder가 먼저인 이유는 socket 성공 뒤 처음 들어온 bytes를 디버그 화면에 그대로 뿌리는
유혹을 막고, 잘못된 길이나 127 값을 실제 배터리로 저장하지 않기 위해서다.

## 8. P-AAP-4 반증 뒤 1회 재시험 설계

### S-AAP-4 — foreground에서 사용자가 요청한 한 번만 재시험

1. 평소 ACL connect receiver는 BLE fallback만 시작하고 non-SDK socket을 절대 열지 않는다.
2. 앱이 화면 앞에 있을 때 사용자가 `직접 배터리 1회 테스트`를 눌러야만 experiment extra가
   붙은 service start가 일어난다.
3. socket connect 뒤 handshake 한 packet만 보내고 connect response status 0을 기다린다.
4. handshake 성공 뒤 notification enable 두 packet과 InitExt를 각각 write+flush한다.
5. 근거가 부족했던 명시적 battery request command `0x0003`은 보내지 않고 push battery만
   기다린다.
6. 30초 안에 battery가 없으면 socket과 service를 정리한다. ACL이 끊겼다가 자동 재연결돼도
   receiver start에는 experiment extra가 없으므로 같은 시도가 반복되지 않는다.

베스트 프랙티스 반복 검증:

- 1차 검증: UI와 receiver가 같은 start method를 쓰던 구조를 분리해 자동 재시도 경로를 닫는다.
- 2차 검증: handshake response 전에 session message를 보내지 않아 protocol 상태 경계를
  보수적으로 지킨다.
- 3차 검증: 성공 기준을 battery 숫자만으로 두지 않고 A2DP/ACL 20초 유지까지 함께 본다한다.
- 4차 검증: 실패하면 실험 dependency를 다시 제거하고 안전 APK로 되돌린다.

재시험 예측:

| ID | 예측 | 반증 조건 | 즉시 조치 |
|---|---|---|---|
| P-AAP-R1 | hidden socket은 이전처럼 생성·연결된다. | socket create/connect 실패 | **1회 일치:** 18:18:41 type 3 connected |
| P-AAP-R2 | handshake-only 전송 뒤 status 0 응답이 온다. | 응답 전 remote close/ACL disconnect | **1회 일치:** v2 handshake 응답 뒤에만 활성화 전송 |
| P-AAP-R3 | enable 뒤 battery push가 온다. | handshake 성공 뒤 20초 battery 0 | **1회 일치:** 왼쪽 98%, 오른쪽 92%, 케이스 unknown 수신 |
| P-AAP-R4 | A2DP와 ACL이 20초 이상 유지된다. | profile/ACL disconnect | **1회 일치:** 30초 전 구간 connected, 종료 후에도 유지 |

세 독립 연결이 모두 통과했고, 마지막 시험은 왼쪽 착용·오른쪽 case 충전이라는 물리 상태와
wire mapping이 일치했다. 자동 경로 승격의 실기기 기준은 충족됐다. 다만 현재 설치 APK의
`ConnectionReceiver`는 아직 BLE만 시작하며, 자동화 구현도 연결당 한 번·30초 제한과 실패 뒤
무재시도 원칙을 그대로 유지해야 한다.
