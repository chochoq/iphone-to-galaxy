# 007 — Apple 실제 화면을 기준으로 한 연결 시트 재설계

## 1. 왜 기존 설계를 폐기하는가

첫 구현은 권한, DND, 잠금, 세션 중복, 6초 제거와 같은 실패 비용이 큰 기반을 먼저 검증했다.
그 과정에서 시각 표면은 남색 상단 카드와 세 개의 배터리 박스를 사용했다. 실제 연결·자동 제거는
성공했지만, 사용자가 “Apple 제품에서 뜨는 것과 디자인이 너무 다르다”고 느꼈다. 맞는 피드백이다.
첫 설계는 Apple 화면을 직접 확인한 결과가 아니라 기능을 빠르게 읽히게 하려는 자체 해석이었다.
기능의 성공을 시각 경험의 성공으로 요약하지 않고 renderer 설계를 다시 연다.

## 2. 확인한 근거와 관측 범위

### 2.1 공식 동작 근거

- Apple Support, [Charge your AirPods](https://support.apple.com/en-us/119912): iPhone/iPad에서
  뚜껑을 열거나 AirPods를 case에서 꺼내 가까이 두는 장면, 배터리 상태 확인 흐름을 확인했다.
  2026-05 갱신본은 iOS 26 설정 화면을 싣지만 nearby popup의 상세 픽셀 명세는 제공하지 않는다.

### 2.2 실제 화면 교차 관측

- Fix369, [AirPods 4 battery popup 촬영](https://www.youtube.com/watch?v=QOhP9NYwUNk),
  2025-01-14: 이미 연결된 AirPods에서 하단 시트가 뜨며 중앙 `AirPods`, 우측 상단 X,
  이어버드/케이스 두 제품 그림, 각 그림 아래 작은 초록 battery와 percentage가 보인다.
- Fix369, [AirPods Pro 3 pairing popup 촬영](https://www.youtube.com/watch?v=u1COzcLLCdo),
  2025-10-10: 같은 하단 시트 문법에 큰 제품 그림과 파란 Connect 버튼이 더해진다.
- Mobileinto, [AirPods pairing popup 촬영](https://www.youtube.com/watch?v=binGXNUkT9s),
  2026-03-25: 별도 기기·촬영에서도 하단 고정, 밝은 표면, 배경 dim, 중앙 제목의 계층이 반복된다.

pairing 화면의 Connect 버튼은 우리 장면과 목적이 다르므로 복사하지 않는다. paired-battery 화면을
주 기준으로 사용하고, 서로 다른 촬영에서 반복된 구조만 Apple 시각 문법으로 확정한다.

## 3. 사용자 시나리오

### S-012 — 화면을 보며 AirPods를 착용한다

1. 사용자가 잠금 해제된 Galaxy를 사용하는 중 AirPods를 착용한다.
2. Galaxy ACL과 AAP exact battery가 도착한다.
3. 뒤 앱이 약 30% 어두워지고 화면 아래에서 밝은 시트가 부드럽게 올라온다.
4. 시트 중앙에는 `테스트용 AirPods Pro`, 아래에는 큰 흰색 이어버드 그림이 보인다.
5. 그림 아래에서 `왼쪽 86%`와 `오른쪽 94%`를 읽는다. case 값이 실제로 들어온 때만 case
   그림과 값을 두 번째 묶음으로 본다.
6. 아무것도 하지 않으면 6초 뒤 아래로 내려가며 사라진다. X를 누르면 즉시 사라진다.

### S-013 — case 값이 이번 연결에 없다

1. 이어버드 착용 연결은 성공했지만 AAP가 case component를 보내지 않는다.
2. 시트는 `케이스 —` 박스를 만들지 않는다. 이어버드 그림과 좌우 값만 중앙에 둔다.
3. 모르는 값을 0%처럼 보이게 하거나 빈 제품이 고장 난 것처럼 보이게 하지 않는다.
4. case를 열어 수동 확인해 값이 생긴 다음 preview하면 두 제품 묶음이 나타난다.

### S-014 — dark mode 또는 큰 글자

1. Galaxy가 dark mode이면 표면은 iOS 계열의 짙은 중성 회색, 글자는 흰색으로 바뀐다.
2. 제품 그림의 흰색 외곽과 회색 그림자는 두 테마에서 모두 분리된다.
3. 글자 크기가 커져도 제목은 한 줄 ellipsis, 좌우 battery는 두 줄까지 허용한다.
4. X의 touch target은 시각 크기와 무관하게 48dp 이상을 유지한다.

## 4. 사용자 시나리오의 best practice와 반복 검증

### BP-A — 연결과 페어링을 혼동하지 않는다

- 1차 검증: Apple paired-battery 촬영에는 파란 Connect 버튼이 없다.
- 2차 검증: pairing 촬영 두 개에는 Connect 또는 연결 진행 버튼이 있다.
- 결론: 우리 앱은 이미 Android Bluetooth가 연결된 뒤 실행되므로 버튼을 두지 않는다.

### BP-B — 실제 관측값만 보여 준다

- 1차 검증: Apple은 보통 이어버드 묶음을 하나의 값으로 축약한다.
- 2차 검증: 우리 AAP는 왼쪽과 오른쪽이 86/94처럼 다르게 들어오며 한 값으로 합칠 근거가 없다.
- 3차 검증: case unknown을 `—` 제품으로 크게 놓으면 정보가 아니라 결손을 강조한다.
- 결론: Apple의 두 제품 묶음 계층은 따르되 이어버드 아래에는 좌우 값을 각각 쓰고, case는
  값이 있을 때만 나타낸다.

### BP-C — Apple처럼 보이되 시스템 UI인 척하지 않는다

- 제품명은 실제 사용자가 선택한 이름을 쓴다.
- Apple 공식 bitmap이나 애니메이션을 APK에 넣지 않는다. 로고와 글자 없이 별도로 생성한 독립
  제품 render를 투명 자산으로 묶고, 실제 값과 무관한 표시는 그림 안에 넣지 않는다.
- Android의 overlay 권한과 system alert 표시는 숨기지 않는다.
- 전체 화면 activity, Face ID·Dynamic Island 모사, Apple logo는 사용하지 않는다.

### BP-D — 시각 수정이 당시 검증된 안전 계약을 깨지 않는다

- `ConnectionPopupPolicy`, session claim, low-battery 우선순위, notification fallback은 그대로 둔다.
- renderer만 교체하고 6초 timeout과 service destroy 제거를 같은 controller에 유지한다.
- 미리보기와 실제 자동 연결이 동일한 `buildSheet`를 사용해야 한다.
- WindowManager type 2038 개수가 표시 중 1, 종료 뒤 0인지 다시 확인한다.

**후속 정정:** 여기서 DND 억제를 안전 계약으로 본 것은 틀렸다. 자정의 실제 연결에서 overlay가
억제된 뒤 notification fallback까지 DND에 intercepted되어 사용자가 아무 표면도 보지 못했다.
잠금·화면 꺼짐 경계는 유지하되, 화면을 실제로 보는 중의 무음 카드는 DND 중에도 허용한다.
상세한 반증과 새 정책 행렬은 설계 009가 현재 기준이다.

## 5. 화면 설계 인덱스

### V-1 배치

- 위치: 화면 하단 중앙
- 폭: 사용 가능 폭 - 24dp, 최소 304dp
- 하단 간격: navigation inset 위 10~12dp
- 표면 높이: content 기반 약 330~370dp; case가 없으면 불필요하게 줄이지 않고 제품 그림을 크게 유지
- corner radius: 30dp
- 뒤 화면 dim: 0.28~0.32, 별도 전체 화면 view를 만들지 않고 window dim 사용

### V-2 정보 계층

```text
              테스트용 AirPods Pro        ×

          [큰 이어버드 한 쌍 그림]     [열린 case 그림]

          왼쪽 86%  오른쪽 94%            케이스 84%
             작은 초록 battery             작은 초록 battery
```

case unknown이면 오른쪽 묶음을 제거하고 이어버드 묶음을 중앙 정렬한다. `Galaxy에 연결됨`,
세 개의 진한 배터리 박스, `6초 후 닫힘` footer는 Apple 촬영 화면에 없으므로 시각 표면에서 뺀다.
자동 종료 자체는 유지한다.

### V-3 색과 동작

- light surface `#F2F2F7`, primary `#111114`, secondary `#636366`
- dark surface `#2C2C2E`, primary `#FFFFFF`, secondary `#AEAEB2`
- battery fill `#34C759`, empty track `#C7C7CC`
- shadow/elevation은 약하게 18dp; 카드 내부의 별도 colored box는 없음
- 등장: 아래 48dp + alpha 0에서 280ms decelerate
- 종료: 아래 28dp + alpha 0으로 200ms

## 6. 의존성 우선 구현 순서

문서 읽는 순서대로 만들지 않는다.

1. 가장 재사용 범위가 큰 battery glyph와 투명 product asset 계약을 theme-independent 입력으로 만든다.
2. case known/unknown과 L/R partial 상태를 받아 묶음 수를 정하는 presentation helper를 만든다.
3. 그 위에 bottom-sheet renderer를 조립한다.
4. 마지막에 WindowManager gravity, dim, animation을 바꾼다.
5. 미리보기 screenshot을 먼저 보고 typography와 art를 수정한 뒤 실제 재연결을 한 번 검증한다.

이 순서면 위치나 색을 다시 바꿔도 배터리 진실성, 제품 그림, 수명 제어를 분리해 수정할 수 있다.

## 7. 결과 예측과 반증 조건

| 예측 | 반증 조건 | 다음 수정 |
|---|---|---|
| 남색 배너보다 Apple 연결 화면으로 즉시 인식된다. | 사용자가 여전히 일반 Android 알림처럼 느낀다. | 제품 art 크기·시트 높이·등장 motion 재측정 |
| case unknown 연결에서는 이어버드가 중앙에 크게 보인다. | 빈 오른쪽 공간이나 `—`가 주 정보처럼 보인다. | 묶음 width/visibility 수정 |
| navigation bar 위에 10dp 이상 간격이 남는다. | gesture handle 또는 taskbar와 겹친다. | system inset 기반 y 계산 보정 |
| dim과 6초 수명이 뒤 앱 사용을 과도하게 막지 않는다. | 뒤 화면을 읽기 어렵거나 touch가 오래 막힌다. | dim 0.2 또는 수명 5초 비교 |
| light/dark 모두 흰 제품 윤곽이 분리된다. | light에서 그림이 사라지거나 dark에서 번진다. | outline/shadow 명도 조정 |

설계하면서 느낀 점은 “크다”와 “Apple 같다”가 전혀 같은 요구가 아니라는 것이다. 첫 구현은 크기와
수명은 맞았지만, Apple popup을 기억하게 만드는 핵심은 하단에서 올라오는 밝고 비어 있는 표면과
제품 자체가 정보의 중심이라는 점이었다. 이번 수정은 장식 색을 Apple처럼 바꾸는 일이 아니라
정보의 주인공을 박스에서 제품과 배터리로 바꾸는 일이다.

## 8. 변경 이력 — 실제 연결 카드와 검사 미리보기의 수명을 분리한다

### S-015 — 연결하지 않은 채 디자인을 천천히 확인한다

1. 사용자가 AirPods를 현재 연결하지 않은 상태에서 앱의 `큰 연결 카드 미리보기`를 누른다.
2. 앱은 저장된 마지막 배터리 snapshot을 사용한다. 따라서 미리보기 표시 여부를 현재 Bluetooth
   연결 여부와 잘못 묶지 않는다.
3. 시트는 6초 뒤 사라지지 않는다. 사용자가 제품 그림, 정보 계층, 크기를 충분히 살핀 뒤 X로 닫는다.
4. 실제 연결로 자동 표시된 시트는 계속 6초 뒤 사라져 일상 사용을 가로막지 않는다.

처음에는 실제 표면과 미리보기가 같은 renderer뿐 아니라 같은 6초 수명도 공유하게 했다. 렌더 경로를
하나로 유지하는 것은 회귀 방지에 도움이 됐지만, 사용자가 대화 화면을 본 뒤 휴대폰으로 시선을 옮기는
검사 장면에서는 6초가 지나 결과를 놓치기 쉬웠다. **정정:** 시각 renderer와 닫기 동작은 공유하되,
수명 정책만 `explicitPreview`에서 분리한다. 자동 카드는 6초, 사용자가 직접 요청한 미리보기는 X 또는
앱 종료까지 유지한다. 지속 미리보기는 연결 자동화의 방해도나 DND 계약을 넓히지 않는다.

검증 기준은 미리보기 표시 8초 뒤에도 package 소유 type 2038 window가 정확히 1개이고, X를 누르면
0개가 되는 것이다. 이어서 실제 연결 경로의 6초 자동 제거가 그대로인지 별도 회귀 확인한다.

### 후속 정정 — 실제 연결 카드도 사용자가 닫을 때까지 유지한다

위 분리는 구현·설치 직후 사용자의 의도를 다시 확인하면서 곧바로 대체됐다. 사용자가 원한 것은 검사
미리보기만 오래 두는 것이 아니라, 실제 연결 카드도 본인이 카드 또는 X를 누를 때까지 남는 동작이었다.
따라서 `automatic=6초`, `preview=지속`이라는 중간 판단은 현재 설계가 아니다.

- 자동 연결과 명시적 미리보기 모두 timeout을 예약하지 않는다.
- 카드 본문을 누르면 앱을 열지 않고 시트만 닫는다. X도 같은 dismiss 경로를 사용한다.
- AirPods 연결 해제나 service 종료에서는 stale battery surface를 남기지 않도록 기존 즉시 정리를
  유지한다.
- 잠금·화면 꺼짐에서 자동 카드를 처음부터 띄우지 않는 주의 경계는 유지한다. DND 억제는
  2026-09-05 실제 무표시 실패로 폐기했으며 설계 009가 이를 대체한다.

이 선택은 놓치지 않는다는 요구를 가장 직접적으로 만족하지만, 닫지 않은 카드가 화면 아래를 오래
가릴 수 있다는 비용이 있다. 사용자가 직접 닫기를 원한다고 명시했으므로 지금은 지속 표시를 우선하며,
실기기에서는 8초 뒤 window 1개, 본문 tap 뒤 0개, 다시 표시해 X 뒤 0개를 각각 확인한다.
