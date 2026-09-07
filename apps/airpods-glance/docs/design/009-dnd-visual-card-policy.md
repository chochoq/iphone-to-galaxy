# 009 — 수면 DND와 무음 연결 카드 표시 정책 정정

## 1. 발견된 사용자 시나리오

### S-018 — 수면 모드 중 화면을 보며 AirPods를 착용한다

1. 매일 쓰는 수면 모드가 자정에 활성화되어 Galaxy의 DND `zen_mode=1`이 된다.
2. 사용자는 잠금이 풀린 휴대폰 화면을 실제로 보고 있으며 AirPods를 착용해 연결한다.
3. 앱은 선택된 AirPods의 ACL과 AAP battery를 정상 수신한다.
4. 연결 카드는 소리나 진동 없이 현재 화면 아래에 나타난다. 화면을 켜거나 잠금을 우회하지 않는다.
5. 사용자가 카드 본문 또는 X를 누르면 사라지고, 닫지 않았다면 연결 해제나 service 종료 때 제거된다.
6. 같은 수면 모드의 알람·반복 전화·통화 연결 규칙에는 아무 변화가 없다.

### S-019 — 수면 중 휴대폰을 보고 있지 않다

1. DND 여부와 별개로 화면이 꺼져 있거나 keyguard가 잠겨 있다.
2. 앱은 화면을 깨우지 않고 잠금화면 위에도 application overlay를 만들지 않는다.
3. widget과 연결 상태는 갱신하며 notification fallback의 노출 여부는 Android DND 정책에 맡긴다.
4. 나중에 잠금을 풀었다는 이유만으로 지나간 연결 카드를 새 사건처럼 다시 띄우지는 않는다.

## 2. 최초 설계가 실패한 실제 증거

2026-09-05 00:01의 재연결에서 앱 프로세스는 type-3 AAP Bluetooth socket을 열었고 앱 화면도
AirPods를 `연결됨 · 방금 확인`으로 표시했다. 즉 연결 감지와 battery 수신은 실패하지 않았다.
그런데 package 소유 type-2038 window는 0개였다. 같은 시각 연결 fallback notification ID 2102가
두 번 생성됐지만 `intercepted ... new:!priority`로 기록됐다. 기기 전역 `zen_mode`는 1, 화면은
Awake, overlay app-op은 allow였다.

관측 사실과 해석을 분리하면 다음과 같다.

- **관측:** 연결 서비스와 AAP는 실행됐고 자동 카드 진단은 증가하지 않았다.
- **관측:** `ConnectionPopupPolicy`는 automatic + DND 조합만 false로 만들었다.
- **관측:** 그 뒤의 notification fallback도 Samsung DND에 intercepted됐다.
- **해석:** DND를 overlay와 notification 양쪽에 겹쳐 적용해 사용자에게 아무 결과도 남기지 않은
  표시 정책이 직접 원인이다.
- **불확실성:** Samsung의 다른 One UI 버전이나 사용자의 예외 앱 설정에서는 fallback notification이
  보일 수도 있다. 하지만 이 기기에서 이미 가로막힌 사실 때문에 fallback을 보장으로 간주할 수 없다.

## 3. 사용자 시나리오의 best practice와 반복 검증

### BP-A — DND의 목적을 모든 시각 표면 금지로 확대하지 않는다

- 1차 검증: 우리 application overlay는 notification sound, vibration, audio focus를 요청하지 않는다.
- 2차 검증: 화면 켜짐과 잠금 해제를 별도로 요구하므로 사용자를 깨우거나 잠금 위로 새 정보를
  노출하지 않는다.
- 3차 검증: DND 뒤 notification으로 물러나면 Samsung이 그 표면마저 차단해 연결 확인성이 0이 됐다.
- 결론: 사용자가 화면을 보고 있는 순간의 무음 연결 카드는 DND 중에도 표시한다.

### BP-B — 수면 안전 경계는 DND가 아니라 화면·잠금 상태로 고정한다

- `PowerManager.isInteractive()`가 false면 표시하지 않는다.
- `KeyguardManager.isKeyguardLocked()`가 true면 표시하지 않는다.
- 특별 overlay 권한이 없거나 battery reading이 하나도 없으면 표시하지 않는다.
- DND가 켜졌다는 사실만으로 위 세 경계를 우회하거나 강화하지 않는다.

### BP-C — 고쳤다는 말과 자동 경로의 실측을 분리한다

- pure policy에서 DND off/on 두 active-unlocked 경우가 모두 true인지 고정한다.
- DND on + locked는 계속 false여야 한다.
- 빌드·서명·권한 검사는 코드가 설치 가능한지 증명하지만 새 ACL의 실제 카드를 대신하지 않는다.
- 마지막 완료 판정은 DND가 실제로 켜진 상태의 물리 재연결에서 type-2038 window 1개와 connection
  notification fallback 증가 없음까지 확인한 뒤 내린다.

## 4. 표시 정책 인덱스

| 화면 | 잠금 | overlay 권한 | 값 | DND | 결과 |
|---|---|---|---|---|---|
| 켜짐 | 해제 | 있음 | 있음 | 꺼짐 | 무음 큰 카드 |
| 켜짐 | 해제 | 있음 | 있음 | 켜짐 | 무음 큰 카드 |
| 꺼짐 | 무관 | 있음 | 있음 | 무관 | 큰 카드 없음 |
| 켜짐 | 잠김 | 있음 | 있음 | 무관 | 큰 카드 없음 |
| 켜짐 | 해제 | 없음 | 있음 | 무관 | notification fallback |
| 켜짐 | 해제 | 있음 | 없음 | 무관 | 값이 올 때까지 큰 카드 없음 |

## 5. 의존성이 강한 순서의 구현

문서 순서대로 UI부터 바꾸지 않는다.

1. 가장 많은 호출자가 의존하는 순수 `ConnectionPopupPolicy`의 DND 행을 먼저 고친다.
2. DND active + unlocked=true, DND active + locked=false를 JVM fixture로 고정한다.
3. renderer, 3D 자산, AAP, session claim, 알림 channel은 변경하지 않은 채 APK를 빌드한다.
4. Samsung에 덮어 설치하고 overlay 권한과 DND가 그대로 유지됐는지 읽는다.
5. 마지막으로 새 물리 연결 한 번을 만들어 자동 service 경로를 확인한다.

이 순서는 시각 자산이나 Bluetooth transport를 불필요하게 다시 건드리지 않으면서, 표시 여부를 결정하는
단일 기반을 먼저 수정한다. 나중에 잠금 정책이 바뀌더라도 boolean fixture에서 결과를 먼저 검토할 수 있다.

## 6. 결과 예측과 반증 조건

| 예측 | 반증 조건 | 다음 분석 |
|---|---|---|
| DND 중 active-unlocked 재연결에 카드가 한 번 뜬다. | fallback만 생기거나 아무 창도 없다. | exact snapshot 시각, session claim, addView 예외를 분리 기록 |
| 카드가 떠도 알람·통화 설정은 바뀌지 않는다. | 앱이 audio/notification policy를 변경한다. | 해당 권한·호출을 제거하고 APK manifest 재검사 |
| 잠금 중에는 창이 생기지 않는다. | type-2038 window가 잠금 위에 생긴다. | keyguard 판정과 window flag 즉시 회귀 |
| 기존 3D 회전과 닫기 동작은 같다. | 정적 그림, 중복 창, X 실패가 생긴다. | renderer가 아니라 service/controller 수명부터 추적 |

## 7. 설계하면서 든 생각과 느낌

처음에는 “수면 모드를 존중한다”는 말을 DND일 때 큰 화면을 전부 금지하는 것으로 너무 넓게
해석했다. 하지만 사용자가 원한 수면 보호는 알람과 중요한 전화를 살리면서 불필요한 소리를 줄이는
것이었다. 이미 깨어서 화면을 보는 사람에게 소리 없는 연결 카드를 숨기는 것은 조용함이 아니라
기능 소실이었다. 특히 fallback도 같은 DND에 막힌 실제 기록이 이 차이를 분명하게 만들었다.
앞으로 주의 경계는 막연한 모드 이름보다 실제 방해 행위—화면 깨우기, 잠금 우회, 소리, 진동—로
나눠 검증한다.

## 8. 구현 뒤 실기기 피드백

수정 APK를 설치한 뒤 `zen_mode=1`을 유지해 새 ACL을 만들었다. 첫 automatic AAP는 handshake
timeout으로 값이 없어 카드 조건까지 도달하지 못했다. 같은 ACL에서 사용자가 명시한 battery 재시도는
즉시 성공했고, explicit preview가 아닌 실제 connection-card 호출에서 type-2038 창 1개가 보였다.
진단은 큰 카드만 8→9, notification fallback은 2 그대로였다. 따라서 DND boolean 수정은 실기기에서
검증됐지만 자동 battery의 일시적 timeout은 독립 관찰 항목으로 남긴다.

처음에는 package와 type이 같은 줄에 있을 것이라는 shell 검색 가정 때문에 열린 창을 0으로 세었다.
진단 counter만 믿지 않고 screenshot과 WindowManager의 전체 window block을 함께 보자 실제 창과 frame을
확인할 수 있었다. 테스트 도구도 설계 일부이므로 다음 회귀에서는 block 단위 판독 또는 window token
type을 사용한다.
