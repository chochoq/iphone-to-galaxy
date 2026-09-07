# 2026-09-04 — 설치·위젯·미연결 수명주기 결과

## 1. 사용자 장면

사용자의 SM-F971N에 새 개인용 앱을 처음 설치했다. AirPods는 페어링되어 있지만 시험 당시
연결되지 않았고 MaterialPods는 비교·복구 수단으로 유지했다. 목표는 실제 배터리 값에
앞서 APK 신뢰 경계, 앱 화면, widget 배치, 미연결 정리 동작을 검증하는 것이었다.

## 2. 구현 전 예측

- APK는 Bluetooth scan/connect, notification, connected-device foreground service,
  boot restore만 요청한다.
- 한 대뿐인 bonded AirPods는 자동 선택되지만 모델 세대는 이름으로 추측하지 않는다.
- Samsung widget picker에서 4×1 provider가 보이고 홈 화면에서 세 component가 보인다.
- 미연결 상태의 사용자 시작은 영구 감시가 되지 않고 30초 뒤 스스로 정리된다.

## 3. 실제 관찰

1. 순수 JVM 테스트 27개가 통과했다. type, length, prefix, unknown nibble, 좌우 flip 두
   경우, charging flag, freshness, 저전력 edge가 포함됐다.
2. 서명 APK의 permission dump에는 `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`,
   `POST_NOTIFICATIONS`, 두 foreground service permission, `RECEIVE_BOOT_COMPLETED`만
   있었다. Internet, location, overlay, accessibility, microphone은 없었다.
3. 설치 후 bonded AirPods 한 대가 이름으로 선택됐고 Bluetooth·알림 runtime permission이
   모두 granted로 확인됐다. 주소는 화면이나 도구 결과에 기록하지 않았다.
4. 앱 첫 화면은 1248×1972에서 제목, 선택 기기, 세 battery placeholder, 자동화 switch,
   순차 permission 설명이 잘리지 않았다.
5. 첫 widget picker는 provider를 찾았지만 preview가 `위젯을 추가할 수 없습니다`라고
   표시됐다. 로그에는 `AppWidgetHostView: missing defaultLayout`이 있었다.
6. `previewLayout`을 추가해 재설치한 뒤 picker가 실제 widget 레이아웃을 inflate했고 공식
   `추가` 버튼으로 appWidgetId 27이 Samsung launcher에 bind됐다.
7. picker의 축소 미리보기에는 제목만 보였지만 실제 4×1 홈 화면은 기기명, 새로고침,
   `왼쪽 — · 오른쪽 — · 케이스 —`, `연결 안 됨` 세 줄을 모두 표시했다.
8. 미연결 상태에서 앱 버튼으로 확인을 시작하자 service type `connectedDevice`, 무음 LOW
   ongoing notification이 만들어졌다. 30초 뒤 service 목록과 활성 notification 목록에
   앱 항목이 0개였다.

## 4. 예측과 다른 점

Samsung picker는 `initialLayout`을 실물용으로 알고 있어도 미리보기용 default layout을
자동으로 재사용하지 않았다. 또한 picker의 preview host 높이는 실제 홈 화면보다 작았다.
따라서 picker에 provider가 보이는 것, preview가 inflate되는 것, 실제 홈에서 정보가
보이는 것을 세 개의 별도 시험으로 분리해야 했다.

## 5. 원인 후보와 배제 근거

- provider 미등록은 아니었다. 첫 시도부터 `dumpsys appwidget`에 provider가 있었다.
- 실제 `RemoteViews` 부적합도 아니었다. `previewLayout` 선언 뒤 같은 layout이 picker와
  실제 홈에서 inflate됐다.
- 3줄 레이아웃 자체의 높이 실패도 아니었다. 실제 widget bounds 4×1에서 세 줄이 모두
  UI tree에 존재하고 화면에도 보였다.

## 6. 설계 반영

- 004 문서에 Samsung picker의 `previewLayout` 계약을 추가했다.
- INDEX 변경 이력에 첫 실패, 정정, 실제/preview 분리 판단을 남겼다.
- widget 완료 조건을 provider 노출 하나가 아니라 preview와 실물 렌더 확인으로 강화했다.

## 7. 다음 반증 시험

다음 시험은 사용자가 AirPods를 실제 연결한 상태에서 수행한다. 먼저 Apple 후보/유효/주소
불일치 카운터로 `neverForLocation`과 identity address 예측을 판정한다. exact frame이
들어오면 좌·우·케이스 표시와 session popup을 확인하고, 이어서 오른쪽/왼쪽 한쪽만
케이스에 넣어 초기 좌우 가설을 반증한다.
