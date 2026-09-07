# 003 — 아키텍처와 상태 수명

## 1. 데이터 흐름

```text
Android Bluetooth 연결 이벤트
  -> PairedAirPodsResolver가 선택 기기인지 확인
  -> AirPodsMonitorService 시작/중지
  -> Apple 0x004C BLE scan result
  -> AirPodsPacketDecoder의 순수 검증·해석
  -> DeviceIdentityPolicy의 주소/연결 gate
  -> BatteryStateStore에 원자적 스냅샷 저장
  -> WidgetRenderer가 모든 위젯 갱신
  -> ConnectedNotification 갱신
  -> 첫 신뢰 상태이면 ConnectionPopup 한 번
  -> 임계값을 새로 통과하면 LowBatteryNotifier 한 번
```

UI가 BLE byte를 직접 해석하지 않고, scanner가 `RemoteViews`를 직접 조립하지 않게 한다.
디코더·정책·저장 계약이 여러 표면의 공통 기반이어야 이후 좌우 해석을 고쳐도 앱, 위젯,
알림을 따로 수정하지 않는다.

## 2. 핵심 모델

```text
AirPodsSnapshot
  selectedDeviceName: String?
  selectedDeviceAddressHash: String?  // UI에 노출하지 않음
  connected: Boolean
  identityConfidence: EXACT | NEEDS_CALIBRATION | NONE
  modelId: Int?
  left: BatteryComponent?
  right: BatteryComponent?
  case: BatteryComponent?
  observedAtEpochMillis: Long?
  source: BLE_PUBLIC_DECILE

BatteryComponent
  percent: Int?       // 0,10,...100 또는 null
  charging: Boolean?  // fresh+connected일 때만 현재형으로 표현
```

주소 원문은 `SharedPreferences`에 저장하더라도 앱 private 영역에만 둔다. 진단 로그와 UI는
일치 여부만 표시한다. 해시도 디버깅 식별이 필요할 때만 저장하며 1차 구현에서는 생략할
수 있다.

## 3. 구성요소와 책임

### `AirPodsPacketDecoder`

- Android 의존성이 없는 순수 Java
- manufacturer data 한 개를 입력받아 decoded candidate 또는 rejection을 반환
- 길이, type, declared length, prefix, nibble을 검증
- 좌우 flip과 charging flag 해석을 한 곳에 둠
- 합성 패킷 단위 테스트의 첫 대상

### `PairedAirPodsResolver`

- Bluetooth 권한 상태를 확인
- 페어링된 이름과 주소를 읽음
- 저장된 선택 기기가 아직 bonded인지 확인
- 한 대이면 자동 선택, 여러 대이면 UI에 선택 필요 상태 반환
- 주소나 이름을 로그에 쓰지 않음

### `ConnectionReceiver`

- ACL connected/disconnected, Bluetooth adapter state를 수신
- 선택 기기 연결에만 반응
- connected에서 monitor service 시작
- disconnected에서 store의 connected만 false로 만들고 service 중지
- 오래 걸리는 스캔이나 렌더링을 receiver 안에서 실행하지 않음

### `AirPodsMonitorService`

- `foregroundServiceType="connectedDevice"`
- 선택 AirPods 연결 중일 때만 실행
- Apple manufacturer filter로 BLE scan
- result는 decoder와 identity policy에 전달
- 상태가 실제로 바뀔 때만 store/위젯/알림 갱신
- disconnect, Bluetooth off, 권한 취소, service destroy에서 scan을 반드시 중지
- 실측에서 Apple frame이 초당 100건 이상 들어올 수 있으므로 진단 카운터는 패킷마다
  disk commit하지 않고 process memory에서 1초 단위 batch로 합쳐 저장

### `BatteryStateStore`

- 하나의 commit으로 전체 snapshot을 기록
- 각 필드를 따로 쓰다가 위젯이 좌우가 다른 시점의 값을 읽는 일을 막음
- process 재시작 뒤에도 마지막 관측과 신선도를 복원
- live charging은 connected와 fresh 조건을 함께 만족할 때만 제공

### `AirPodsWidgetProvider`

- 저장된 snapshot만 읽어 빠르게 렌더링
- 위젯 클릭: 앱 열기
- 새로고침 클릭: 명시적 service refresh 요청
- 연결 중이 아니면 service를 억지로 오래 시작하지 않음

### 알림 구성요소

- Monitor channel: 연결 중 foreground 상태, 낮은 중요도, 무음
- Connection channel: 첫 fresh snapshot 한 번, 자동 소멸
- Battery warning channel: 임계값 통과 알림, 기본 무음 또는 낮은 소리
- 광고, 홍보, 평가, 업데이트 권유 channel 없음

## 4. 수명주기

### 연결 전

- service 없음
- ongoing notification 없음
- 위젯은 마지막 값과 `연결 안 됨` 표시

### 연결 직후

- system broadcast → service foreground 시작
- 알림에는 먼저 `배터리 확인 중` 표시
- identity가 확인된 첫 frame → snapshot 저장, 위젯과 알림 갱신, 팝업 1회

### 연결 중

- filtered scan 유지
- 같은 snapshot 반복은 버림
- `observedAt`은 새 유효 패킷에 맞춰 갱신하되 UI update를 과도하게 하지 않도록
  최소 간격 적용

### 연결 해제

- connected=false 원자적 저장
- scan 중지, foreground 종료
- charging icon 숨김
- 위젯은 마지막 관측 시각 보존

## 5. 권한

필수:

- `BLUETOOTH_SCAN` with `neverForLocation`
- `BLUETOOTH_CONNECT`
- `POST_NOTIFICATIONS`
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_CONNECTED_DEVICE`

수명주기 복원:

- `RECEIVE_BOOT_COMPLETED`: widget 캐시 재렌더 및 저장 상태 정리용. 부팅만으로 스캔 시작은
  하지 않는다.

제외:

- `INTERNET`
- 위치 권한
- `SYSTEM_ALERT_WINDOW`
- 접근성 권한
- 연락처, 전화, 마이크, 사진 권한

## 6. 오류 상태

| 상태 | 앱 | 위젯 | 서비스 |
|---|---|---|---|
| 권한 없음 | 필요한 권한과 이유 표시 | `권한 필요` | 시작 안 함 |
| Bluetooth 꺼짐 | 켜기 안내 | `Bluetooth 꺼짐` | 중지 |
| AirPods 미페어링 | 설정 이동 제공 | `기기 없음` | 시작 안 함 |
| 연결됨, frame 없음 | 진단 카운트와 재시도 | `배터리 확인 중` | 제한 시간 뒤 저전력 재시도 판단 |
| frame 있으나 주소 불일치 | 교정 필요 | `기기 확인 필요` | 원시 값 채택 안 함 |
| 유효 snapshot | 세 값과 freshness | 세 값과 freshness | 연결 중 유지 |

## 7. 구현 순서가 설계 문서 순서와 다른 이유

가장 먼저 만들어야 하는 것은 화면이 아니라 `decoder -> state contract -> identity policy`다.
좌우 해석이나 unknown 규칙이 바뀌면 모든 화면이 영향을 받기 때문이다. Widget과 알림을
먼저 만들면 예쁜 mock data가 실제 데이터 계약을 밀어내고, 나중에 null·stale·identity
상태를 억지로 끼우게 된다.
