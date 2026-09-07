# AAP 직접 배터리 — 구현 결과 예측과 실기기 전 사전 검사

## 1. 이번 반복에서 구현한 경계

선택된 bonded device 하나에만 Android 17 공개 `BluetoothSocketSettings` API로 L2CAP
PSM `0x1001` 연결을 연다. hidden API 우회는 없다. session open, notification enable,
InitExt, battery info request까지만 보내며 ANC·이름·개인키·serial 관련 명령은 보내지 않는다.
수신 원시 frame은 메모리에서 한 번 해석한 뒤 버리고 저장·로그 출력하지 않는다.

공통 snapshot에는 `AAP_EXACT_PERCENT` source를 새로 두었다. 최근 AAP 값이 있으면 주변 BLE
decile 또는 회전 주소의 불일치 신호가 exact 값을 덮거나 calibration 상태로 후퇴시키지
않는다. 연결 해제에서는 AAP socket, BLE scanner, foreground notification이 같은 service
종료 경로로 정리된다.

## 2. 실행 전 결과 예측

### 2.1 정상 경로

1. 에어팟이 A2DP/HFP로 연결되면 ACL receiver가 foreground service를 한 번 시작한다.
2. 로컬 진단에서 `API → 소켓 생성 → 연결 → 요청 → 응답 → 배터리`가 순서대로 1 이상이 된다.
3. 첫 battery message 뒤 앱·ongoing notification·홈 위젯이 같은 좌/우/케이스 값을 보인다.
4. 값이 79%처럼 10의 배수가 아니어도 그대로 표시하고 앱 설명은 `1% 단위 직접 확인`으로
   바뀐다.
5. 같은 시간에 들어온 BLE 신호는 fresh AAP 값을 덮지 않는다.

### 2.2 구분할 실패 경로

- `API=0`: service가 시작되지 않았거나 실행 기기가 API 37 미만이다.
- API만 증가: public builder/socket 생성 단계 문제다.
- 소켓 생성 뒤 시간초과: 선택 기기는 bonded지만 실제 ACL 사용 가능 상태가 아니다.
- 연결·요청 뒤 응답 시간초과: L2CAP 연결은 됐으나 AAP handshake가 성립하지 않았다.
- 응답 뒤 배터리 0: protocol session은 성공했지만 notification/request의 battery frame을
  받지 못했다. 이 경우 control 기능을 늘리지 않고 request timing만 별도 실험한다.

## 3. 사전 테스트 결과

- 순수 Java core test: 47 assertions 통과.
- AAP decoder: Pro 3 캡처의 left 80%, right 79%, case 48%, charging 해석 통과.
- malformed entry, 다른 packet/service/command, 127/255 unknown, disconnected component
  회귀 통과.
- APK: compile SDK 36에서 reflection 경계 compile 성공, v3 서명 검증 성공.
- 정책 검사: INTERNET, LOCATION, OVERLAY, ACCESSIBILITY, MICROPHONE 권한 없음.
- SM-F971N 업데이트 설치 성공. 기존 selected device, Bluetooth/notification 권한과 widget
  데이터가 유지됐고, 에어팟이 케이스 안에서 disconnected일 때 숫자를 만들어 내지 않았다.

## 4. 아직 판정하지 않은 것

공개 socket API의 실제 Samsung Android 17 동작, Pro 3 handshake, 실배터리와 좌우 물리 대응,
A2DP 비간섭, disconnect cleanup은 에어팟을 다시 착용해 연결한 다음 판정한다. 이 단계 전에는
“제품 경로 완료”라고 기록하지 않는다.
