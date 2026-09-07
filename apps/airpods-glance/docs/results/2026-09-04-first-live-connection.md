# 2026-09-04 — 첫 실제 AirPods 연결

## 1. 사용자 장면

사용자가 페어링된 AirPods를 갤럭시에 실제 연결했다. MaterialPods를 함께 유지한 비교 단계였다.

## 2. 구현 전 예측

- ACL connected broadcast가 Android 17에서 connected-device foreground service를 시작한다.
- `neverForLocation` BLE scan이 공개 Apple battery frame을 받는다.
- frame 주소가 bonded identity로 resolve되면 exact snapshot을 얻는다.

## 3. 첫 관찰

- 서비스는 `BLUETOOTH_BROADCAST` 예외 사유로 백그라운드 시작이 허용됐고 connectedDevice
  foreground 상태가 됐다. 따라서 P-004는 이 연결에서 확인됐다.
- 1분가량 지난 시점에도 첫 APK 진단은 Apple 후보 0, 유효 0, 주소 불일치 0, 스캔 실패
  0이었다.
- 동시에 뜬 MaterialPods의 연결 카드도 두 battery 값을 `-`로 표시했고 광고 banner가
  함께 나타났다. 기존 앱도 이 순간에는 비교 가능한 battery 값을 얻지 못했다.
- Android Bluetooth 계층은 A2DP 연결 사건은 기록했지만 battery metadata를 별도로
  노출하지 않았다.

## 4. 예측과 다른 점

후보 0만으로 `neverForLocation` filtering이라고 결론 낼 수 없었다. 첫 구현의 scan filter가
type·length·prefix까지 미리 요구해 다른 Apple frame을 decoder와 진단에 전달하지 않았기
때문이다.

## 5. 설계·구현 반영

- 위치 권한은 추가하지 않는다. 같은 권한 범위의 MaterialPods도 이 순간 값을 못 읽었다.
- scan filter를 Apple company ID로 넓히고 decoder가 type·length·prefix를 판정한다.
- 진단에 too-short, wrong-type, wrong-length, wrong-prefix 누적값을 추가한다.
- 주변 frame의 주소나 bytes를 기록하지 않고, exact identity 전에는 battery로 채택하지 않는다.

## 6. 다음 시험

수정 APK를 설치한 뒤 현재 연결에서 화면 버튼으로 30초 scan을 시작한다. Apple 후보가
생기면 거부 상세 또는 주소 불일치로 다음 분기를 정한다. 후보가 계속 0이면 케이스를
열어 proximity advertisement를 유도한 시험과 `neverForLocation` 대조 시험을 분리한다.

## 7. 넓은 Apple filter 재시험에서 이어진 발견

- 같은 연결에서 Apple company ID만 거른 10초 scan은 후보 1,138건을 받았다.
- 유효 공개 battery frame은 0건이었고, 801건은 wrong type, 337건은 최소 길이 미달이었다.
- 따라서 `neverForLocation`이 Apple manufacturer frame 전체를 차단한다는 가설은 이
  시험으로 반증됐다. 다만 이 frame들이 선택 AirPods에서 왔는지는 아직 분리되지 않았다.
- 이 빈도에서 기존 구현처럼 매 frame마다 SharedPreferences `commit()`을 하면 진단 기능
  자체가 battery·I/O 부담이 된다. service memory에 누적한 뒤 1초마다 한 commit으로 합치는
  방식으로 정정했다.
- 다음 빌드는 raw 주소를 저장하지 않은 채 선택 주소 일치/다른 주소 횟수와 Apple message
  type histogram 상위 5개만 표시한다. 이를 케이스 개방 시험과 함께 사용한다.
