# AAP 실기기 시험 — 공개 API 반증과 오디오 안전 중단

## 1. 공개 API 단계 분리

첫 진단은 `API=3, 소켓 생성=0, 실패=3`이었다. stage를 class, builder, type, PSM,
build, method, invoke로 세분화한 뒤 한 번만 재현하자 누적 신규 값은 `class=1,
builder=1, type=0`이었다. `Builder.setSocketType(3)` 호출에서 멈춘 것이다.

Android 공식 Bluetooth module의 현재 `BluetoothSocketSettings.Builder`를 다시 읽었다.
허용 type은 `TYPE_RFCOMM`과 `TYPE_LE`뿐이며, `TYPE_L2CAP=3`은 IntDef에는 있어도 builder
검증식에서 허용하지 않았다. `setL2capPsm` 또한 LE용 `128..255`만 허용한다. 이전 참고
구현도 public 시도 실패 후 hidden fallback으로 넘어가고 있었는데, 이를 “public AAP
지원”으로 잘못 해석했다.

## 2. 좁은 non-SDK fallback 통제 시험

개인용 sideload APK 한 process에서 `BluetoothDevice` class만 exempt하고
`createInsecureL2capSocket(0x1001)` 하나를 호출했다. 새 권한이나 앱 runtime INTERNET은
추가하지 않았다. 시스템 기록에서 type 3 socket은 18:02:54.717에 CONNECTED가 됐다.

그러나 18:03:00.165에 ACL이 reason 8로 종료되며 A2DP와 HFP가 함께 끊겼다. 자동 profile
재연결 중 service도 다시 시작돼 두 번째 socket이 18:03:00.557에 연결됐고, 18:03:01.688에
ACL reason 20으로 다시 종료됐다. handshake success와 battery는 0건이었다.

## 3. 피드백 분석과 즉시 조치

socket 생성 가능 여부와 제품 사용 가능 여부는 다르다. P-AAP-4의 반증 조건이 실제로
발생했으므로 더 많은 자동 retry나 좌우 시험을 하지 않았다. 다음 연결에서도 반복될 수
있는 설치 상태를 먼저 제거해야 했다.

- service의 자동 AAP 시작을 제거했다.
- HiddenApiBypass dependency와 network build dependency를 제거했다.
- BLE fallback, 앱, 위젯, notification의 기존 안전 경로만 남겼다.
- 47개 core assertion, APK 서명, 금지 권한 검사를 재실행한 뒤 안전 APK를 설치했다.

안전 APK 설치 뒤 같은 AirPods를 다시 연결했다. ACL/A2DP/HFP가 정상 연결됐고 30초 이상
유지됐으며, 시스템 socket 기록에 우리 앱의 type 3 연결이 새로 생기지 않았다. foreground
service와 `배터리 확인 중` 알림은 연결 동안 유지됐다. 따라서 위험 경로 제거는 실기기에서
재확인됐지만 battery 제품 목표는 아직 달성되지 않았다.

## 4. 다음 가설

통제 시험은 production 참고 구현과 달리 명시적인 battery request `04 00 04 00 03 00`을
추가로 보냈고, packet마다 flush하지 않았다. 이 차이가 peer의 ACL 종료 원인인지 아직
확정하지 않는다. 재시험한다면 자동 경로가 아니라 foreground의 한 번짜리 실험으로만,
battery request 제거와 packet별 flush를 적용하고 사용자의 명시적 확인 뒤 수행한다.
