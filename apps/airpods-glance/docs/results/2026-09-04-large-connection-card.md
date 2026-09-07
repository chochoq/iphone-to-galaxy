# 큰 연결 카드 실기기 결과 — 2026-09-04

## 1. 사용자 장면과 물리 상태

사용자는 AirPods 연결 때 기존 high-importance 알림이 일반 알림처럼 작아 연결과 배터리를 바로
알기 어렵다고 했다. MaterialPods 정도의 큰 표면을 원하지만 광고나 상시 대기 화면은 원하지
않는다. 시험 기기는 SM-F971N Android 17이고 저장 snapshot은 `테스트용 AirPods Pro`, 왼쪽
87%, 오른쪽 94%, case unknown이다.

## 2. 구현 전 예측

- `TYPE_APPLICATION_OVERLAY`는 Samsung의 명시적 `다른 앱 위에 표시` 허용 뒤에만 생성된다.
- 현재 폭에서 좌우 16dp를 뺀 카드는 세 component를 자르지 않고 보여 준다.
- 당시 예측은 화면 켜짐·잠금 해제·DND 아님·값 있음 조건에서만 자동 카드가 표시된다는 것이었다.
  이 DND 예측은 2026-09-05 실사용에서 반증되어 설계 009로 대체됐다.
- 미리보기는 저장 snapshot으로 같은 renderer를 실행하고 6초 뒤 view를 완전히 제거한다.
- 닫기 target은 자동 종료를 기다리지 않고 즉시 같은 view를 제거한다.

## 3. 실제 화면과 진단값

- Samsung 설정에서 이 앱의 overlay만 허용했고 app-op은 `SYSTEM_ALERT_WINDOW: allow`였다.
- 최종 APK의 preview frame은 `[42,110]-[1206,648]`이었다. 상태 표시줄 아래에서 화면 너비에
  가깝게 보였고 기기명, 왼쪽 87%, 오른쪽 94%, case `—`, 6초 안내가 잘리지 않았다.
- 생성 중 WindowManager에서 package 소유의 type 2038 window를 확인했다.
- 6초 뒤 같은 window 검색 결과가 없어 `auto_dismiss_pass`였다.
- 다시 표시해 X를 누른 뒤에도 window가 없어 `close_button_pass`였다.
- core policy를 포함한 54 JVM assertions, APK 서명과 권한 policy 검사가 통과했다.
- 실제 시험에서 양쪽을 case에 10초 넣었다가 다시 착용하자 19:18:11 AirPods가 Galaxy에
  remote 연결됐다. 앱을 누르지 않아도 service가 Bluetooth broadcast 사유로 시작됐고 type
  2038 카드가 생성된 뒤 19:18:17 제거됐다.
- 같은 시험에서 AAP automatic 진단은 4→5, 위젯은 왼쪽 86%·오른쪽 94%로 갱신됐으며 연결
  카드 진단은 `크게 1 · 알림 대체 0`이었다. 같은 session의 중복 카드나 fallback 알림은 없었다.

## 4. 예측과 다른 점

overlay 위치와 제거 수명은 예측과 같았다. 다만 첫 screenshot에서 뒤 `MainActivity` 제목이
Android 17의 투명 status bar 아래로 들어가 있었다. overlay frame은 system이 이미 y=110부터
배치했으므로 카드 문제가 아니라 기존 앱 root의 edge-to-edge inset 누락이었다.

## 5. 원인 후보와 배제 근거

카드에 임의 top margin을 더하는 수정은 배제했다. WindowManager dump에서 카드 frame이 status
bar의 하단과 정확히 맞았기 때문이다. 앱 root가 고정 padding만 사용한 것이 원인이어서
`systemBars()` inset을 기존 padding에 더했다. 재설치 뒤 앱 제목 bounds가 y=168부터 시작해
status bar 겹침이 사라졌다.

## 6. 설계 반영

- `004-widget-notification-and-app-ux.md`에 application overlay 정책과 inset 발견을 기록했다.
- `005-validation-loop.md`의 과거 “overlay 권한 0개” 기준을 선택 권한의 제한된 사용으로 고쳤다.
- task 11은 policy, fallback, 수명, 권한, preview의 구현·검증 상태와 남은 실제 연결 시험을
  구분해 기록했다.

## 7. 다음 시험에서 반증할 내용

실제 새 AirPods ACL에서 자동 service 경로는 통과했다. 이때 남긴 “DND에서는 기존 알림
fallback만 따르면 된다”는 다음 날 실제 수면 모드에서 틀린 것으로 드러났다. Samsung DND가 그
fallback까지 가로막아 표시가 모두 사라졌기 때문이다. 과거 결과를 지우지 않고, 현재 계약은
설계 009와 2026-09-05 결과 문서가 대체한다.
