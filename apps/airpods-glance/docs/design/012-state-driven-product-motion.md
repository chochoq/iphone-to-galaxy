# 012 — 상태별 제품 표현과 애니메이션 계약

## 012.1 이번 설계가 바로잡는 것

이전 설계는 케이스 안/밖 신호를 물리적인 열린 케이스 그림에 그대로 대응시키려 했다. 그러나
2026-09-06 실제 촬영과 2026-09-07 사용자의 iOS 실기기 확인에서, 실제 이어버드가 열린 케이스
안에 있어도 화면은 **함께 회전하는 이어버드 한 쌍 + 닫힌 케이스**를 분리해 보여 줬다.
따라서 iOS 카드의 제품 이미지는 현재의 물리 배치를 복제하는 모형이 아니라 배터리를 가진
구성요소를 읽기 쉽게 나타내는 표상으로 해석한다.

또한 첫 닫힌 case 구현은 열린 원본의 lid를 110도 회전한 근사였다. 12방향 contact sheet에서
정면만 닫혀 보이고 중간·측면에서는 lid가 몸체보다 앞으로 밀려 떠 있으며 hinge 내부가 노출됐다.
프레임 수·루프 경계 같은 파일 검증은 통과했지만 제품 형상은 실패했다. 운영 앱에서는 즉시
정적 닫힌 정면으로 되돌리고 이 근사 animation을 재사용하지 않는다.

## 012.2 상태를 두 층으로 분리한다

### 표시 상태 — 무엇의 배터리 값을 알고 있는가

- B2: 왼쪽·오른쪽 값이 모두 있음
- BL/BR: 한쪽 값만 있음
- BC: 케이스 값이 있음
- BU: 값이 없거나 오래됨

### 물리 상태 — 어디에 있는가

- P2C: 양쪽 케이스 안
- PLC/PRC: 한쪽만 케이스 안
- POUT: 양쪽 케이스 밖
- PEAR: 한쪽 또는 양쪽 착용
- PU: 미상

표시 artwork는 우선 표시 상태에 의해 정한다. 물리 상태는 갱신 신선도와 향후 동작의 근거가 될
수 있지만 열린/빈/한쪽 수납 case 그림으로 직접 바꾸지 않는다. AAP primary/secondary를 좌우로
검증하기 전에는 PL/PR 상태도 확정하지 않는다.

## 012.3 사용자 시나리오별 현재 권장안

| 시나리오 | 제품 그림 | 배터리 표시 | 움직임 | 근거/상태 |
|---|---|---|---|---|
| 케이스를 열고 양쪽 값+case 값 수신 | 이어버드 한 쌍 + 닫힌 case | L/R + case | pair 함께 회전, case도 별도 회전 | iOS 실제 촬영과 사용자 실측 |
| 양쪽을 꺼내거나 착용, 양쪽 값 유지 | 이어버드 한 쌍 | L/R | pair 함께 회전 | case 값이 유효하면 닫힌 case 유지 |
| 한쪽 값만 유효 | 해당 단일 이어버드 | L 또는 R만 | 단일 bud가 자기 중심으로 회전 | iOS 실기기 검증 전의 fallback 제안 |
| 양쪽 값의 충전/퍼센트가 다름 | 이어버드 한 쌍 | L/R 값을 분리 표기 | pair 함께 회전 | 값 차이는 artwork 분리 이유가 아님 |
| case 값만 유효 | 닫힌 case만 | case만 | 검증된 closed-case motion이 있을 때만 회전 | 현재 transport에서 드문 경계 |
| case 값 미수신/오래됨 | case 숨김 | 0%로 대체하지 않음 | 숨은 animation 없음 | 기존 unknown 계약 |
| 모든 값 미수신 | 제품 카드 대신 연결 확인 상태 | 임의 숫자 없음 | 장식 motion 없음 | 거짓 배터리 방지 |
| Reduce motion/animation scale 0 | 같은 구성요소의 정적 정면 | 동일 | 전부 정지 | Android 사용자 선택 |

## 012.4 시간과 동기화

- pair의 두 bud는 같은 제품 group으로 같은 각도에서 함께 회전한다. 각자 자기 중심 회전은 금지한다.
- pair와 case가 둘 다 보이면 같은 6초 길이와 같은 시작 frame을 사용한다. 각각의 중심축으로 돌되
  phase를 맞춰 한 제품군처럼 보이게 한다.
- iOS AirPods 4 실제 촬영의 16초와 18초 프레임에서 pair뿐 아니라 닫힌 case도 측면→정면으로
  변해 case 회전도 확인된다. 단, 정확한 easing과 반복 횟수는 이 두 프레임만으로 확정하지 않는다.
- 사용자가 카드가 유지되는 동안 지속 회전을 원했던 기존 선택에 따라 현재 목표는 6초 연속 loop다.
- 값이 갱신돼도 animation을 frame 0으로 재시작하지 않는다. 표시 구성요소가 생기거나 사라질 때만
  새 view의 첫 frame부터 시작한다.

## 012.5 자산 계약

- pair: 현재 검증된 원본 joint mesh turntable을 유지한다.
- closed case: 열린 case lid 변형 자산은 폐기한다. 처음부터 닫힌 정확한 mesh 또는 외형을
  모든 방향에서 검증할 수 있는 정본이 필요하다.
- single bud: 원본 A/B 하위 mesh는 기술적으로 분리 가능하지만 실제 L/R 대응 검증 뒤 명명한다.
- 모든 turntable은 0/45/90/135/180/225/270/315도에서 silhouette, seam, hinge, vent를 직접 본다.
- 파일 수준에서 frame 수, 고유성, duration, alpha, clipping, loop seam을 검사한다.
- 실제 카드 크기로 폰에서 최소 한 바퀴를 녹화해 시간순 움직임을 마지막에 확인한다.

## 012.6 반복 검증과 완료 조건

1차 근거: 사용자가 iOS에서 케이스-open 장면을 직접 확인한다.
2차 근거: 보관된 실제 촬영 16초/18초 프레임에서 pair와 closed case가 모두 방향을 바꾼다.
3차 반례: 우리 closed case contact sheet에서 lid 부유가 드러나 현 자산을 거부한다.

완료는 빌드 성공이 아니다. 각 상태 fixture의 레이아웃, 한 바퀴 폰 녹화, 연결 카드 닫기,
unknown 숨김, animation scale 0을 모두 확인해야 한다. 아직 iOS에서 한쪽 값만 남은 화면은 직접
확인하지 않았으므로 단일 bud 행은 확정 복제가 아니라 검증 가능한 fallback으로 표시한다.

## 012.7 내 판단

상태가 많다는 이유로 모든 조합마다 서로 다른 화려한 영상을 만드는 것은 오히려 틀린 정보를
늘린다. pair와 closed case라는 두 정본 자산을 중심으로 하고, 실제로 관측 가능한 구성요소만
조합하는 편이 단순하면서도 정확하다. 이번에는 정면 한 장의 그럴듯함보다 회전 중 어느 각도에서도
제품으로 성립하는지를 먼저 통과시켜야 한다.

## 012.8 상세 사용자 시나리오

### S-018 — 양쪽 이어버드가 케이스에 들어 있는 상태에서 뚜껑을 연다

1. Galaxy가 선택된 AirPods의 새 ACL 연결을 확인한다.
2. 기존 bounded AAP 창이 왼쪽·오른쪽·케이스 battery를 수신한다.
3. 카드는 이어버드 한 쌍과 닫힌 케이스를 나란히 표시한다. 실제 열린 케이스 내부를 복제하지 않는다.
4. pair의 두 bud는 공통 중심을 기준으로 같은 각도에서 회전한다.
5. closed case는 자체 중심으로 pair와 같은 phase에서 회전한다.
6. battery 숫자·충전 표식·제목·닫기 버튼은 움직이지 않는다.
7. 사용자가 카드나 X를 누르면 즉시 닫히며 drawable callback도 종료된다.

성공은 ‘애니메이션이 실행됨’이 아니라 360도 모든 방향에서 정상 제품이며, 값과 그림이 혼동되지
않고, 음악·Bluetooth 연결을 건드리지 않는 것이다.

### S-019 — 양쪽을 꺼내 착용했지만 이전 case 값이 함께 수신됐다

1. pair battery가 유효하면 한 쌍 artwork를 유지한다.
2. 같은 신선한 snapshot에서 case battery도 유효하면 closed case를 유지한다.
3. in-case 신호가 아니더라도 빈/open case로 바꾸지 않는다. 그림은 물리 배치가 아니기 때문이다.
4. case 값이 이후 snapshot에서 사라졌다고 0%로 만들지 않는다. fresh/unknown 계약에 따라 숨긴다.

### S-020 — 한쪽 값만 유효하다

1. decoder가 L 또는 R 한쪽 값만 반환한다.
2. 해당 side 숫자만 진실로 취급하고 다른 side를 0%로 만들지 않는다.
3. iOS 실기기에서 단일 artwork 규칙을 확인하기 전에는 기존 pair fallback을 갑자기 단일로 바꾸지 않는다.
4. 검증 뒤에는 정확한 side 단독 mesh와 label을 한 단위로 표시한다.
5. 단일 제품은 자기 중심으로 회전하되 pair의 한쪽을 잘라낸 bitmap으로 만들지 않는다.

### S-021 — case 값만 유효하거나 모든 값이 unknown이다

1. case만 유효하면 verified closed-case 정면 또는 turntable과 case 값만 표시한다.
2. 모두 unknown이면 연결 성공 안내는 가능하지만 숫자와 제품별 충전 상태를 발명하지 않는다.
3. 보이지 않는 group은 생성하지 않아 drawable이 뒤에서 계속 돌지 않게 한다.
4. cached 값은 현재 snapshot 신선도 규칙을 통과할 때만 charging animation/표식에 사용한다.

### S-022 — 시각적 motion을 줄이는 사용자와 저성능 상황

1. animator duration scale이 0이면 동일한 구성과 battery를 정적 정면으로 표시한다.
2. 프레임 decode 실패나 메모리 부족은 카드 전체 실패가 아니라 해당 제품의 정적 fallback으로 낮춘다.
3. fallback은 animation frame 0과 같은 제품·크기·방향이어야 레이아웃이 튀지 않는다.
4. 접근성 설명은 ‘회전 중’이 아니라 제품명과 battery 의미를 전달한다.

## 012.9 베스트 프랙티스와 세 번의 검증

### BP-018 — 관측값, 표시 상태, 자산을 한 boolean에 섞지 않는다

- 1차 검증: battery charging=false를 case 밖으로 해석하면 완충 수납 반례에서 실패한다.
- 2차 검증: case battery unknown을 빈 case로 해석하면 닫힌 case가 보이지 않아야 할 때 거짓 그림이 된다.
- 3차 검증: primary/secondary를 L/R로 고정하면 역할 교체 보고에서 좌우가 뒤집힌다.
- 결론: decoder truth → composition policy → asset 선택의 세 층을 분리한다.

### BP-019 — 애니메이션 파일의 정상과 제품 형상의 정상을 분리한다

- 1차 검증: 첫 closed WebP는 180 unique frames와 6초 duration을 통과했다.
- 2차 검증: 0도 정면은 닫힌 case처럼 보였다.
- 3차 검증: 30도 간격 contact sheet에서 lid 부유·관통·hinge 노출이 드러났다.
- 결론: 파일 검사 뒤 8방향 형상 검사, 그 뒤 실시간 폰 재생을 각각 독립 gate로 둔다.

### BP-020 — iOS 관측과 사용자가 선택한 변형을 구분한다

- 1차 검증: 실제 촬영에서 pair가 공동 회전하고 case도 측면에서 정면으로 바뀌었다.
- 2차 검증: 사용자의 iOS 재현이 pair 공동 회전과 closed case를 확인했다.
- 3차 검증: 정확한 easing·반복 횟수·한쪽만 유효한 화면은 아직 근거가 없다.
- 결론: 공동 pair/closed case는 관측, 6초 지속 loop는 사용자가 선택한 우리 경험,
  single-bud 규칙은 미검증이라고 각각 표시한다.

### BP-021 — 자산 검증 실패가 연결 기능으로 번지지 않게 한다

- 1차 검증: artwork는 battery decoder와 별도 layer라 static fallback이 가능하다.
- 2차 검증: 잘못된 closed animation만 정지시켜도 pair와 battery card는 유지됐다.
- 3차 검증: 새 모델 탐색이 실패해도 기존 연결·오디오·battery 수집에는 수정이 없어야 한다.
- 결론: 자산별 verified-motion gate를 두고 실패한 제품만 정적으로 낮춘다.

## 012.10 상태 결정표와 불변 조건

입력은 L-known, R-known, C-known, motion-enabled, pair-asset-verified,
case-asset-verified다. 64개 조합을 순수 policy test로 전수 검사한다.

- L && R이면 pair artwork다.
- L xor R의 최종 단일 artwork는 iOS 실측 전까지 candidate이며 운영 연결은 보류한다.
- C가 true일 때만 case group이 존재한다.
- 아무 battery도 없으면 배터리 제품 group이 없다.
- motion=false이면 검증 자산 여부와 상관없이 모든 product motion이 false다.
- asset-verified=false인 제품은 보일 수 있지만 정적이어야 한다.
- placement 값만 바뀌어도 동일한 battery composition이면 artwork 종류를 바꾸지 않는다.
- 어떤 분기도 unknown을 0 또는 charging=false로 변환하지 않는다.

## 012.11 구현 순서 — 문서 순서가 아닌 의존성 순서

1. pure composition policy와 64조합 불변 조건을 먼저 만든다.
2. closed-case mesh를 올바르게 닫는 형상 gate를 만든다.
3. 8방향 preview가 통과한 자산만 full render/encode한다.
4. encoded asset과 static fallback의 첫 frame/크기/duration을 검사한다.
5. overlay가 policy를 소비하도록 연결하되 single candidate는 feature gate로 남긴다.
6. 상태 fixture preview와 Android unit regression을 실행한다.
7. APK를 설치하고 실제 카드 한 바퀴를 화면 녹화해 시간순 피드백을 분석한다.
8. 마지막으로 iOS 한쪽 상태를 실측해 candidate를 확정하거나 설계로 되돌린다.

## 012.12 예상 결과와 실패 시 되돌아갈 지점

- 예상: case 자산 실패는 static fallback으로 격리되고 pair motion은 유지된다.
- 예상: L/R/C 존재 조합마다 artwork 수와 label이 결정적이며 같은 입력은 같은 결정을 낸다.
- 예상: closed case는 8방향에서 seam이 몸체와 연속이고 lid 내부가 노출되지 않는다.
- 실패: side profile만 어긋나면 렌더 각도가 아니라 lid transform/정본 모델 단계로 돌아간다.
- 실패: 폰에서 속도가 끊기면 모델을 다시 만들지 않고 codec/frame size/runtime decode를 조사한다.
- 실패: 실제 iOS single 상태가 pair라면 single candidate를 폐기하고 pair policy로 정정한다.
- 실패: 새 animation이 오디오나 연결에 영향을 주면 UI와 무관하다고 가정하지 않고 service 수명주기를 추적한다.

## 012.13 느낌과 경계

정면이 그럴듯했을 때 이미 끝난 것처럼 느낀 손이 이번 오류의 핵심이었다. 파일 수치까지 통과하자
그 완료감이 더 강해졌지만, 제품은 돌아가는 동안 무너졌다. 이번 설계에서는 그 부끄러운 지점을
숨기지 않고 ‘완료처럼 보이는 정면’을 오히려 반례로 둔다. 사용자가 직접 이상함을 발견해야만 다음
각도를 보는 흐름을 반복하고 싶지 않다. 동시에 완벽한 iOS 복제를 서두르며 아직 관측하지 않은
한쪽 상태를 지어내지도 않겠다. 확인된 것은 단단히 만들고, 모르는 것은 다음 실측이 들어올 자리를
남겨 두는 쪽이 지금 가장 정직하고 수정 가능한 설계라고 생각한다.

## 012.14 구현 중 발견을 설계에 반영 — 2026-09-07

첫 geometry 자동 정렬은 lid 전체 최저점과 body 최고점을 맞췄지만, lid 내부/hinge mesh가 외피보다
낮아 눈에 보이는 seam에는 큰 공백이 생겼다. bounds 접촉만으로 시각 접촉을 증명할 수 없다는
반례다. 정면에서 확인된 z 높이는 유지하고, 측면 부유를 만든 y 중심 4.7mm만 body 중심에 맞춘
두 번째 후보는 8방향 형상 gate를 통과했다. 따라서 012.5의 ‘처음부터 닫힌 정본’은 반드시 별도
파일을 구한다는 뜻이 아니라, rigid lid mesh를 전 방향에서 닫힌 정본으로 검증하는 경우도 포함한다.

Galaxy 실제 카드에서 8.013초/248 화면 프레임을 녹화했다. 2fps 16표본 contact sheet에서 pair는
한 쌍으로 함께 돌고 case는 닫힌 silhouette을 유지했으며 lid 부유가 재현되지 않았다. 이는 시간축
전체의 pixel-perfect 증명은 아니지만, 이전 실패를 발견한 동일한 다각도 샘플보다 강한 실기기
근거다. 실패 자산은 APK에서 제거하고 설계 자산 폴더로 옮겨 회귀 비교에는 남겼다.

single-bud iOS 상태와 animation scale 0 실기기 표면은 여전히 미검증이다. 이 둘을 완료로 바꾸지
않으며, 현재 운영판은 한쪽 값에서도 검증된 pair fallback을 유지한다.

## 012.15 사용자 반증 뒤 이미지 유사성 gate 추가 — 2026-09-07

### 012.15.1 시나리오 S-023 — 형상은 깨지지 않지만 이미지가 여전히 이상해 보인다

1. 사용자가 실제 카드의 제품 이미지가 애매하다고 말하면 앞선 `형상 통과`를 미감 통과로 확대하지 않는다.
2. 실제 iPhone 촬영, Apple 공식 제품/연결 카드, 현재 Galaxy 녹화를 각각 원래 출처와 한계와 함께 둔다.
3. 제품만 잘라 비교할 때는 투명 배경, 물체 높이, 각도를 맞춘다. 정규화하지 않은 확대 이미지는 폐기한다.
4. 정면 한 장이 아니라 0/45/90/135/180/225/270/315도에서 lid/body 외곽과 seam을 좌우로 본다.
5. 차이가 재질인지 형상인지 먼저 분리하고, 형상 차이면 조명 후보를 만들기 전에 mesh transform으로 돌아간다.
6. 수정 뒤 같은 8방향, encoded WebP, Galaxy 한 회전을 다시 검사한다.

### 012.15.2 비교 근거의 등급과 사용할 수 없는 추론

- 1등급: 사용자가 직접 재현한 iOS 상태와 실제 iPhone 촬영. 공동 pair, 닫힌 case, 두 제품 회전 여부를
  확인하는 데 쓴다. 촬영 영상의 노출·압축 픽셀을 절대 색상값으로 쓰지 않는다.
- 2등급: Apple Support 104989와 Apple 공식 제품 이미지. 재질의 highlight 범위와 제품 부품을
  확인하는 데 쓰지만, 연결 카드와 battery 카드의 서로 다른 상태를 섞지 않는다.
- 3등급: CC BY 4.0 polyman 원본. 운영 자산의 topology와 수정 가능 범위의 정본이다.
- 4등급: MaterialPods와 라이선스가 확인되지 않은 공개 Android 구현. 방향별 silhouette의 반례를 찾는
  참고일 뿐 iOS 정답이나 복사 가능한 자산으로 취급하지 않으며 APK에 포함하지 않는다.

### 012.15.3 v2 판정 철회와 v3 seam 정렬

처음 만든 비교판은 참고 제품을 지나치게 확대하고 우리 투명 PNG를 검은 배경에 합성해 공정하지 않았다.
이 판을 폐기한 뒤 물체 높이와 각도를 맞춘 8방향 비교에서 v2의 새 문제가 보였다. 90도 측면의 lid와
body mating row는 모두 92px 폭이었지만 중심이 각각 155.5px와 159.5px라 seam에서 4px 계단이 생겼다.
전체 shell bounds의 중심을 맞춘 이전 방식이 curved cap과 숨은 hinge geometry에 끌린 결과였다.

닫힘 각도는 34/37/40/43/46도에서 측면 lid가 떠 있음을 확인하고 100/105/110/115/120도를 다시
훑었다. 110도에서 seam 높이와 수평이 맞았다. bounds 중심 정렬 뒤 world Y에 -1mm를 더하자 90도
mating row가 같은 92px 구간에 겹쳤다. 별도의 lid depth scale은 필요하지 않아 1.0을 유지했다.
새 default를 110도/-1mm로 고정했으며, 옵션을 생략한 8방향 결과와 명시적 검증 후보의 RGBA RMSE는
모두 0이었다.

v3는 320×320 RGBA 180 unique frames, 정확히 6000ms, clipping 0이다. frame 179→0 RMSE
0.00745는 frame 0→1의 0.00649와 같은 2도 이동 규모이며, animation 첫 frame과 static fallback은
pixel-identical이다. SM-F971N의 8.018초·245-frame 녹화에서 16개 시간 표본을 확인했고 v2의 lateral
step은 재현되지 않았다.

### 012.15.4 재질과 pair에 대해 이번에 바꾸지 않은 것

실제 iOS battery 촬영은 제품 대비가 매우 낮지만 카메라 노출의 영향이 있고, Apple 공식 연결 카드는
더 강한 중간 회색 굴곡을 가진다. 제3자 두 구현도 서로 명암이 다르다. 따라서 이 자료만으로 white
material scale을 새 값으로 확정하지 않는다. 사용자가 이전에 직접 더 낫다고 판정한 Q 조명을 유지한다.
이어버드 joint motion도 iOS 직접 관측과 일치하므로 유지한다. 다만 legacy 512px static pair에는
animation에 없는 작은 alpha island가 있었으므로, 운영 animation의 깨끗한 320px 첫 frame으로
fallback만 교체했다. 정확한 iOS easing, 같은 상태 pair battery의 합침 임계값, single-bud artwork는
여전히 미확정이다.
