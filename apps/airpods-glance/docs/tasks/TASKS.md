# 에어팟 한눈에 — 구현 태스크와 판단 이력

## 19. “이미지가 애매하다”는 사용자 반증을 출처별 비교로 재현하고 수정한다

### 19.1 비교 자체가 만든 착시부터 제거한다

#### 19.1.1 기준과 현재 자산을 같은 물체 높이·배경·yaw로 정규화한다 — 완료

처음 만든 pair/case 비교판은 MaterialPods crop이 과도하게 확대됐고, 투명한 우리 PNG가 검정으로
보였다. 이 상태에서 case가 두껍다거나 pair가 벌어졌다고 말하면 비교 과정이 결론을 만든다. 그 판을
폐기하고 각 물체 alpha/white-background 경계를 측정해 동일 높이와 흰 배경으로 다시 합성한다.
정면뿐 아니라 6초 영상에서 45도 간격 8장을 추출해 기준 왼쪽·현재 오른쪽으로 놓는다.

정규화 결과 기본 case 가로세로 비율과 pair의 큰 silhouette은 유사했다. 따라서 모델 전체를 교체하는
태스크로 바로 가지 않았다. 반면 45/90도 case seam의 외곽 계단은 배경과 크기를 바꿔도 유지되어 실제
형상 문제로 분리됐다.

#### 19.1.2 실제 iOS, Apple 공식, 라이선스 있는 원본, 제3자 참고의 증거 권한을 분리한다 — 완료

실제 iPhone 촬영은 공동 pair와 closed case 및 회전을 확인하지만 노출된 RGB와 편집된 timing은
확정하지 않는다. Apple 공식 연결 카드는 재질 reference지만 battery surface 상태 정답은 아니다.
polyman CC BY 원본은 수정 가능한 topology 정본이다. MaterialPods와 라이선스 불명 GitHub 구현은
silhouette 반례만 찾고 자산은 복사하지 않는다. 이 순서를 설계 012.15에 남겨 ‘다른 앱과 같음’을
‘iOS와 같음’으로 요약하지 않는다.

### 19.2 frame 생성보다 앞에서 lid transform을 다시 푼다

#### 19.2.1 닫힘 각도를 측면 sweep으로 재검증한다 — 완료

renderer default 40도는 정면에서 두 shell이 겹쳐 닫힌 것처럼 보였지만 90도에서는 lid가 떠 있었다.
34/37/40/43/46도와 100/105/110/115/120도를 각각 렌더했다. 110도에서 seam의 높이와 수평이 맞고,
그보다 작으면 내부가 보이며 크면 lid가 body를 감싸는 반례를 확인했다. default를 110도로 고정한다.

#### 19.2.2 whole-bounds center가 아니라 visible seam center를 맞춘다 — 완료

v2 90도 alpha mask에서 lid와 body mating row 폭은 각각 92px로 이미 같았다. 그러나 구간 중심이
155.5px와 159.5px여서 4px lateral step이 있었다. lid depth scale 0.88/0.91/0.94/0.97/1.0을 먼저
검사했지만 폭 문제가 아니므로 scale은 1.0으로 복원했다. world Y offset -1.0/-0.5/0/+0.5/+1.0mm를
훑어 -1mm에서 두 92px row가 완전히 겹치는 것을 확인했다. 이 과정을 생략하고 ‘눈대중으로 조금 이동’
이라고 요약하면 같은 오류를 재현할 수 없으므로 측정값과 실패 sweep을 함께 보존한다.

#### 19.2.3 보정 default와 명시 후보의 8방향 픽셀을 비교한 뒤 full render한다 — 완료

옵션 없는 renderer가 110도/-1mm를 사용하게 하고, 명시적 후보와 8방향 RGBA RMSE 0인지 먼저 본다.
통과 뒤에만 case 180 frames를 만든다. 결과는 8/8 RMSE 0, 180 unique, 6000ms, clipping 0이었다.

### 19.3 운영 자산과 실제 카드에서 같은 오류가 사라졌는지 확인한다

#### 19.3.1 v2를 비교 이력으로 보존하고 v3와 동일한 static fallback을 연결한다 — 완료

v2 animation/static을 rejected-runtime-assets에 `seam-offset` 이름으로 복사한 뒤 운영 resource를
v3로 올린다. Java resource도 v3 이름을 참조해 파일 이름이 판정 이력을 숨기지 않게 한다. case와 pair
static은 각각 운영 WebP frame 0과 pixel-identical하게 만든다. pair의 이전 512px fallback에서 보인
작은 alpha island는 새 320px fallback에 없어졌다.

#### 19.3.2 core→APK→설치→8초 녹화 순서로 회귀한다 — 완료

262,649 assertions, APK v3 signature, permission policy를 먼저 통과시킨다. 선택 기기와 앱 data를
보존한 `install -r` 뒤 preview를 열고 type-2038 1개를 확인한다. 8.018초·245-frame 녹화를 2fps
16표본으로 펼쳐 front/oblique/side/rear에서 lid/body 외곽을 본다. v2 lateral step은 재현되지 않았고
카드는 사용자 직접 비교를 위해 열린 상태로 유지했다.

#### 19.3.3 조명·easing·battery 합침을 근거 없이 함께 수정하지 않는다 — 현재 이미지 완료·미실측 상태 대기

iPhone 촬영은 저대비지만 노출 영향이 있고 Apple 공식 연결 카드는 더 진하다. 사용자가 승인했던 Q 조명을
이번 geometry 수정과 섞지 않는다. 정확한 easing, equal-battery grouping threshold, single-bud 화면은
실측이 추가될 때 설계부터 갱신한다.

사용자가 설치된 v3 카드를 직접 보고 “이제 잘 나온다”고 확인했다. closed-case v3와 Q 조명은 사용자
시각 gate까지 완료한다. 미실측 세 항목은 이 성공에 묻어 완료로 바꾸지 않는다.

## 18. 상태별 제품 motion을 검증 gate 뒤에서 다시 구현한다

### 18.1 표시 상태의 결정을 Android UI 밖으로 분리한다

#### 18.1.1 L/R/C 존재와 자산 검증 여부의 64조합을 설명하는 pure policy를 만든다 — 완료

현재 ConnectionPopupOverlay는 case의 percent 유무만 검사하고 earbuds group은 항상 만든다.
이 구조에서는 모든 값이 unknown이어도 pair가 나타나며, case animation의 품질 실패를 코드가
구분할 방법이 없다. Boolean을 overlay에 더 붙이기 전에 ProductCompositionPolicy가 artwork 종류,
case 표시, 각 제품 motion 가능 여부를 결정하도록 한다. 모든 조합을 전수 검사하되 single-bud는
미검증 candidate라는 상태를 잃지 않는다. overlay 연결은 이 계약이 통과한 뒤다.

ProductCompositionPolicy를 Android 의존성 없이 만들고 64개 조합에서 artwork 종류, case 존재,
product 존재, pair/case motion gate, single candidate, reduced-motion 우선의 6~7개 불변 조건을
검사했다. 기존 검사를 합쳐 262649 assertions가 통과했다. placement는 입력에 넣지 않았다.

### 18.2 closed case의 형상을 frame 생성보다 먼저 검증한다

#### 18.2.1 실패한 110도 lid 근사의 bounds를 측정하고 rigid transform 정렬 가능성을 판정한다 — 완료

첫 근사는 lid 회전만 했기 때문에 110도 상태의 lid y 중심이 body와 약 4.7mm 어긋나고,
lid 최저점은 body 상단보다 약 6.9mm 아래로 관통했다. 이 수치는 contact sheet의 부유/관통과
일치한다. 같은 원본 lid mesh를 rigid rotation 뒤 body y 중심과 seam z에 맞춰 translation한
후 8방향을 다시 본다. 어느 방향에서라도 내부 노출·단차가 남으면 각도 조정을 반복하지 않고
처음부터 닫힌 정본 mesh를 찾는 태스크로 전환한다.

첫 자동 정렬은 lid 전체 bounds 최저점을 seam으로 맞췄다. 그러나 그 최저점은 보이는 외피가 아니라
내부/hinge mesh에도 영향을 받아 8방향 모두 큰 수직 틈이 생겼다. 숫자상 접촉이 시각 seam 접촉을
뜻하지 않는 반례다. 이 결과를 버리고, 이미 정면에서 맞았던 높이는 유지하면서 측면 부유의 원인인
약 4.7mm depth 중심만 정렬하는 두 번째 preview로 수정했다.

두 번째 preview의 0/45/90/135/180/225/270/315도를 직접 확인했다. lid와 body seam이
전 방향에서 이어지고, 앞선 후보처럼 공중에 뜨거나 내부 bowl이 노출되지 않았다. 후면 hinge와
측면 indicator도 회전 방향에 따라 일관된 위치로 나타났다. 이 후보를 형상 gate 통과로 판정한다.

#### 18.2.2 8방향 형상 gate를 통과한 경우에만 180프레임을 만든다 — 완료

0/45/90/135/180/225/270/315도에서 body-lid silhouette, seam, hinge와 포트 방향을 본다.
preview 실패 상태에서 full render를 시작하지 않는다. 통과하면 180프레임을 만들고, 기존 파일
검사에 첫 frame과 static fallback의 pixel 동일성도 추가한다.

depth-only 정렬 후보로 180프레임을 생성했다. 새 generic verifier가 320×320, 180 unique RGBA,
6000ms, alpha, clipping, loop seam을 검사했고 seam step 4.652는 내부 최대 19.147보다 작았다.
정적 fallback PNG가 decoded animation 첫 frame과 pixel 단위로 같은 것도 확인했다.

### 18.3 policy와 verified asset을 카드에 연결한다

#### 18.3.1 실패한 case motion은 static으로 유지하고 pair joint motion만 보존한다 — 완료

사용자의 지적 직후 운영 앱의 case animated resource를 closed static PNG로 바꿨다. pair는 기존
joint turntable을 유지했다. 262233 core assertions, APK 권한·서명 검사 뒤 Galaxy에 덮어 설치했다.
이는 최종 animation이 아니라 잘못된 motion을 노출하지 않는 복구 기준점이다.

#### 18.3.2 single-bud 실측 전에는 candidate를 운영 분기에 연결하지 않는다 — 대기

원본 A/B mesh가 분리된다는 기술 사실과 iOS가 한쪽 상태에서 어떤 artwork를 쓰는지는 다른 문제다.
policy는 candidate를 표현할 수 있어도 overlay는 현재 pair fallback을 유지한다. iOS 실측 결과가
들어오면 설계 012.3/012.10을 먼저 수정한 뒤 asset mapping을 연결한다.

#### 18.3.3 verified composition과 closed-case v2를 overlay에 연결한다 — 구현·실기기 검사 완료

overlay는 pure policy로 earbuds/case group 존재를 정한다. case-only 보고에서 더는 dash pair를
만들지 않는다. 한쪽 보고는 candidate를 운영 single 자산에 연결하지 않고 검증된 pair fallback을
유지한다. case는 8방향 gate를 통과한 v2 animation/static을 사용한다.

APK를 Galaxy에 설치하고 저장된 L/R 100%, case 70% fixture로 실제 미리보기 카드를 띄웠다.
8.013초 동안 248 화면 프레임을 녹화해 16개 시간 표본을 직접 확인했다. joint pair와 closed case가
한 바퀴 이상 이어졌고 이전 lid 부유는 보이지 않았다. 테스트 뒤 카드 본문을 눌러 overlay를 닫았으며
dumpsys에는 MainActivity만 남았다. animator scale은 1.0이었다.

### 18.4 결과 예측과 테스트 피드백을 같은 상태표로 되돌린다

#### 18.4.1 pure/asset/APK/실기기 결과를 단계별로 기록한다 — 현재 범위 완료·single 실측 대기

pure test 성공을 시각 성공이라 부르지 않고, WebP 검사 성공을 폰 playback 성공이라 부르지 않는다.
각 gate의 예상과 실제 차이는 결과 문서에 기록하고 반례가 바꾼 설계 문장과 태스크를 링크한다.
사람이 실제 제품을 움직여야 하는 single 상태와 화면 녹화만 남으면 그 사실을 완료와 섞지 않는다.

결과 문서에 pure policy, 두 geometry 실패/수정, WebP, Galaxy playback, 미검증 항목을 분리했다.
실패 WebP 5개는 삭제하지 않고 `design-assets/3d/rejected-runtime-assets`로 옮겨 APK에서만 제외했다.

## 17. iOS 실기기 관측으로 애니메이션 가설을 정정한다 — 2026-09-07

### 17.1 관측된 케이스-open 표면을 현재 기준으로 삼는다

#### 17.1.1 독립 회전 적용을 되돌리고 기존 pair 회전을 복원한다 — 구현 완료·재빌드 대기

전날 영상 프레임에서는 pair가 함께 도는 것을 확인했지만, 사용자가 원하는 별도 UX라는 이유로
독립 회전을 적용했다. 오늘 사용자가 iOS 실기기에서 케이스만 여는 정확한 시나리오를 재현해
pair 공동 회전과 닫힌 case 표시를 확인했다. iOS 기준을 따르기로 한 제품 목표상 독립 적용은
잘못이었다. ConnectionPopupOverlay를 기존 검증된 `airpods_pair_turntable_3d`로 복원한다.
독립 WebP와 렌더 도구는 삭제하지 않아 판단과 비교 자산을 보존한다.

#### 17.1.2 원본 mesh의 lid 그룹을 닫아 cardinal 후보를 검증한다 — 진행 중

case mesh에서 lid로 식별된 `uzpdkgqkIIWTYxJ`를 복제해 hinge 후보를 기준으로 회전한다.
각도 후보의 정면/측면에서 틈·관통·비정상 중심 이동을 확인한 뒤 닫힌 case 180프레임을 만든다.
케이스가 닫혔다는 외관만 관측됐고 case 자체의 회전 여부는 아직 별도 사실로 확정하지 않는다.

각도 32/36/40/44/48도는 내부가 보여 전부 거부했다. 70/90/110/130/150도를 다시 비교했고,
110도에서 정면 seam이 닫히고 90/180/270도에도 lid 방향과 외형이 유지됐다. 130도 이상은
뚜껑이 몸체를 감싸며 위치가 어긋나 거부했다. 110도로 180프레임을 렌더했다.

#### 17.1.3 공동 pair와 닫힌 case를 운영 카드에 묶어 회귀 검사한다 — 구현·검사 완료

닫힌 case WebP는 320×320, 180 unique RGBA frames, 6000ms이며 클리핑이 없다. 루프 경계
차이 6.034는 내부 최대 step 30.743보다 작았다. 정적 fallback도 같은 닫힌 case frame 0으로
교체한다. 기존 open/independent 자산은 삭제하지 않는다. pure core/APK/설치 결과는 결과 문서에
기록한다. case 자체 회전이 iOS와 완전히 같은지는 관측 근거가 부족하므로 후속 시각 확인 항목이다.


## 16. 실물 없이 가능한 자산 작업을 이어간다

### 16.1 원본 계층과 피벗을 분리한 뒤 시각적으로 검증한다

#### 16.1.1 두 하위 EMPTY를 독립 제품으로 만들고 공전이 사라지는지 검증한다 — 완료

원본 import-hierarchy의 DprZyuuKYVGeqRc/RTZiZFLcZlxaClC는 각각 완전한 mesh 집합이다.
각 그룹 bounds 중심을 별도 pivot으로 저장하고 0/45/90/180/270/360도에서 중심이
1e-6 이내로 고정되는지 검사했다. 이전 pair 경로는 유지하고 --variants로만 바꾼다.
이 A/B 그룹명은 AAP primary/secondary나 해부학적 좌우의 증거가 아니다.

#### 16.1.2 케이스 네 변형의 카메라와 배율을 공유하고 cardinal을 직접 본다 — 완료

낮은 상세 stand-in을 제외한 case-root에 수납 copy를 0/1/2개 조합했다. 기존 승인된
조명·재질·수납 높이 -0.028은 보존했다. 케이스 중심과 배율은 양쪽 수납 기준으로 통일했다.
7개 제품×4각도 28 PNG를 생성했다. 빈 케이스 정면/측면/후면과 한쪽 수납, 독립 pair의
네 각도를 직접 확인했다. 빈 케이스 정면에서는 낮은 카메라 때문에 내부 홈이 보이지 않으므로
홈 형상의 완전성을 이 검사로 증명하지 않는다. 운영 화면 자동 상태 선택은 아직 보류한다.

#### 16.1.3 독립 회전 180프레임을 생성하고 루프 자산으로 변환한다 — 완료

cardinal에서 두 이어버드가 나란히 유지되고 90도에도 서로를 가리지 않는 것을 확인한 뒤
full render를 시작했다. frame/180의 각도로 360도 중복 프레임 없이 6초 루프를 만든다.
완성 뒤 프레임 수/크기/알파와 루프 경계를 확인하고 결과를 기록한다.

결과: img2webp에서 33/33/34ms를 반복해 정확히 6000ms를 만들었다. 새 WebP는 2660560 bytes다.
기본 Python에 Pillow가 없어 검사 시작이 실패했다. 프로젝트 build 아래 격리된 venv에 Pillow를
설치해 재실행했으며 180개 고유 RGBA 프레임, 알파, 무클리핑, 총 시간, 루프 경계 검사가 통과했다.
ConnectionPopupOverlay의 pair drawable만 새 자산으로 바꿨다. 배터리·수납 신호·케이스 선택은
변경하지 않았다. 기존 pure tests 262233 assertions와 APK 권한·서명 검사도 통과했다.


## 15. 수납 신호 검증 — 2026-09-06, 사용자 없이 가능한 범위

### 15.1 그림보다 판독 계약을 먼저 확정한다

#### 15.1.1 공개 문서의 slot 의미를 읽고 좌우 고정 가설을 반증한다 — 완료

LibrePods commit 53679cc의 AAP Definitions에서 opcode 06의 primary/secondary 전환 주석을
확인했다. 배터리 opcode 04의 좌우 실측을 그대로 재사용할 수 없으므로, 설계 010에 두 근거의
경계를 기록했다. 충전 여부로 수납을 만드는 편법은 완충·케이스 방전 반례 때문에 채택하지 않는다.

#### 15.1.2 8바이트 보고만 해석하고 모든 미상 코드가 UNKNOWN으로 남는지 검사한다 — 완료

AapPlacementDecoder는 Android 의존성 없이 header/opcode/길이를 먼저 검사한다. 두 slot의
256×256 조합을 테스트해 코드 0/1/2와 unknown이 섞여도 순서를 보존하는지 확인한다.
다른 opcode·잘린 보고·확장 길이는 거부한다. 진단 키는 원래 바이트를 그대로 넣지 않고
0/1/2/unknown 네 종류로 제한한다. 기존 테스트와 합쳐 262233 assertions 통과했으며,
대부분은 전수 조합 반복 검사다. 물리 시나리오 26만 개를 통과했다는 뜻이 아니다.

#### 15.1.3 기존 read loop에 수동적 진단만 연결하고 전송 정책을 유지한다 — 구현 완료

새 Listener나 UI 상태 저장소 대신 기존 onStage를 사용한다. handshake 완료 이후에만
보고 수/slot 조합/unknown/길이 거부를 집계한다. 원본 패킷과 시각은 저장하지 않는다.
새 명령·재시도·추가 연결은 없고 30초 창은 그대로다. 누적값은 현재 수납 상태로 사용하지 않는다.
APK 빌드·권한·서명·설치 결과는 결과 문서에 남긴다.

#### 15.1.4 실물 대응이 확인된 뒤에만 상태별 케이스와 좌우 독립 회전을 연결한다 — 대기

사용자가 없는 동안 이어버드를 물리적으로 꺼냈다고 가정하지 않는다. 설계 010.4 순서대로
실물 상태와 카운터 차이를 비교하되, 누적값은 순서를 증명하지 못하므로 각 단계마다 별도
관측이 필요하다. primary 역할이 뒤집힌 경우도 재현한다. 좌우를 못 정하면 한쪽 수납 그림을
추정해서 만들지 않는다. 30초 이후 갱신 공백과 세션 신선도 모델을 먼저 해결한 뒤 UI에 연결한다.


태스크 번호는 작업을 작게 보이게 만들기 위한 장식이 아니다. 제목만 읽고 재현할 수 없는
판단은 본문에 전제, 관찰, 실패 조건, 산출물을 함께 남긴다. 구현 순서는 번호가 아니라
의존성 파급력을 따른다.

## 1. 배터리 데이터 계약과 해석기

### 1.1 공개 BLE 프레임을 거짓 양성 없이 해석한다

#### 1.1.1 Apple manufacturer data의 type·declared length·prefix를 모두 검증하는 순수 Java decoder를 만든다 — 구현·JVM 검증 완료

처음에는 `07 19`만 검사하려 했지만 CAPod의 최근 회귀에서 같은 type의 비표준 frame이
표준 offset의 쓰레기 값을 만들 수 있음을 확인했다. prefix `01`과 최소 길이를 함께
검증하고, 실패 원인을 enum으로 반환해 실기기에서 “패킷이 없음”과 “패킷을 거부함”을
구분한다. Android 클래스가 들어가면 JVM 단위 테스트가 어려워지므로 byte array만 받는다.

#### 1.1.2 battery nibble 0~10과 unknown/invalid를 분리하고 허위 100% clamp를 금지한다 — 구현·JVM 검증 완료

Podsify와 일부 구현은 11~14를 100으로 보정한다. 충전 판단에 직접 영향을 주는 숫자이므로
우리 앱은 11~14를 unknown으로 둔다. left/right/case가 개별적으로 unknown일 수 있게
모델을 nullable로 설계한다.

#### 1.1.3 CAPod·Podsify 일치 가설의 좌우 flip과 charging flag를 구현하되 실기기 검증 전 상태임을 테스트 이름과 문서에 남긴다 — 구현 완료·실기기 판정 대기

GreenPods와 충돌하므로 구현 자체를 사실 확정으로 기록하지 않는다. bit 5의 두 상태,
왼쪽/오른쪽만 charging인 합성 packet을 모두 테스트하고, 실측이 뒤집히면 테스트 fixture와
설계 ADR을 함께 정정한다.

구현 기록(2026-09-04): 첫 JVM 실행에서 unknown fixture를 `0xFA`로 만들어 놓고 bit 5가
set인 경우 low nibble이 왼쪽이라는 바로 앞의 가설과 모순되게 “왼쪽 15”라고 기대했다.
decoder를 테스트에 맞추지 않고 fixture를 `0xFE`로 정정했다. 이 실패는 좌우 규칙을 byte
수준에서 물리 시험과 대조해야 하는 이유를 재확인했다.

## 2. 상태 저장과 표현 계약

### 2.1 앱·위젯·알림이 같은 snapshot을 읽게 한다

#### 2.1.1 연결·identity confidence·세 component·관측 시각을 한 단위로 저장하는 `AirPodsSnapshot`과 store를 만든다 — 구현 완료·실측 snapshot 대기

필드별 SharedPreferences write는 widget이 left는 새 값, right는 옛 값을 읽게 할 수 있다.
단일 직렬화 문자열 또는 단일 commit 경계를 사용하고, 파싱 실패 시 0%를 만들지 않는다.

#### 2.1.2 fresh/live/stale 표현 정책을 순수 함수로 만든다 — 구현·JVM 검증 완료

charging bit를 캐시에 저장하더라도 disconnected 또는 오래된 상태에서는 현재 충전
아이콘을 숨긴다. 앱, 알림, widget이 서로 다른 freshness 기준을 갖지 않게 공통 정책을
둔다.

#### 2.1.3 low-battery 알림 상태 머신을 component별로 만든다 — 구현·JVM 검증 완료

같은 20% advertisement가 수십 번 들어와도 한 번만 알려야 한다. 임계값 아래 진입,
회복, 충전, 새 연결 세션의 전이를 테스트로 고정한다.

## 3. 선택 기기와 연결 수명주기

### 3.1 주변 AirPods와 사용자의 AirPods를 섞지 않는다

#### 3.1.1 페어링된 AirPods 후보를 읽고 한 대일 때 자동 선택하는 resolver를 만든다 — SM-F971N 확인 완료

현재 기기는 후보가 한 대지만 이름 문자열만으로 프로토콜 세대를 단정하지 않는다. 주소는
private preference에만 저장하고 UI와 로그에 출력하지 않는다.

#### 3.1.2 선택 기기의 ACL connect/disconnect만 service 수명주기로 전달하는 receiver를 만든다 — 구현 완료·실제 재연결 시험 대기

AirPods는 HFP와 A2DP가 순차 연결될 수 있지만 ACL은 물리 연결 사건으로 한 번만 취급한다.
중복 event에서도 popup session token이 바뀌지 않아야 한다.

#### 3.1.3 scan result 주소 일치를 우선하고 불일치 후보를 자동 확정하지 않는 identity policy를 만든다 — 구현 완료·주소 resolution 실측 대기

주소 불일치만 들어오는 경우가 Android의 정상 address resolution일 가능성도 있으므로
packet 자체를 버린 횟수와 RSSI/model만 진단에 남긴다. 첫 실기기 시험 결과에 따라 근접
교정 단계를 설계로 되돌린다.

## 4. 연결 중 감시 서비스

### 4.1 필요한 시간에만 BLE를 관찰한다

#### 4.1.1 `connectedDevice` foreground service와 Apple manufacturer scan filter를 연결한다 — 구현·미연결 timeout 검증 완료

연결 전 상시 service를 만들지 않는다. service 시작 직후 낮은 중요도의 `배터리 확인 중`
알림을 올리고, Bluetooth off·disconnect·permission revoke·destroy 모든 경로에서 scanner를
stop한다.

#### 4.1.2 decoder와 identity policy를 통과한 snapshot만 store에 반영하고 의미 변화 때만 표면을 갱신한다 — 구현 완료·실측 frame 대기

광고 packet은 짧은 간격으로 반복된다. 매 packet마다 RemoteViews와 notification을 다시
그리지 않고 battery/charging/connection 변화 또는 freshness 최소 간격을 기준으로 한다.

#### 4.1.3 `neverForLocation`이 SM-F971N에서 Apple frame을 누락하는지 진단 카운터로 확인한다 — 진단 구현 완료·AirPods 연결 시험 대기

공식 문서는 일부 beacon filtering 가능성을 명시한다. 0건일 때 곧장 위치 권한을 추가하지
않고 raw Apple frame 수, filter match 수, address match 수를 분리해 다음 실험을 결정한다.

첫 연결 기록(2026-09-04): 서비스 자동 시작은 확인됐지만 엄격한 `07/19/01` scan filter
앞에서는 후보가 0이었다. 이 filter로는 위 문단이 요구한 raw Apple과 decoder rejection을
분리할 수 없다는 구현 불일치를 발견했다. Apple company ID만 filter하고 type·length·prefix는
decoder에서 거부하도록 수정했다. 위치 권한은 추가하지 않았다.

## 5. 홈 화면 위젯과 알림 표면

### 5.1 현재 버전에서 배터리를 다시 볼 수 있게 한다

#### 5.1.1 4×1 기본·2×1 축소 가능한 `RemoteViews` 위젯을 만든다 — 4×1 실기기 렌더 검증 완료·축소 대기

좌·우·케이스와 freshness가 핵심이고 장식은 후순위다. unknown을 `—`로, disconnected를
마지막 확인 시각과 함께 표시한다. Fold 외부 화면에서 잘림을 확인한다.

#### 5.1.2 store commit 직후 모든 widget id를 갱신하고 재부팅 후 cached snapshot을 복원한다 — 구현 완료·실측 snapshot/재부팅 대기

정기 30분 update에 의존하면 연결 직후 위젯이 낡는다. service가 명시적으로 update를
보내고 provider는 빠르게 render만 한다.

#### 5.1.3 위젯 새로고침과 앱 열기 PendingIntent를 충돌 없이 배치한다 — 배치·공식 추가 완료·클릭 회귀 대기

전체 widget 클릭과 refresh 버튼 intent가 겹치지 않게 request code와 action을 분리한다.
미연결 새로고침은 스캐너를 장시간 켜지 않고 앱의 상태 설명으로 이동한다.

#### 5.1.4 연결 중 ongoing 알림, 세션당 한 번 popup, 저전력 알림 channel을 분리한다 — channel/ongoing timeout 검증 완료·실연결 popup 대기

foreground 알림을 popup 중요도로 만들면 지속적으로 방해가 된다. 세 역할을 channel로
분리하고 DND를 우회하지 않는다. 연결 popup과 low-battery가 동시에 발생하면 하나만
눈에 띄게 한다.

## 6. 온보딩과 진단

### 6.1 실패 이유를 사용자가 해결 가능한 언어로 보여 준다

#### 6.1.1 Bluetooth scan/connect와 notification 권한을 순서대로 요청하고 각 이유를 먼저 설명한다 — 화면·권한 부여 검증 완료

첫 화면에서 세 권한을 한꺼번에 던지지 않는다. 위치·마이크·접근성 권한이 필요 없다는
내용도 함께 보여 준다.

#### 6.1.2 선택 기기, 연결 상태, 유효/거부/주소 불일치 frame 카운트를 보여 주는 로컬 진단 영역을 만든다 — 구현·화면 검증 완료

배터리가 안 보일 때 `다시 해보세요`로 끝내지 않고 어느 단계에서 끊겼는지 구분한다.
주소와 원시 packet은 표시하거나 저장하지 않는다.

## 7. 결과 예측과 실기기 검증

### 7.1 설치 성공을 제품 성공으로 오해하지 않는다

#### 7.1.1 JVM decoder/state tests, APK 서명·권한·component 검사를 실행한다 — 완료

INTERNET, LOCATION, OVERLAY, ACCESSIBILITY가 요청되지 않음을 `dumpsys package`와 APK
manifest 양쪽에서 확인한다. 위젯 provider와 receiver exported 범위를 검사한다.

#### 7.1.2 SM-F971N에 설치하고 권한·위젯 picker·연결/해제 수명주기를 확인한다 — 설치·권한·picker 완료, 실연결 대기

사용자가 보고 있던 앱을 임의로 방해하지 않도록 설치 뒤 앱을 여는 시점을 알린다. 위젯
추가는 launcher 사용자 동작이 필요하면 그 한 단계만 요청한다.

#### 7.1.3 사용자의 물리적 한쪽 충전 조작으로 좌우와 charging을 확정한다 — 대기

오른쪽만 case, 왼쪽만 case를 각각 수행한다. 결과가 가설과 다르면 decoder만 뒤집고 끝내지
않고 ADR-006과 테스트 fixture, 결과 기록에 처음 가설과 반증 장면을 남긴다.

#### 7.1.4 연결 3회, 회의 모드 Bluetooth off, 수면 DND, MaterialPods 공존을 회귀 시험한다 — 대기

popup 중복, service 잔존, ongoing notification 잔존, widget stale 오표현을 확인한다.
모두 통과한 뒤 MaterialPods 제거 여부를 사용자에게 설명한다.

## 8. BLE 실측 반증 뒤 AAP 직접 battery 확장

### 8.1 더 정확한 source를 받아도 기존 상태 계약이 거짓말하지 않게 바꾼다

#### 8.1.1 `BatteryComponent`의 10% 배수 제약을 BLE decoder 내부로 옮기고 snapshot source를 직렬화한다 — 구현·JVM migration 검증 완료

처음 공통 모델에 10% 배수 제약을 둔 이유는 BLE 값의 정직성을 지키기 위해서였다. 실측 뒤
AAP 1% source가 필요해졌으므로 그 제약을 삭제만 하면 BLE invariant까지 잃는다. 공통 모델은
0~100을 받고 BLE decoder test는 여전히 10%만 내는지 보장한다. 기존 JSON에 source가 없는
설치 데이터는 `NONE` 또는 관측값의 옛 의미에 맞는 `BLE_PUBLIC_DECILE`로 migration한다.

#### 8.1.2 앱의 정밀도 문구와 widget freshness가 source를 공유하는지 확인한다 — 구현·빌드 검증 완료, 1% 실측 화면 대기

앱 화면에 항상 10%라고 쓰면 AAP 79%와 모순된다. 숫자 표시 함수는 그대로 쓰되 설명은
source별로 바꾼다. charging freshness는 source와 무관하게 connected+fresh gate를 유지한다.

### 8.2 socket보다 먼저 AAP bytes의 거부 규칙을 고정한다

#### 8.2.1 message type·service·command·entry count·5-byte 경계를 검증하는 순수 Java decoder를 만든다 — 구현·JVM 검증 완료

L2CAP read가 짧거나 battery가 아닌 command일 때 `null`과 0%를 혼동하지 않는다. rejection
enum으로 원인을 남기고 raw packet은 decoder 밖에서 보존하지 않는다.

#### 8.2.2 left/right/case와 1%·charging·disconnected 의미를 실제 캡처 fixture로 검증한다 — 공개 Pro 3 캡처 포함 JVM 검증 완료

wire type 02=right, 04=left, 08=case와 charging 01/05를 합성·공개 캡처 fixture 양쪽으로
시험한다. percent 127/255와 disconnected case 0은 unknown으로 처리해 가짜 0%를 막는다.

### 8.3 Android 17 공개 L2CAP 연결을 service 수명에 묶는다

#### 8.3.1 `BluetoothSocketSettings`를 reflection으로 호출하는 API 37 전용 adapter를 만든다 — 구현·실호출 완료, BR/EDR 미지원으로 제품 경로 폐기

compile SDK 36에서도 실행 기기 API 37의 공개 class를 사용할 수 있게 reflection을 쓴다.
hidden `createInsecureL2capSocket`이나 hidden-api exemption은 fallback으로 넣지 않는다.

#### 8.3.2 선택 bonded device에 PSM 0x1001 socket 하나만 열고 8초 watchdog·close 경로를 만든다 — non-SDK 통제 시험에서 socket 성공·ACL 단절, 자동 경로 제거

main thread를 막지 않고 중복 start가 task를 늘리지 않게 single executor와 socket lock을
사용한다. connect/read 중 disconnect가 오면 close로 즉시 깨우며 exception에는 주소를 넣지
않는다.

#### 8.3.3 handshake·notification enable 뒤 battery만 decoder에 전달하고 공통 snapshot을 갱신한다 — decoder 구현 완료, handshake/battery 실측 전 오디오 안전 조건 실패

1차 범위는 제어·개인키·serial 수집이 아니다. 다른 command는 숫자만 세고, battery만 source
`AAP_EXACT_PERCENT`로 저장한다. AAP success 뒤에도 BLE scan은 fallback으로 유지하지만 더
낮은 정밀도 snapshot이 AAP fresh 값을 덮지 않게 source 우선순위를 둔다.

구현 기록(2026-09-04): socket 연결 성공과 AAP handshake 성공은 서로 다른 실패 지점이다.
하나의 8초 timer로 합치면 “Bluetooth 링크 실패”와 “상대가 AAP에 응답하지 않음”을 구분할
수 없어서 connect 8초와 handshake 10초 watchdog을 분리했다. 서비스가 종료되면 먼저
client를 stop해 read를 깨운 뒤 main-handler callback을 제거한다. BLE 주소 불일치가 이미
성공한 AAP exact snapshot을 `NEEDS_CALIBRATION`으로 되돌리는 경로도 함께 차단했다.

### 8.4 물리 시험으로 AAP가 제품 경로인지 판정한다

#### 8.4.1 socket API→connect→handshake→battery 단계 카운터로 첫 실제 연결을 검증한다 — public builder 거부·hidden socket connect 성공·handshake/battery 0 확인

어느 단계에서 멈췄는지 하나의 `AAP 실패`로 요약하지 않는다. 공개 API 존재, socket 생성,
connect, handshake response, battery message를 각각 확인한다.

#### 8.4.2 왼쪽/오른쪽 한쪽 case와 전체 disconnect에서 숫자·charging·service 정리를 검증한다 — A2DP/ACL 비간섭 실패로 좌우 시험 전 중단

좌우를 물리적으로 바꿔 화면만 보는 것이 아니라 widget과 ongoing notification도 같은 값을
보는지 확인한다. 시험 중 A2DP가 끊기면 P-AAP-4 반증으로 즉시 자동 경로를 중단한다.

## 9. P-AAP-4 반증 원인 분리 재시험

### 9.1 실험이 일상 자동 경로로 새지 않게 한다

#### 9.1.1 ACL receiver start와 foreground UI의 1회 experiment start를 별도 extra로 분리한다 — 구현·실기기 1회 경로 검증 완료

이전 구현은 disconnect 뒤 ACL 자동 재연결에서도 service가 다시 AAP를 열어 두 번째 단절을
만들었다. 이번에는 UI에서 명시적으로 시작한 첫 service instance만 AAP를 열고, receiver가
만든 start intent는 BLE만 실행한다. 제목만의 `수동 테스트`가 아니라 어떤 intent가 socket
권한을 갖는지 재현할 수 있도록 extra와 call site를 함께 남긴다.

### 9.2 protocol 순서를 최소화한다

#### 9.2.1 connect 뒤 handshake response를 확인한 다음 enable packet을 개별 flush한다 — 구현·status 0 뒤 전송 실측 완료

첫 구현은 handshake, enable 두 개, InitExt, 근거가 부족한 battery request를 read loop 전에
연속 전송했다. socket 성공과 5초 뒤 ACL 종료 사이에서 어느 message가 원인인지 알 수 없었다.
재시험은 handshake-only gate를 두고 status 0 뒤에만 세 enable/init packet을 전송한다.

#### 9.2.2 명시적 battery request `0x0003`을 제거하고 push만 관찰한다 — 구현·98%/92% push 수신 완료

참고 production 경로는 notification enable 뒤 battery push를 사용하며 request를 보내지
않았다. enum 이름만 보고 request가 안전하다고 추론했던 결정을 철회한다. 이 시험에서는
제어·key·device info와 마찬가지로 request도 보내지 않는다.

### 9.3 오디오 안전을 battery 성공보다 먼저 판정한다

#### 9.3.1 socket→handshake→battery counter와 A2DP/ACL 20초 유지 여부를 함께 기록한다 — 3/3회·오른쪽 case 물리 시험 통과

battery를 한 번 받더라도 오디오가 끊기면 제품 성공이 아니다. 연결 전후 Bluetooth profile과
system socket event 시각을 대조하고, disconnect가 한 번이라도 재현되면 dependency를 제거한
안전 APK로 복귀한다.

첫 통제 결과(2026-09-04 18:18): socket 연결, handshake 응답, post-handshake 활성화,
battery 수신을 v2 전용 counter로 확인했다. 앱과 위젯은 왼쪽 98%, 오른쪽 92%, 케이스 `—`를
표시했다. t=1,2,3,4,5,6,8,10,12,15,20,25,30초 모든 표본에서 ACL/A2DP가 connected였고
event history에 새 단절이 없었다. 30초 experiment timeout은 AAP socket만 닫았으며 자동
receiver에는 experiment extra가 없어 재시도하지 않는다. 같은 연결에서 버튼만 반복하는 것은
독립 재연결 검증으로 세지 않는다.

두 번째 결과(2026-09-04 18:28): 첫 시험 뒤 AirPods가 실제로 remote-user disconnect되고 새
ACL/A2DP connection이 만들어진 것을 system history로 먼저 확인했다. AirPods A2DP로 음악이
재생되는 상태에서 같은 13개 시점을 표본화했고, 30초 내내 connection `CONNECTED`와 playback
`PLAYING`이 유지됐다. 새 snapshot은 왼쪽 95%, 오른쪽 92%, 케이스 `—`였고 v2 전용
experiment/handshake/activation/window counter가 각각 2가 됐다.

세 번째 결과(2026-09-04 18:32): 18:31:24 disconnect 뒤 18:32:02 새 ACL을 확인했다. 왼쪽을
착용하고 오른쪽을 열린 case 안에 둔 상태에서 음악을 재생했다. 30초의 같은 13개 시점 모두
connection `CONNECTED`, playback `PLAYING`이었다. 앱은 왼쪽 92%, 오른쪽 92% `충전 중`,
case 84%를 표시했고 위젯도 `왼쪽 92% · 오른쪽 92%⚡ · 케이스 84%`로 일치했다. v2 counter는
experiment/handshake/activation/window가 각각 3이 됐고 새 disconnect는 없었다.

#### 9.3.2 검증된 AAP를 연결당 한 번·30초 제한의 자동 경로로 승격한다 — 구현·설치·local/remote 재연결 실측 완료

검증 성공을 이유로 무제한 read socket이나 retry loop를 만들지 않는다. 선택된 AirPods의 새
ACL 연결에서만 한 번 시작하고, battery 수신 여부와 관계없이 30초에 socket을 닫는다. 실패한
같은 ACL session에서는 재시도하지 않으며 widget의 수동 새로고침도 자동 실험 extra를 만들지
않는다. 구현 뒤 실제 재연결 1회에서 사용자가 버튼을 누르지 않아도 값이 바뀌는지 확인한다.

구현 기록: session이 처음 열릴 때 `automatic_aap_attempted=false`를 저장하고 verified ACL start가
이를 원자적으로 claim한다. 중복 start나 service recreation에서는 claim이 거부된다. 사용자가
나중에 case를 열어 값을 다시 보고 싶은 명시적 앱 버튼은 manual extra로 분리했다. 두 경로
모두 같은 검증 protocol과 30초 close를 사용하며 자동 실패만 반복되지 않는다.

자동판 실측(2026-09-04 18:43): 사용자가 앱 버튼을 누르지 않고 Bluetooth 설정에서 AirPods를
연결했다. ACL 직후 우리 type 3 socket이 정확히 한 번 연결됐고 진단은 automatic 1, manual 0,
window close 1이었다. battery push로 위젯이 왼쪽 90%, 오른쪽 98%, case `—`로 바뀌었다.
연결 뒤 음악은 AirPods A2DP에서 계속 `started`였고 새 disconnect가 없었다.

## 10. AirPods가 Galaxy에 먼저 붙지 않는 선행 연결 UX

### 10.1 배터리 자동화와 Bluetooth profile 자동 연결을 분리한다

#### 10.1.1 Galaxy 저장 정책과 실제 connection initiator를 확인한다 — 1차 진단 완료

A2DP connection policy는 100(allowed)이고 AirPods는 active/recent/fallback으로 저장됐다.
그럼에도 착용 뒤 자동 attempt는 없었고 18:43:12 System UI에서 사용자가 선택했을 때만 local
connect가 시작됐다. 우리 receiver/AAP는 18:43:15 ACL 뒤에 실행되므로 원인이 아니다.

#### 10.1.2 다른 Apple host 경쟁을 배제한 case 재개방 시험을 한다 — Mac Bluetooth off 조건에서 2회 재현 완료

근처 iPhone·iPad·Mac Bluetooth를 잠시 끈 뒤 case를 닫았다 열고 Galaxy 설정을 누르지 않은 채
15초 기다린다. 이때 자동 연결되면 Apple automatic switching/last-host가 원인이다. 그래도
연결되지 않으면 재페어링 또는 선택 bonded device에 대한 보수적 connect helper를 별도 설계한다.
상시 Apple company-id scan은 주변 타인의 신호 오인과 배터리 비용이 있어 먼저 넣지 않는다.

실측 기록: iPhone/iPad Bluetooth가 꺼지고 Mac Bluetooth만 남은 조건에서 Galaxy 자동 연결이
없었다. Mac Bluetooth를 끄자 AirPods가 18:51:36과 18:52:04 두 번 Galaxy에 remote connection
request를 보냈고 ACL/A2DP가 자동 연결됐다. 각 새 session에서 우리 앱도 automatic direct
battery를 한 번씩 실행했다. 따라서 강제-connect helper를 구현하지 않고 Mac의 AirPods별
`이 Mac에 연결` 정책을 조정하는 것이 먼저다.

#### 10.1.3 Mac Bluetooth는 유지하고 AirPods의 자동 전환만 제한한다 — 완료

Mac의 AirPods 세부사항에서 `이 Mac에 연결`을 `이 Mac에 마지막으로 연결했을 때`로 바꾼다.
기기 지우기는 iCloud 기기 전체에 영향을 줄 수 있으므로 하지 않는다. Mac Bluetooth를 다시
켜 마우스·키보드를 복원한 뒤, Galaxy 화면을 누르지 않은 case 재개방 1회로 확인한다.

재시험 결과: Mac Bluetooth를 다시 켜고 AirPods별 자동 전환을 제한한 뒤 18:57:17 AirPods가
Galaxy에 remote-initiated ACL을 만들었다. A2DP와 우리 automatic battery가 이어졌고 왼쪽 87%,
오른쪽 94%, case `—`를 수신했다. 따라서 전체 Mac Bluetooth를 끄는 우회 없이 완료했다.

## 11. 큰 연결 카드

### 11.1 Android 표면 정책을 먼저 고정한다

#### 11.1.1 permission·interactive·keyguard·DND·reading을 입력으로 받는 순수 표시 정책을 만든다 — 구현 완료, DND 기대값은 14.1.1에서 정정

WindowManager 코드 안에 조건을 흩뜨리면 preview와 자동 연결이 다르게 행동한다. Android가
없는 boolean policy를 먼저 만들고 권한 없음, 화면 꺼짐, 잠금, DND, 값 없음 각각이 false인지
fixture로 고정한다. 사용자 preview만 DND를 우회할 수 있지만 잠금과 권한은 우회하지 않는다.

구현 기록: `ConnectionPopupPolicy`가 다섯 환경 입력과 preview 여부만 받아 판단하게 분리했다.
권한 없음, 화면 꺼짐, 잠금, DND, 값 없음, 정상 사용, 명시적 preview의 일곱 경우를 추가해 전체
JVM 54 assertions가 통과했다. WindowManager나 Samsung 상태에 기대지 않아 이후 표시 조건을
바꿔도 실제 창 코드와 독립적으로 회귀를 재현할 수 있다.

#### 11.1.2 시스템 알림 fallback과 low-battery 우선순위를 보존한다 — 구현·APK 정책 검사 완료

session popup claim은 surface 선택 전에 한 번만 수행한다. low-battery warning이 있으면 기존
경고가 claim하고, 없을 때 overlay를 먼저 시도한 뒤 실제로 표시할 수 없으면 connection
notification을 낸다. 어느 경우에도 두 표면이 동시에 뜨지 않게 한다.

구현 기록: 기존 session claim과 low-battery 선점 순서를 유지하고, 큰 카드가 실제로 add된 경우에만
성공으로 기록한다. 특별 권한 또는 환경 조건 때문에 add되지 않으면 기존 연결 알림을 호출한다.
따라서 권한이 없어도 자동 battery와 위젯은 바뀌지 않고, 동일 session의 이중 표면도 생기지 않는다.

### 11.2 overlay 수명과 접근성을 구현한다

#### 11.2.1 6초 자동 종료·닫기·앱 열기·service destroy 정리를 갖는 큰 카드를 만든다 — 구현·실기기 preview 완료

현재 display 폭에서 32dp를 뺀 상단 카드로 만들고 L/R/case를 한국어로 표시한다. 색 외에
`충전 중` 문자열을 제공하고, 닫기 target은 48dp 이상으로 둔다. static singleton 대신 service가
controller를 소유해 disconnect 때 WindowManager view를 제거할 수 있어야 한다.

실측 기록: SM-F971N에서 frame은 `[42,110]-[1206,648]`로 상태 표시줄 바로 아래에 놓였고,
왼쪽 87%·오른쪽 94%·case `—`가 잘리지 않았다. 6초 뒤 type 2038 window가 없어졌고, 다시
띄운 뒤 48dp 이상 닫기 target을 누른 시험에서도 즉시 없어졌다.

#### 11.2.2 특별 권한 안내와 현재 snapshot 미리보기를 앱에 추가한다 — 구현·권한 허용·미리보기 완료

앱은 `다른 앱 위에 표시` 설정 화면을 열고 현재 허용 상태를 표시한다. 권한 뒤 재연결을 강요하지
않도록 `큰 카드 미리보기` 버튼으로 저장 snapshot을 즉시 렌더한다. 권한을 거절해도 자동 battery,
widget, notification은 그대로여야 한다.

구현 기록: 앱에 권한 상태, Samsung의 `다른 앱 위에 표시` 설정 진입, 저장 snapshot 미리보기를
추가했다. 설치 뒤 `SYSTEM_ALERT_WINDOW: allow`를 확인했고, 설정 목록에서는 이 앱의 스위치만
켰다. 미리보기 screenshot에서 뒤 앱 화면이 보존되고 카드만 짧게 겹치는 것을 확인했다.

### 11.3 실기기 결과로 크기와 방해도를 판정한다

#### 11.3.1 SM-F971N에서 preview 시각 QA와 자동 연결 1회를 검증한다 — 완료

텍스트 잘림, fold display 폭, 6초 제거, 뒤 앱 touch 복원, overlay view 잔존을 확인한다. 이후
실제 AirPods 재연결에서 카드가 세션당 한 번이고 DND/잠금 fallback이 유지되는지 확인한다.
이 문장의 DND fallback 기대는 2026-09-05 실제 무표시로 반증됐고 14.1에서 대체한다. 잠금
fallback만 계속 유효하다.

preview에서 큰 카드의 폭·정보 계층·6초 제거·X 제거는 통과했다. 이 screenshot이 기존 앱 제목이
Android 17 status bar 아래로 들어가는 별도 문제도 드러내 root에 `systemBars()` inset을 반영했고,
재설치 뒤 제목 시작점이 y=168로 내려간 것을 확인했다.

실제 재연결 기록: 사용자가 양쪽을 case에 넣어 10초 닫았다가 다시 착용하자 19:18:11 Galaxy에
remote 연결됐다. 앱 service가 Bluetooth broadcast 허용 사유로 자동 시작됐고 AAP automatic
횟수는 4→5, widget은 왼쪽 86%·오른쪽 94%로 갱신됐다. WindowManager 로그에서 type 2038
view가 생성된 뒤 19:18:17 제거됐고 진단은 `크게 1 · 알림 대체 0`이었다. 따라서 앱을 누르지
않은 실제 경로의 세션당 한 번 큰 카드와 자동 제거까지 완료했다.

## 12. Apple 실제 화면 기반 시각 재설계

### 12.1 데이터와 그림 primitives를 먼저 만든다

#### 12.1.1 좌우 값을 합치지 않는 이어버드 묶음 presentation을 고정한다 — 구현·59 assertions 완료

Apple 촬영은 이어버드를 한 묶음·한 percentage로 보였지만 우리 실측은 왼쪽 86%, 오른쪽 94%다.
평균이나 최솟값으로 줄이면 사용자가 어느 쪽을 충전할지 판단할 근거를 잃는다. 이어버드 묶음은
두 side label을 유지하고, case는 component가 null일 때 묶음 자체를 제거한다. 구현자는 이
결정을 단순 “두 column”으로 요약하지 말고 서로 다른 protocol 결과를 보존하려는 이유를 함께 본다.

구현 기록: `ConnectionSheetPresentation`은 왼쪽/오른쪽 label을 각각 만들고, AAP가 case를
`BatteryComponent(null, null)`로 보내는 실제 형태도 숨긴다. 86/94가 분리되는지, unknown case는
사라지고 84% case는 나타나는지 다섯 assertion을 추가해 전체 59개가 통과했다.

#### 12.1.2 독립 이어버드·case art와 battery glyph를 만든다 — 구현·자산 QA 완료

공식 Apple bitmap을 복사하지 않고 로고 없는 투명 제품 render와 작은 battery glyph를 만든다.
제품 art는 density-independent 영역에서 FIT_CENTER로 보이고 percentage는 battery fill 너비에만
사용한다. charging은 초록색만 쓰지 않고 작은 bolt와 `충전 중` content description으로 남긴다.

구현 기록: 처음에는 Canvas path로 제품을 그렸으나 첫 screenshot에서 형태가 평면적이고 case가
과도하게 커 보였다. 이 피드백을 설계에 반영해 이미지 생성 기능으로 로고·문자·배경 없는 독립
이어버드/열린 case render를 만들고, 원본 alpha를 보존한 512px 동일 canvas에서 이어버드 450×360,
case 400×330 안에 정규화했다. Apple 공식 제품 bitmap은 APK에 포함하지 않았다.

### 12.2 renderer와 window motion을 교체한다

#### 12.2.1 남색 상단 card를 adaptive light/dark 하단 sheet로 바꾼다 — 구현·설치·light preview 완료

WindowManager의 permission·policy·claim은 유지한 채 gravity를 BOTTOM으로 옮기고 dim 0.30을
적용한다. 중앙 title, 48dp X, 제품 묶음, battery만 남기며 subtitle, 진한 세 칸 box, timeout
footer를 제거한다. case null/known 두 fixture를 같은 build path에서 렌더해야 한다.

구현 기록: surface를 light `#F2F2F7`, dark `#2C2C2E`로 분기하고 gravity BOTTOM, dim 0.30,
중앙 title, 우측 닫기, 제품 두 묶음으로 교체했다. 현재 case가 83%로 들어온 preview에서 이어버드와
case가 모두 나타났고, 그 직전 case unknown preview에서는 이어버드 하나만 중앙에 나타났다.

#### 12.2.2 아래에서 올라오고 아래로 사라지는 수명을 검증한다 — 구현·회귀 통과

등장은 translationY 48dp→0와 alpha 0→1을 280ms, 종료는 0→28dp와 alpha 1→0을 200ms로 한다.
기존 6초 handler, X, service destroy의 removeViewImmediate 계약은 바꾸지 않는다. 움직임을
바꾸다가 종료 callback이 두 번 실행되는 회귀가 없는지 type 2038 개수로 확인한다.

실측 기록: 280ms 아래→위 등장 뒤 6초 자동 종료에서 `apple_sheet_auto_dismiss_pass`, 다시
띄워 X를 누른 시험에서 `apple_sheet_close_pass`였다. 두 시험 뒤 package 소유 type 2038은 0이었다.

### 12.3 실기기 screenshot을 Apple 관측 구조와 비교한다

#### 12.3.1 저장 snapshot preview로 위치·계층·navigation inset을 확인한다 — 완료

SM-F971N screenshot에서 surface가 하단에 있고 뒤가 dim되며 title→제품→battery 순서인지 본다.
frame bounds, gesture handle 겹침, case unknown에서 빈 두 번째 묶음이 없는지 기록한다. “예뻐졌다”는
판정만 남기지 않고 Apple 실제 촬영과 달라진 부분이 Android 제약인지 우리 실수인지 구분한다.

실측 기록: 최종 frame은 `[31,1157]-[1216,1907]`이고 gesture navigation 영역은 y=1933부터라
26px 간격이 남았다. 뒤 화면 dim, title→두 product→battery 순서, 84/93/83 숫자와 X가 모두
잘리지 않았다. 첫 Canvas art가 평면적이고 case가 과도하게 커 보였던 결과는 독립 투명 render와
동일 canvas 정규화로 수정했다. 첫 battery label에서 percent가 잘린 문제도 glyph→percent→part의
세로 계층과 78dp 고정 폭으로 수정한 뒤 재촬영했다.

#### 12.3.2 사용자의 시각 피드백 뒤 실제 재연결 1회를 확인한다 — 대기

미리보기를 사용자가 보고 크기와 느낌을 판정한 뒤에만 실제 연결을 다시 시험한다. 새 ACL에서
renderer가 한 번 생성되고 6초 뒤 제거되며 AAP·위젯·오디오 수치가 기존과 같은지 확인한다.

### 12.4 검사 미리보기와 실제 자동 카드의 수명을 분리한다

#### 12.4.1 연결 없이도 마지막 snapshot을 충분히 살필 수 있게 미리보기를 X까지 유지한다 — 중간 구현 후 12.4.2로 대체

고정 좌표로 버튼을 누른 첫 시도는 실제 버튼을 빗나갔고, 다음 정확한 시도는 창이 떴지만 대화 화면에서
휴대폰으로 시선을 옮기는 동안 6초가 지나 사용자가 놓쳤다. 이 실패를 “다시 빨리 누르기”로 처리하면
검사자가 매번 타이밍을 맞춰야 한다. 현재 연결 여부는 원인이 아니며 저장 snapshot이 이미 있으므로,
명시적 preview는 자동 timer를 예약하지 않고 X 또는 activity 종료로만 제거한다. 자동 연결 경로는
기존 6초 timer를 반드시 유지한다.

구현 후 8초 대기 시 type 2038이 1개인지, X 뒤 0개인지, JVM policy에서 automatic=true와
preview=false가 각각 고정되는지 확인한다. 마지막으로 재설치가 Bluetooth/AAP 설정과 overlay 권한을
보존했는지도 확인한다.

#### 12.4.2 실제 자동 카드도 timeout 없이 본문 tap 또는 X까지 유지한다 — 구현·실기기 검증 완료

12.4.1을 설치한 직후 사용자가 “6초로 하지 말고 내가 누르거나 X를 눌렀을 때 없애면 안 되나”라고
범위를 바로잡았다. 미리보기만 지속시키는 중간 구현을 최종안으로 남기지 않는다. 자동·preview 모두
timeout을 예약하지 않으며 카드 본문 tap은 앱을 여는 부수 효과 없이 dismiss만 한다. 연결 해제 및
service destroy 정리는 별개이므로 유지해 stale 카드가 남지 않게 한다.

검증은 pure policy 61 assertions, APK signature/permission 검사, 설치 뒤 8초 지속, 본문 tap 제거,
다시 표시한 뒤 X 제거 순서로 한다. 이때 각 단계의 type 2038 window 수를 1→0→1→0으로 직접 읽어
보이는 듯했다는 판정과 실제 WindowManager 상태를 구분한다.

## 13. 제품 turntable 애니메이션

### 13.1 제품 시점 자산을 먼저 만든다

#### 13.1.1 이어버드와 열린 case의 16 yaw 시점을 테마별로 만든다 — 완료

기존 정적 render를 제품 identity 기준으로 사용한다. 전면에서 시작해 22.5도씩 증가하는 4×4 sheet를
이어버드와 열린 case에 각각 만들고, case의 뚜껑 각도·두 이어버드·초록 LED가 후면에서도 유지되는지
본다. 첫 이어버드 시트는 실제 alpha 대신 checkerboard 픽셀이 들어왔으므로 결과를 그대로 쓰지 않고
light `#F2F2F7`와 dark `#2C2C2E` 단색 시트를 별도로 재생성했다.

#### 13.1.2 16시점을 65-frame·약 6초 animated WebP 네 개로 묶는다 — 완료

각 핵심 각도 사이 세 프레임을 보간하고 frame delay 92ms, loop 0으로 인코딩한다. `webpmux -info`로
pair/case light/dark 모두 320×320, 65 frames, infinite loop인지 확인한다. 생성 원본은
`design-assets/generated-turntables`, APK 소비 자산은 `res/drawable-nodpi`에 둔다.

### 13.2 renderer 수명주기에 motion을 연결한다

#### 13.2.1 attach에서 시작하고 detach에서 정지하는 제품 view를 만든다 — 구현·64 assertions 통과

지속 overlay이므로 drawable을 생성 직후 무조건 시작하면 창이 제거된 뒤 callback이 남을 수 있다.
`ProductTurntableView`가 attach 이후 `Animatable.start()`, detach 직전 `stop()`을 소유한다. 전역
animator scale 0은 pure policy로 정적 fallback을 선택한다. 기본 1.0, 축소 0.5, 비활성 0의 세
경우를 추가했고 전체 64 assertions가 통과했다.

#### 13.2.2 기존 두 정적 ImageView만 교체하고 실기기 loop·dismiss를 본다 — 실기기 시각 반증, APK에서 철회

배터리, case visibility, overlay permission, DND, persistent dismiss 경로는 건드리지 않는다. light
preview에서 시간차 screenshot 또는 눈으로 전면/측면/후면 변화를 확인하고 본문 tap과 X 각각 뒤
type 2038 window 0을 확인한다. 어두운 테마는 별도 시각 반증이 남아 있음을 완료 조건에서 숨기지 않는다.

실측 기록: SM-F971N의 animator scale은 1.0이었다. 지속 preview를 띄운 뒤 0초, 1.5초, 3초,
5.7초에 화면을 캡처했고 전체 화면 기준 연속 표본 사이 93,016~103,749 pixels, 0초와 5.7초 사이
85,079 pixels가 달랐다. contact sheet에서도 이어버드와 case가 전면·측면·후면으로 달라졌고 제목과
battery 영역은 고정됐다. overlay는 5.7초 뒤에도 type 2038 1개로 유지됐다. 사용자가 현재 motion의
속도와 형태를 직접 보는 중이므로 시각 만족과 본문/X dismiss는 아직 완료로 닫지 않는다.

후속 시각 판정: 8.15초 실기기 screenrecord를 0.5초 간격으로 펼쳐 보자 이어버드가 늘어나고 개수가
달라지며, case 뚜껑과 내부가 회전 각도 사이에서 변형됐다. `changed pixels`는 재생 여부만 증명했지
자연스러운 회전을 증명하지 못했다. 원인은 한 3D 물체를 렌더한 것이 아니라 이미지 생성기가 각 셀의
제품을 다시 해석했고, `-morph`가 서로 다른 형상 사이를 겹쳐 보간했기 때문이다. 네 WebP는 APK에서
즉시 철회하고 rejected 자료로 보존한다.

### 13.3 단일 3D 모델을 회전 렌더한다

#### 13.3.1 사용할 수 있는 3D 모델과 라이선스·렌더 경로를 확인한다 — 완료

동일 mesh를 카메라·조명 고정 상태에서 회전시켜야 topology, 이어버드 개수, case lid angle이 프레임
전체에서 보존된다. 실시간 Android 3D 엔진보다 오프라인 360도 렌더를 우선한다. 카드가 열린 동안
무한 반복하는 작은 bitmap animation에는 이쪽이 decoder·배터리·APK 복잡도 면에서 유리하다.

확인 기록: polyman의 CC BY 4.0 glTF 원본과 license를 함께 보존했다. 직접 만든 단순 parametric model은
형태가 만화처럼 보여 제품 카드 품질 기준을 통과하지 못했고 APK에 넣지 않았다. licensed model의 네
각도 렌더에서 같은 topology가 유지됨을 확인했다. 첫 import 구현은 상위 0.01 scale을 잃어 100배 확대와
중심 이동을 만들었으므로 re-parent 방식을 철회하고 world-matrix yaw로 고쳤다. case 단독 렌더에서 검은
저해상도 stand-in이 드러난 문제는 해당 mesh를 숨기고 같은 상세 bud hierarchy를 case well에 배치하는
방식으로 수정했다. 이 조정은 다른 모델을 합성한 것이 아니라 같은 원본 내부의 상세 mesh를 재사용한다.

#### 13.3.2 180개 frame을 같은 mesh에서 렌더하고 약 6초 transparent WebP로 인코딩한다 — 완료

0°를 포함하고 360° 중복 frame은 포함하지 않는다. 320×320 RGBA, frame당 33ms, infinite loop로 pair와
case를 각각 만든다. 파일 정보뿐 아니라 0°·90°·180°·270°와 loop 경계의 실제 화면을 펼쳐 부품 수,
뚜껑 각도, 그림 위치가 바뀌지 않는지 본다. frame 수가 많다는 사실을 자연스러움의 대리값으로 삼지 않는다.

결과 기록: pair 736KiB, case 885KiB이며 둘 다 320×320, alpha, 180 frames, 33ms, loop 0이다.
6초 합성 preview를 0.5초 간격으로 펼쳐 같은 mesh의 가림 관계가 연속되는 것을 직접 확인했다. frame
179→0 RMSE는 pair 3780.91, case 3020.87로 각자의 평상시 0→1 이동 3788.83, 3057.82와 비슷해
loop 경계만 큰 폭으로 튀지 않았다. 이 수치는 시각 검수를 대신하지 않고 경계 검사만 보조한다.

#### 13.3.3 검증된 WebP만 overlay view에 연결하고 한 회전을 실기기에서 직접 본다 — 완료

기존 `ProductTurntableView`의 attach/start, detach/stop, animator-scale 0 fallback을 재사용한다. 배터리,
case unknown visibility, 본문/X dismiss에는 손대지 않는다. 설치 뒤 8초 이상 screenrecord해 0.5초 간격
contact sheet와 실제 영상을 보고, morph·순간이동·loop 되감김이 없을 때만 사용자에게 카드를 열어 둔다.

실기기 결과: SM-F971N 화면 녹화는 8.006초·242 frames로 약 30fps였다. 16-frame contact sheet에서
pair와 case만 같은 속도로 앞→옆→뒤→옆으로 변하고 제목·배터리는 고정됐다. 첫 녹화는 카드 표시 전에
버튼 tap이 들어가지 않아 앱 본문만 남았고, 창 개수만으로 성공 판정하지 않고 카드를 먼저 연 뒤 다시
녹화했다. 본문 tap과 X는 각각 type 2038 창을 1→0으로 만들었고, 마지막에는 preview를 다시 열어 뒀다.

### 13.4 형상 검증과 조명 검증을 분리한다

#### 13.4.1 사용자 판정에서 드러난 검은 외곽 음영을 아래 보조광으로 수정한다 — 완료

같은 mesh가 자연스럽게 회전한다는 13.3의 결론은 형상 문제만 해결했다. 실제 카드에서 흰 플라스틱
아랫면이 검정으로 눌렸는데도 모션 contact sheet에 집중해 조명 문제를 완료 조건에서 빠뜨렸다. 전체
노출을 먼저 올린 v1은 형상을 하얗게 날렸고, lower fill 10의 v2도 평면적으로 보여 둘 다 설치 전에
기각했다. 원래 material/exposure/세 주광을 되돌린 뒤 energy 2의 넓은 lower fill만 더한 v3에서 외부
곡면이 연회색으로 남았다.

v3로 pair/case 각 180장을 새 경로에 렌더하고 8개 중간 각도를 직접 본다. 320×320·alpha·180 frames·
33ms·loop 0을 확인한 뒤에만 resource를 교체한다. 재빌드 후 core 64 assertions, APK v3 signature와
policy를 통과시키고, SM-F971N에서 카드가 열린 상태로 8초 녹화해 한 회전 전체의 명암을 다시 본다.
이후의 품질 판정은 “3D니까 자연스럽다”가 아니라 형상 안정성과 재질·조명을 별도 항목으로 남긴다.

#### 13.4.2 Apple 공식 연결 카드와 대조해 넓은 순백 하이라이트를 재질 단계에서 줄인다 — 완료

13.4.1의 v3는 외곽 black crush를 연회색으로 바꿨지만 반대로 큰 면이 250~255 RGB에 붙었다. 이 상태를
lower fill 완료로 닫지 않고, Apple Support 104989의 iOS 26 AirPods Pro 연결 카드 원본을 reference-only로
확보해 정면을 나란히 본다. Apple 쪽의 넓은 중간 회색, 제한된 모서리 highlight, 회색 case cavity와
검은 vent를 분리해 비교 기준으로 삼는다. 숫자는 색공간이 다른 두 이미지의 절대 spec으로 사용하지 않고
현재 clipping을 찾는 보조 관측으로만 쓴다.

renderer에 emission, AgX look, exposure, world, main/lower light, white base scale, roughness, specular IOR를
각각 노출한다. A~C의 tone-only, D~F의 light/emission, G~O의 base/roughness 후보를 먼저 비교하고,
긴 흰 반사 띠가 남는 이유를 specular IOR로 좁힌 뒤 P~R을 만든다. 선택 Q를 renderer 기본값으로 옮긴
다음 옵션 없는 4방향 렌더가 Q와 같은 RGBA pixel인지 확인한다. 180-frame full render→animated WebP
계약→64 core assertions→APK signature/policy→SM-F971N 8초 녹화 순서로 검증하고 카드는 열린 채 둔다.

## 14. 수면 DND 중 연결 카드 무표시 정정

### 14.1 표시 여부를 결정하는 기반부터 고친다

#### 14.1.1 자정 실사용 기록으로 Bluetooth 실패와 표시 정책 실패를 분리한다 — 완료

사용자가 “연결해서 꼈는데 카드가 안 뜬다”고 한 시각을 기준으로 먼저 ACL/AAP, 화면, keyguard,
overlay app-op, type-2038 window, notification history를 같은 구간에서 읽는다. UI가 최신 연결을
보이고 type-3 socket이 열렸는데 창만 0이면 3D renderer나 battery transport를 다시 만들지 않는다.

진단 기록: 00:01:14와 00:01:51에 앱의 type-3 socket이 열렸고 UI는 `연결됨 · 방금 확인`이었다.
화면 Awake, overlay allow였지만 `zen_mode=1`이었다. connection notification ID 2102는 두 번 모두
`intercepted ... new:!priority`였다. 코드의 automatic DND false와 system notification DND가 겹쳐
아무 표면도 남지 않은 것이 직접 원인으로 확정됐다.

#### 14.1.2 무음 카드와 잠금 경계를 분리해 pure policy 기대값을 바꾼다 — 구현·65 assertions 완료

DND를 모든 시각 표면의 금지어로 취급하지 않는다. 카드가 sound, vibration, audio focus를 만들지
않고 `screenInteractive && !keyguardLocked`를 이미 요구하므로 DND on에서도 true여야 한다. 다만
DND가 잠금 경계를 우회하지 않는다는 반대 fixture를 함께 추가해 수면 중 화면을 깨우는 수정으로
번지지 않게 한다.

구현 기록: `ConnectionPopupPolicy`의 permission, interactive, keyguard, reading 조건은 그대로 두고
DND만 결과에서 제거했다. automatic + DND + unlocked는 true, automatic + DND + locked는 false로
고정했고 전체 65 assertions가 통과했다. AAP, renderer, session claim, 알림 channel은 수정하지 않았다.

### 14.2 설치 결과와 물리 검증을 나눈다

#### 14.2.1 APK를 빌드·정책 검사하고 현재 설정을 보존해 덮어 설치한다 — 완료

`test-core.sh → build.sh → verify-apk.sh`를 통과한 APK만 설치한다. 설치 뒤 수면 모드를 테스트하려고
임의로 끄지 않으며 `zen_mode=1`과 overlay allow가 유지됐는지 다시 읽는다. 이 단계가 통과해도 기존
Bluetooth session에는 새 ACL broadcast가 없으므로 자동 카드 실측 완료라고 요약하지 않는다.

설치 기록: 2026-09-05 00:06:50 SM-F971N 덮어 설치 성공, APK v3 signature와 권한 policy 통과,
설치 뒤 `zen_mode=1`, `SYSTEM_ALERT_WINDOW: allow` 유지.

#### 14.2.2 DND가 켜진 새 AirPods 연결에서 무음 카드 표면을 확인한다 — DND 경로 완료, 자동 AAP 1회 timeout 관찰

AirPods를 case에 넣어 기존 ACL을 끝낸 뒤 다시 착용한다. 자동 연결 시 type-2038 window가 정확히
1개인지, notification fallback이 새로 증가하지 않는지, AAP가 연결당 한 번인지 확인한다. 카드가
보이면 본문 또는 X로 닫아 window 1→0을 확인한다. 음악 재생이나 Bluetooth 전체 off는 이 표시
정책 시험에 필요 없으므로 하지 않는다.

실측 기록: 00:13:04 기존 ACL 종료, 00:16:19 새 ACL과 automatic AAP 시작을 확인했다. 첫 AAP는
handshake 응답 없이 10초 timeout이 나 값과 카드가 없었다. 같은 연결에서 수동 확인 1회를 실행하자
battery counter 94→97, 큰 카드 8→9가 됐고 fallback은 2로 유지됐다. `zen_mode=1`인 동안 type-2038
창 1개가 `[31,1157]-[1216,1907]`에 visible이었으며 screenshot에도 98%/100% 카드가 나타났다.
따라서 DND 시각 표면은 완료다. 다만 새 ACL부터 자동 battery까지 한 번에 통과한 결과는 아니므로,
누적 15회 중 첫 handshake timeout이 다음 자연 연결에서 반복되는지 별도 관찰한다. 이번 창은 사용자가
직접 확인하도록 열어 두었고 dismiss 회귀는 이전의 본문/X 1→0 결과를 유지한다.

#### 14.2.3 automatic AAP 첫 응답 timeout이 반복되는지 관찰한 뒤 재시도 정책을 결정한다 — 관찰 중

한 번의 timeout만 보고 같은 ACL의 자동 socket을 즉시 두 번 열면, 과거에 어렵게 검증한 오디오
비간섭과 연결당 한 번 계약을 근거 없이 넓히게 된다. 현재 누적 automatic 15회에서 timeout 1회이며
직후 사용자 명시 재시도는 성공했다. 다음 자연 연결에서도 같은 timeout이 반복될 때만 `profile 연결
후 짧은 지연`과 `실패 뒤 bounded 1회 retry`를 각각 독립 가설로 설계·오디오 회귀한다. 반복되지
않으면 일시적 AirPods 응답 부재로 기록하고 자동 socket 수를 늘리지 않는다.

## 15. 고정 동작을 사용자가 조절하는 설정으로 바꾼다 — 2026-09-08

### 15.1 화면보다 먼저 설정의 의미와 저장 경계를 만든다

#### 15.1.1 기존 prefs와 소비 지점을 읽고 바꿀 값·유지할 제약을 분리한다 — 완료

카드 닫기와 경고 기준은 취향이다. 반면 자동 AAP 30초 제한은 오디오 안전성의 근거라서
같은 ‘시간’이라고 설정에 열면 안 된다. AppSettings, 연결 카드 정책, overlay, LowBatteryPolicy와
MonitorService를 읽고 새 설정을 0 또는 3–60초, 이어버드/케이스 각 5–50%로 한정했다.
기기 선택·기존 세 스위치·통신 주기는 그대로 유지한다. D-013의 A1–A7을 이 기준으로 작성했다.

#### 15.1.2 UserOptions를 Android와 분리하고 잘못된 저장값도 개별 복원한다 — 완료

UI에서만 범위를 막으면 손상된 prefs나 이후 호출자가 우회할 수 있다. 생성자에서 범위를 검증하고,
restore에서는 타입/범위를 필드별로 검사해 해당 필드만 기본값으로 복원한다. 새 키가 없으면
기존 동작인 직접 닫기·20%·15%를 읽는다. 저장은 같은 editor의 commit 결과를 확인하고 실패하면
기존 옵션을 메모리에서도 복원하며 UI에 실패를 알린다. 저장 성공과 화면 표시를 분리하지 않는다.

#### 15.1.3 기존 연결 세션을 재시작하지 않고 새 설정을 소비한다 — 완료

경고 정책 객체를 갈아 끼우면 이미 알린 배터리에 다시 알릴 수 있다. 임계값만 교체하고 세션 이력을
남겼다. 다음 배터리 수신에서 평가한다. 카드는 show 때 옵션을 한 번 읽어 이번 카드의 수명을
정한다. 직접 닫기는 timer를 만들지 않고, 자동 닫기는 초→ms 변환 후 예약한다.
연결 해제·잠금 정책과 기존 제품 그림에는 새로운 의미를 얹지 않았다.

### 15.2 익숙한 정보 위계와 저장·취소 동선을 만든다

#### 15.2.1 Apple 공식 설정 이미지를 열어 색과 그룹의 역할을 확인한 뒤 native UI로 옮긴다 — 완료

Apple 지원 문서 108764의 iOS 26 AirPods 이미지를 실제로 열었다. 장식 이미지보다 기기 상태,
흰 그룹, 검정 정보, 파랑 변경값, 회색 설명의 위계를 참고했다. 첫 화면에는 배터리와 연결 상태,
그 아래 자주 바꾸는 값을 둔다. 기술 진단은 접되 권한 부족은 접힌 안에 숨기지 않는다.
최대 폭 560dp와 큰 글자일 때 세로형 설정 행을 넣고, 아이콘과 제품 회전 자산은 수정하지 않았다.

#### 15.2.2 숫자 선택·취소·기본값을 연결하고 실제 저장값으로 행을 갱신한다 — 완료

표시만 바뀌는 목업이 되지 않도록 숫자 선택창의 저장에서 UserOptions를 만들고 commit한다.
취소는 저장 함수를 호출하지 않는다. 복원에는 확인창을 두고 새 조절값만 되돌린다.
미리보기에도 실제 닫기 설정을 적용해 이어폰을 새로 연결하지 않고 수명을 시험할 수 있게 했다.

### 15.3 예측을 시험하고 실패 원인을 설계로 되돌린다

#### 15.3.1 순수 경계와 APK를 검사한 뒤 기존 서명을 유지해 업데이트 설치한다 — 완료

기존 APK를 비공개 artifacts에 보관하고 새 APK와 서명을 비교했다. 코어 263,039 assertions,
빌드와 APK 권한 정책 검사를 통과했다. 삭제 없이 덮어 설치하고 선택 기기·기존 스위치·권한·캐시를
화면에서 확인했다. 실제 새 연결 없이 배터리 통신 성공을 다시 주장하지 않는다.

#### 15.3.2 자동/수동 닫기와 숫자 변경·취소·재실행·복원을 실기기에서 구분해 시험한다 — 완료

자동 3초는 window 존재를 약 0.5초 간격으로 표본화해 2.86초 있음→3.41초 없음으로 확인했다.
이어버드 30% 저장, 케이스 25% 선택 후 취소해 15% 유지, 다시 25% 저장 후 프로세스 재실행으로
30%/25% 유지를 확인했다. 복원 확인창을 누른 뒤 20%/15%와 직접 닫기로 돌아왔다.
마지막 미리보기는 8.09초에도 남고 X를 누르면 없어졌다. 화면 트리 실패 시의 오래된 XML 문제는
D-013에 정정했고 그 이후 판정은 실제 켜진 화면의 새 캡처로 했다.

#### 15.3.3 좁은 화면·큰 글자와 넓은 화면을 별도 렌더하고 미확인 시험을 남긴다 — 완료

폰 전체 글자 설정을 바꾸지 않고 별도 API 35 에뮬레이터로 320dp/1.4배와 674dp/1배를 봤다.
각 실제 렌더를 열어 겹침과 잘림을 확인했다. 이것은 다른 One UI와 Bluetooth 호환성의 증명이 아니다.
새 연결·실제 저전력 수신·음악 청취는 사용자가 돌아왔을 때 별도로 확인한다.
결과는 R 2026-09-08에 연결했고 room 밖 공개본은 수정·커밋·배포하지 않았다.
