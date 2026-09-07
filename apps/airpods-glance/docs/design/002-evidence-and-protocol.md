# 002 — 근거와 배터리 프로토콜 검증

## 1. 근거 계층

1. Android 공식 문서: 권한, foreground service, 위젯 제약의 기준
2. 현재 SM-F971N의 Bluetooth 상태: 실제 페어링·연결·프로필 확인
3. 서로 다른 라이선스와 계보의 공개 구현: 프로토콜 사실의 교차 확인
4. 사용자의 AirPods 실측: 좌우·케이스 해석의 최종 판정

공개 구현이 동의해도 실기기와 다르면 실기기를 우선한다. 반대로 한 번 수신한 패킷만으로
좌우를 확정하지 않고, 물리적으로 어느 쪽을 케이스에 넣었는지 통제한 시험을 사용한다.

## 2. 출처

### Android 공식 문서

- Bluetooth 권한:
  https://developer.android.com/develop/connectivity/bluetooth/bt-permissions
- App Widget 개요:
  https://developer.android.com/develop/ui/views/appwidgets
- Foreground service 백그라운드 시작 제한:
  https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start

2026-09-04 확인 내용:

- Android 12+에서 BLE 관찰에는 `BLUETOOTH_SCAN`, 페어링 기기 조회에는
  `BLUETOOTH_CONNECT`가 필요하다.
- 위치를 파생하지 않는 경우 `neverForLocation`을 선언할 수 있지만 일부 비콘이
  필터링될 수 있다는 공식 경고가 있다.
- 위젯은 `AppWidgetProvider`, provider info XML, `RemoteViews`를 사용한다.

### 공개 구현

- CAPod: https://github.com/d4rken-org/capod
  - GPL-3.0, 성숙한 제품과 테스트를 행동 검증 근거로만 사용한다.
  - 코드를 이 프로젝트에 복사하지 않는다.
- Podsify: https://github.com/Tanexc/Podsify
  - Apache-2.0, BLE 필드 위치와 스캔 조건을 교차 확인했다.
- GreenPods: https://github.com/andrewkomkov/GreenPods
  - MIT, 공개 패킷 구조와 Pro 3 모델 ID를 교차 확인했다.

## 3. 공개 BLE 프레임 가설

Bluetooth SIG Apple company identifier `0x004C`의 manufacturer data에서 다음을
검사한다. 인덱스는 company ID 뒤의 byte array 기준이다.

| 인덱스 | 의미 | 검증 상태 |
|---:|---|---|
| 0 | message type `0x07` | 세 구현 일치 |
| 1 | remaining length `0x19` | 세 구현 일치 |
| 2 | plaintext prefix `0x01` | CAPod의 ghost-frame 회귀 근거로 추가 검증 |
| 3..4 | model ID, big-endian | Podsify·GreenPods·CAPod 일치 |
| 5 | primary/placement status | 비트 의미 일부는 실기기 검증 필요 |
| 6 | 두 이어버드 battery nibble | 세 구현 일치, 좌우 배치에 충돌 있음 |
| 7 low nibble | case battery | 세 구현 일치 |
| 7 high bits | left/right/case charging | CAPod·Podsify 일치, 실기기 검증 필요 |
| 8 | case lid/counter 관련 값 | 구현 해석 차이 존재, 1차 팝업 트리거에 사용하지 않음 |

### 배터리 값

- nibble `0..10` → `0..100%`, 10% 간격
- nibble `15` → 알 수 없음
- `11..14` → 현재 앱에서는 알 수 없음

마지막 규칙은 일부 구현이 100%로 clamp하는 것보다 보수적이다. 잘못된 100%는 충전을
미루게 할 수 있지만 `—`는 다시 확인하게 만들기 때문이다.

### 초기 좌우 가설

CAPod와 Podsify가 일치하는 가설:

- status bit 5가 set이면 왼쪽 값은 battery byte의 low nibble, 오른쪽은 high nibble
- status bit 5가 clear이면 왼쪽은 high nibble, 오른쪽은 low nibble
- 충전 flag도 같은 방향 전환을 적용
- case charging은 원래 byte bit 6

GreenPods는 battery의 high nibble을 primary로 보아 반대 배치를 사용한다. 따라서 이
가설은 코드의 초기값일 뿐, 사용자의 한쪽 케이스 시험이 끝나기 전까지 확정 사실이 아니다.

## 4. ghost frame 방어

CAPod의 최근 회귀 기록에는 type `0x07`이지만 일반 공개 상태 형식이 아닌 프레임이
연결 시 나타나 표준 offset에서 쓰레기 값을 만들 수 있다는 내용이 있다. 따라서 다음을
모두 통과해야 디코더가 값을 낸다.

1. company ID가 Apple `0x004C`
2. data 길이가 최소 10이며 선언 길이와 모순되지 않음
3. type `0x07`
4. length `0x19`
5. prefix `0x01`
6. 배터리 nibble이 개별적으로 `0..10` 또는 unknown

원시 패킷 전체는 영구 저장하지 않는다. 진단에는 모델 ID, 길이, 주소 일치 여부, RSSI,
거부 사유만 남긴다.

### 구현 중 정정 — 2026-09-04 첫 연결

첫 APK는 scan filter에서 type `07`, length `19`, prefix `01`까지 모두 요구했다. 이 방식은
decoder에는 안전하지만 진단의 `Apple 후보 0`이 “Apple manufacturer frame이 없음”인지
“Apple frame이 있으나 type/prefix가 달라 filter에서 사라짐”인지 구분하지 못했다. 실제
연결에서 서비스는 정상 시작했지만 우리 앱과 MaterialPods가 모두 값을 얻지 못해 이
관측 가능성 결함이 드러났다.

scan filter는 Apple company ID만 고정하고, type·declared length·prefix 검증은 순수 decoder로
이동한다. 거부 이유는 누적 숫자로만 보존하며 주소와 원시 bytes는 저장하지 않는다. 주변
Apple 기기 frame이 들어오더라도 decoder와 선택 기기 identity gate를 모두 통과하기 전에는
battery snapshot으로 채택하지 않는다.

## 5. 기기 식별 전략

### 1단계: 페어링된 AirPods 선택

- `BluetoothAdapter.getBondedDevices()`에서 이름에 AirPods가 포함된 기기를 찾는다.
- 현재 기기에서는 한 대이므로 자동 선택한다.
- 여러 대가 있으면 향후 선택 UI가 필요하다. 1차 구현은 첫 실행 화면에 후보를 보여 주고
  하나를 저장할 수 있도록 상태 계약을 열어 둔다.

### 2단계: 연결 게이트

- 선택 기기의 ACL/A2DP 연결이 없는 동안 manufacturer frame을 현재 상태로 채택하지
  않는다.
- 주변 기기 수집 앱이 아니라 내 연결 세션의 상태 앱으로 범위를 제한한다.

### 3단계: scan result identity

- Android가 bonded identity address로 resolve해 동일 주소를 제공하면 즉시 채택한다.
- 다른 주소만 보이면 자동으로 가장 강한 값을 확정하지 않는다.
- 진단 화면에서 `주소 일치 패킷 없음`을 보여 주고 실기기 근접 교정 설계를 다시 연다.

## 6. 첫 실기기 검증 절차

1. MaterialPods는 유지한 채 새 앱을 설치한다.
2. 새 앱의 Bluetooth와 알림 권한을 허용한다.
3. AirPods 양쪽을 케이스에 넣고 뚜껑을 닫는다.
4. 뚜껑을 열고 양쪽을 연결한다.
5. 새 앱과 MaterialPods가 표시한 세 값을 기록하되 어느 쪽이 어느 쪽인지 아직 확정하지
   않는다.
6. 오른쪽만 케이스에 넣어 오른쪽 충전 flag가 켜지는지 확인한다.
7. 반대로 왼쪽만 케이스에 넣어 좌우가 뒤집히지 않는지 확인한다.
8. 케이스를 충전기에 연결해 case charging bit를 확인한다.
9. 3회 연결을 반복해 팝업이 세션당 한 번인지 확인한다.
10. 통과한 뒤에만 MaterialPods 제거를 제안한다.
