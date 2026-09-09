# 에어팟 한눈에 — 설계 인덱스

- [013 — 사용자가 바꾸는 설정과 화면](013-user-controlled-settings.md):
  카드 닫기·배터리 부족 기준 조절, 저장·취소·기본값 복원과 밝은 설정 화면.
  [개발 설치본 검증](../results/2026-09-08-user-settings.md) · [새 아이콘](../../assets/icon/README.md).

- [012 — 상태별 제품 표현과 애니메이션](012-state-driven-product-motion.md):
  물리 배치와 표시 구성요소를 분리하고, 실패한 닫힌 case 근사를 운영 앱에서 정지시킨다.

- 2026-09-07 이미지 유사성 정정: 정규화한 8방향 비교에서 closed case v2의 4px seam 중심
  어긋남을 발견해 110도/-1mm v3로 교체했다.
  [비교 및 실기기 결과](../results/2026-09-07-ios-current-image-comparison.md).

- 2026-09-07 실기기 정정: 케이스-open 카드의 현재 기준은 공동 pair 회전 + 닫힌 case다.
  [검증 및 반영 결과](../results/2026-09-07-ios-open-case-correction.md).

- [011 — 독립 회전과 수납 변형 자산](011-independent-product-variants.md):
  물리 검증 대기와 별개로 진행하는 렌더·시각 검증.

- 2026-09-06 추가: [010 — 수납 신호 관측 계층](010-placement-observation.md).
  iOS 실제 영상 검토 뒤, 상태별 그림에 앞서 primary/secondary 보고를 좌우와 분리해 검증한다.
  기존 배터리 검증의 좌우 mapping은 opcode 04에 관한 것이며 opcode 06까지 증명하지 않는다.

- 문서 상태: AAP가 독립 연결 3회와 무버튼 자동 연결에서 배터리·오디오 검증을 통과했다.
  Mac의 AirPods 자동 전환을 제한해 Galaxy 자동 연결도 복원했다. 큰 연결 카드는 Apple 실제 화면
  기반 하단 시트로 재설계했고, timeout 없는 본문/X 닫기와 CC BY 4.0 단일 3D mesh turntable을
  SM-F971N에서 검증했다. 수면 DND가 자동 카드를 막고 fallback 알림까지 사라지게 한 실사용
  실패를 수정·설치했다. DND가 켜진 실기기에서 무음 3D 카드 표면은 통과했지만 새 연결의 첫 AAP가
  1회 timeout 난 뒤 명시적 재시도로 표시됐으므로, 자동 battery 전 구간의 자연 재연결 관찰은 남아 있다.
- 최초 작성: 2026-09-04
- 대상 기기: Samsung Galaxy Z Fold8 (`SM-F971N`), Android 17, One UI 9.0
- 대상 이어폰: 현재 휴대폰에 페어링된 `테스트용 AirPods Pro`
- 제품 범위: 광고와 인터넷 연결 없이 연결 중 배터리를 읽고, 앱·알림·홈 화면
  위젯에서 보여 주는 개인용 Android 앱

## 읽는 순서

1. [001 — 제품 범위와 사용자 시나리오](001-product-scope-and-scenarios.md)
2. [002 — 근거와 배터리 프로토콜 검증](002-evidence-and-protocol.md)
3. [003 — 아키텍처와 상태 수명](003-architecture-and-state.md)
4. [004 — 위젯·알림·앱 화면 경험](004-widget-notification-and-app-ux.md)
5. [005 — 예측·검증·피드백 루프](005-validation-loop.md)
6. [006 — 실측 반증 뒤 AAP 직접 연결 확장](006-aap-direct-battery.md)
7. [007 — Apple 실제 화면을 기준으로 한 연결 시트 재설계](007-apple-like-connection-sheet.md)
8. [008 — 연결 시트 제품 회전 애니메이션](008-product-turntable-animation.md)
9. [009 — 수면 DND와 무음 연결 카드 표시 정책 정정](009-dnd-visual-card-policy.md)
10. [구현 태스크](../tasks/TASKS.md)

문서 번호는 읽기 위한 순서다. 구현은 이 순서를 따르지 않고, 수정 파급력이 큰
배터리 해석기와 상태 계약부터 시작한다.

## 결정 색인

| ID | 현재 결정 | 근거 | 다시 열 조건 |
|---|---|---|---|
| ADR-001 | 앱 이름은 `에어팟 한눈에`, 패키지는 `com.chocho.airpodsglance`로 한다. | 개인용 앱의 목적이 한눈에 배터리를 확인하는 것이기 때문이다. | 사용자가 다른 이름을 선택할 때 |
| ADR-002 | 홈 화면 위젯을 1차 버전의 필수 기능으로 포함한다. | 팝업을 놓친 뒤 다시 확인하는 것이 핵심 시나리오이며, 사용자가 현재 버전에 명시적으로 요청했다. | 없음. 레이아웃만 실기기 피드백으로 수정 |
| ADR-003 | 연결된 동안만 foreground service로 BLE를 감시하고, 연결이 끊기면 중지한다. | 케이스 개방을 상시 감시하면 대기 알림과 배터리 소모가 다시 생긴다. 연결 범위는 최신 위젯과 낮은 방해를 동시에 만족한다. | 연결 전 케이스 개방 팝업이 필수라는 새 요구가 생길 때 |
| ADR-004 | 좌·우·케이스 공개 BLE 배터리를 10% 단위로 표시하고 1% 정밀도를 꾸며내지 않는다. | Apple proximity-pairing 공개 필드는 nibble 0~10이며 실제 값은 10% 간격이다. | 사용자의 기기에서 AAP의 신뢰 가능한 1% 데이터를 별도 검증할 때 |
| ADR-005 | `0x0F`와 10을 넘는 nibble은 `알 수 없음`으로 처리한다. | 잘못된 패킷을 100%로 보정하면 사용자가 충전이 충분하다고 오판할 수 있다. | 실기기 패킷으로 11~14의 공식 의미가 확인될 때 |
| ADR-006 | 좌우 배터리 배치는 CAPod와 Podsify가 일치하는 해석을 초기 가설로 쓰되 실기기 한쪽 충전 시험 전에는 확정으로 취급하지 않는다. | GreenPods가 반대 해석을 사용하므로 공개 구현끼리 모순이 있다. | 실기기에서 한쪽만 케이스에 넣는 시험 결과가 나올 때 |
| ADR-007 | 선택된 페어링 기기가 연결된 동안의 Apple 패킷만 후보로 받고, 주소 일치 후보를 우선한다. | 주변 사람의 에어팟 배터리를 위젯에 표시하는 프라이버시·정확성 실패를 막아야 한다. | Android가 해결된 주소를 제공하지 않아 실기기 수신이 전혀 안 될 때 |
| ADR-008 | 주소 일치가 실패하면 곧장 주변 최강 신호를 확정값으로 쓰지 않고 진단 상태를 남긴다. | 같은 공간의 동일 모델을 자기 기기로 오인하는 것보다 `확인 필요`가 안전하다. | 케이스 근접 교정 절차가 사용자 검증을 통과할 때 |
| ADR-009 | ~~연결 팝업은 시스템 알림으로만 시작한다.~~ **실사용 피드백으로 재개방.** 알림은 overlay 불가 조건의 fallback으로 유지한다. | 사용자가 실제 연결 알림을 사용한 뒤 MaterialPods처럼 큰 카드가 필요하다고 판단했다. | 큰 카드가 과도하게 방해되거나 실기기 overlay가 불안정할 때 |
| ADR-010 | 연결 중 알림은 배터리 카드 역할을 하고, 연결 해제 시 제거한다. `대기 중` 상시 알림은 만들지 않는다. | MaterialPods에서 불편했던 상시 대기·광고성 표면을 되풀이하지 않기 위해서다. | Android 백그라운드 정책상 연결 감지가 불가능함이 실증될 때 |
| ADR-011 | 인터넷, 광고, 분석, 계정, 위치 권한을 넣지 않는다. | 기능에 필요하지 않고 개인용 로컬 도구의 신뢰 경계를 흐린다. | 현재 범위에서는 다시 열지 않음 |
| ADR-012 | MaterialPods는 새 앱의 실기기 배터리 검증이 끝난 뒤에만 제거한다. | 검증 전에 기존 수단을 없애면 실패 시 사용자가 배터리를 볼 방법이 사라진다. | 새 앱이 실제 좌·우·케이스 값을 반복 재현할 때 |
| ADR-013 | ~~Android 17 공개 `BluetoothSocketSettings`로 AAP L2CAP PSM `0x1001`을 연다.~~ **공개 API 가설은 폐기.** bonded AirPods 직접 AAP는 정확한 battery source 후보로만 유지한다. | BLE `0x07`이 0건이고 회전 주소가 bonded 주소와 달랐지만, 공개 builder는 BR/EDR을 지원하지 않았다. | 공개 BR/EDR API가 생기거나 검증된 대체 transport가 생길 때 |
| ADR-014 | ~~숨겨진 Android API 우회 없이 API 37 공개 socket API만 사용한다.~~ **실기기·공식 소스로 폐기.** 공개 builder는 RFCOMM/LE만 허용하고 BR/EDR type 3과 PSM `0x1001`을 거부한다. | 공개 API라는 이름을 BR/EDR 지원으로 잘못 일반화했다. 공식 builder 검증식과 실기기의 `type=0` 단계가 같은 반증을 냈다. | Android 공개 API가 BR/EDR을 명시 지원할 때 |
| ADR-015 | 상태 source를 `BLE_PUBLIC_DECILE`과 `AAP_EXACT_PERCENT`로 구분하고, AAP 값은 1% 단위 그대로 표시한다. | 기존 10% 제약을 전체 모델에 고정하면 더 정확한 AAP 값을 버리게 된다. UI는 출처에 맞는 정밀도 설명을 해야 한다. | AAP percent의 의미가 실측과 맞지 않을 때 |
| ADR-016 | non-SDK L2CAP fallback은 자동 경로에서 제거한다. | 숨은 socket은 실제로 생성·연결됐지만 약 5초 뒤 A2DP/HFP와 ACL 전체가 끊어져 P-AAP-4 안전 조건을 반증했다. | 명령 순서가 원인임을 통제 시험으로 분리하고, 연결 3회·오디오 회귀를 통과할 때 |
| ADR-017 | 순서를 고친 non-SDK AAP는 검증 APK에서 앱 전면의 명시적 30초 1회 시험으로 제한했다. 자동 ACL 경로에는 아직 넣지 않는다. | handshake 응답 뒤에만 enable/init을 보내고 추정 battery request를 제거하자 세 독립 연결에서 배터리와 오디오 안정성을 재현했다. | **충족:** 독립 연결 3회와 한쪽 case 좌우·충전 시험 통과. 다음 구현에서 bounded 자동 경로로 승격 검토 |
| ADR-018 | 새로 연결된 선택 AirPods마다 AAP를 한 번 자동 시작하고 30초에 닫는다. 같은 ACL의 중복 broadcast와 자동 실패는 재시도하지 않되, 사용자가 앱에서 누른 명시적 확인은 별도로 허용한다. | 세션 저장소의 persistent claim이 service 재생성과 중복 start를 막고, 수동 확인은 case를 나중에 열었을 때 값을 갱신할 수 있게 한다. | 자동 시작 실측에서 socket 2개, 오디오 단절, 또는 잘못된 기기 실행이 관측될 때 |
| ADR-019 | AirPods가 Galaxy에 붙기 전의 자동 연결 문제는 배터리 AAP와 분리한다. 상시 Apple BLE 감시나 강제 profile connect는 추가하지 않는다. | A2DP policy는 허용이었다. Mac Bluetooth를 끄자 AirPods가 Galaxy에 두 번 remote-initiated 연결해 Apple host 경쟁 가설이 지지됐다. | Mac의 AirPods별 자동 전환을 제한한 상태에서도 Galaxy 자동 연결이 실패할 때 |
| ADR-020 | ~~사용자가 특별 권한을 허용하면 화면 사용 중 첫 exact snapshot에 6초짜리 큰 application overlay를 세션당 한 번 표시하며 잠금·화면 꺼짐·DND·권한 없음에서는 알림으로 fallback한다.~~ **수명은 ADR-023, DND 조건은 ADR-026으로 대체.** | 큰 시각 표면 요구와 수면 모드를 함께 존중하려 했지만, DND가 fallback 알림까지 차단해 아무 표시도 남지 않았다. | 수명은 ADR-023, 표시 경계는 ADR-026 참조 |
| ADR-021 | 큰 연결 표면을 남색 상단 카드에서 Apple의 실제 paired-battery popup 문법을 따른 밝은 하단 시트로 바꾼다. 제품 그림은 로고 없는 독립 생성 자산을 쓰고 좌우 exact 값은 합치지 않는다. | 두 실제 iPhone 촬영에서 하단 시트·배경 dim·중앙 제목·우상단 닫기·큰 제품 그림·작은 초록 battery 계층이 반복됐고, 사용자가 기존 시각 차이를 직접 지적했다. | 실기기에서 시트가 너무 크거나 navigation 영역을 가림, dark mode 가독성 실패, 제품 그림이 오인될 때 |
| ADR-022 | ~~실제 자동 연결 시트는 6초 뒤 닫되, 사용자가 누른 디자인 미리보기는 X 또는 앱 종료까지 유지한다.~~ **ADR-023으로 대체.** | 현재 연결과 무관하게 마지막 snapshot을 천천히 검사해야 하며, 대화 화면에서 휴대폰으로 시선을 옮길 때 6초 미리보기를 놓친 실제 실패가 있었다. | 미리보기가 다른 앱 조작을 과도하게 막거나 X·앱 종료 뒤 window가 남을 때 |
| ADR-023 | 자동 연결과 미리보기 시트 모두 timeout 없이 유지하고 카드 본문 tap 또는 X로 닫는다. 연결 해제·service 종료의 stale-view 정리는 유지한다. | 사용자가 검사 시간뿐 아니라 실제 연결 카드도 본인이 닫을 때까지 남기길 명시했다. 본문 tap이 앱을 여는 부수 효과는 필요하지 않다. | 지속 카드가 실제 사용을 과도하게 가리거나 dismiss 뒤 view 잔존이 발견될 때 |
| ADR-024 | ~~독립 생성한 16시점 시트를 65-frame WebP로 보간한다.~~ **실기기 시각 반증으로 폐기.** | 이미지 생성기가 셀마다 제품 형상을 바꾸고 morph가 이를 겹쳐 이어버드 복제·case 변형을 만들었다. | 다시 열지 않음. rejected 자료로만 보존 |
| ADR-025 | 한 개의 3D mesh를 회전시켜 제품별 360도 animation을 오프라인 렌더한다. | 동일 topology를 쓰면 프레임마다 제품 개수·lid angle·형상이 바뀌지 않는다. 실시간 3D 엔진보다 작은 사전 렌더 자산이 이 overlay에 적합하다. | 사용 가능한 모델의 라이선스·시각 품질이 부족하거나 렌더 자산이 과도하게 클 때 |
| ADR-026 | 화면이 켜지고 잠금이 풀렸으며 overlay 권한과 battery reading이 있으면 DND 중에도 무음 연결 카드를 표시한다. 화면을 깨우거나 잠금 위에 띄우지는 않는다. | 실제 수면 모드 연결에서 overlay를 DND로 막은 뒤 notification fallback까지 intercepted되어 사용자가 아무 표면도 보지 못했다. | 카드가 소리·진동을 만들거나, 잠금 중 노출되거나, 수면 사용을 과도하게 가린다는 실측이 있을 때 |
| ADR-027 | 제품 자산의 파일/형상/유사성 gate를 분리한다. closed case는 110도 회전 뒤 whole-bounds 중심에서 world Y -1mm를 더 이동한 v3를 사용하며, Q 조명은 유지한다. | 정규화한 8방향 비교에서 v2의 lid/body mating row가 같은 92px 폭인데도 중심이 4px 달랐다. -1mm 후보는 두 row와 8방향 외곽을 맞췄고 Galaxy 8.018초 녹화에서 계단이 재현되지 않았다. | 실제 iOS의 더 높은 품질 closed-case 방향별 자료가 현재 silhouette을 반증하거나 사용자가 실기기에서 잔여 seam 오류를 발견할 때 |

## 변경 이력

### 2026-09-07 — closed case v2 형상 통과 판정 철회와 v3 seam 정렬

- 첫 reference/current 비교는 크기와 배경이 달라 폐기했다. 물체 높이·배경·yaw를 맞춘 8방향
  비교에서 v2의 45/90도 seam 외곽이 계단처럼 꺾이는 것을 확인했다.
- 90도 mating row의 lid/body 폭은 모두 92px였지만 중심이 4px 달랐다. depth scale 문제가 아니라
  visible seam center 문제였고, bounds center 뒤 world Y -1mm 보정에서 두 구간이 일치했다.
- renderer의 위험한 40도 default를 110도로 바꿨다. 옵션 없는 8방향과 검증 후보는 RMSE 0이었다.
- v3 180 unique/6000ms/clipping 0, static frame 일치, core 262,649, APK v3 signature/policy를 통과했다.
  SM-F971N 8.018초·245-frame 녹화의 16표본에서 v2 step이 재현되지 않았다.
- iPhone 촬영 노출과 Apple 공식 연결 카드의 명암이 서로 달라 material scale은 확정하지 않았다.
  이전 사용자 판정을 받은 Q 조명을 유지하고, 제3자 자산은 APK에 포함하지 않았다.

### 2026-09-06 — iPhone 회전과 케이스 안팎 표현 재검토

- [실제 촬영 비교](../results/2026-09-06-ios-motion-and-case-state-review.md)에서 AirPods 4의
  좌우 한 쌍 회전을 확대 연속 프레임으로 확인했다. 기존의 회전 미확인 기록을 정정한다.
- 실제 이어버드가 케이스 안에 있어도 iPhone 배터리 카드는 쌍과 케이스를 따로 표시했다.
  사용자가 제안한 안팎 상태 일치 방식은 별도의 제품 개선 방향으로 구분한다.
- 현재 앱은 케이스 내부 점유 상태를 저장하지 않으며 같은 채워진 케이스 자산만 표시한다.
  조사 결과를 기록했으며 APK는 아직 수정하지 않았다.

### 2026-09-04 — 초안

- 처음에는 위젯을 후속 기능으로 생각했다.
- **정정:** 사용자가 “위젯 지금 버전에서도 보고 싶다”고 명시해 1차 필수 기능으로
  승격했다.
- 처음에는 연결 순간 한 번만 짧게 스캔하면 충분하다고 생각했다.
- **정정:** 위젯이 연결 중 현재 상태를 보여 주려면 연결 세션 동안 갱신이 필요하다.
  대신 연결이 끊긴 동안에는 감시와 foreground 알림을 모두 중단한다.
- 공개 구현 하나의 좌우 해석을 정답으로 채택하려 했다.
- **정정:** CAPod·Podsify와 GreenPods의 해석이 갈리는 것을 발견해 실기기 한쪽
  충전 시험을 출시 기준에 추가했다.

### 2026-09-04 — 첫 설치 피드백 반영

- Samsung의 위젯 추가 창에서 `initialLayout`만 선언한 첫 APK는 provider 목록에는
  나타났지만 미리보기가 `위젯을 추가할 수 없습니다`로 대체됐다.
- **정정:** `previewLayout`을 별도로 선언했다. 두 번째 설치에서 미리보기 내용이 실제로
  inflate되었고 공식 `추가` 흐름이 성공했다.
- 미리보기 카드는 높이가 작아 제목 줄만 보였지만, 실제 홈 화면 4×1 영역에서는 기기명,
  세 배터리, 연결 상태가 모두 잘리지 않았다. 미리보기 한 장만 보고 실제 레이아웃을
  축소하지 않고 양쪽을 따로 검증한 판단을 유지한다.
- 미연결 상태에서 화면의 수동 확인을 시작한 시험은 foreground service와 무음 알림을
  만들었고 30초 뒤 service와 활성 알림이 모두 0개가 됐다. 이 결과는 연결 전 상시
  서비스를 두지 않는 ADR-003/010과 일치한다.

### 2026-09-04 — 실제 BLE 반증 뒤 AAP 확장

- 실제 연결 broadcast와 foreground service 시작은 성공했다.
- strict BLE filter에서는 후보 0이었다. Apple company ID filter로 넓히자 10초에 1,162건을
  받았지만 공개 battery type `0x07`은 0건이었다. 케이스 개방에서도 결과가 같았다.
- 기존 MaterialPods 역시 같은 연결에서 battery를 `-`로 표시했고, 두 앱 모두
  `neverForLocation`을 사용했다. 위치 권한이나 플래그를 원인으로 단정하지 않는다.
- BLE 광고 주소는 bonded classic 주소와 일치하지 않았다. 가장 강한 주변 값을 선택
  기기로 간주하는 ADR-008 위반을 하지 않았다.
- **설계 확장:** Android 17의 공개 BR/EDR L2CAP socket API로 선택 bonded device의 Apple
  Accessory Protocol을 직접 사용한다. BLE는 제거하지 않고 fallback과 케이스 관측 연구용으로
  남긴다.

### 2026-09-04 — AAP 구현 중 수명주기 보정

- 공개 socket adapter, exact-percent decoder, source migration을 의존성 순서로 구현했고
  47개 JVM assertion과 APK 정책 검사를 통과했다.
- 처음에는 8초 watchdog 하나만 둘 생각이었다.
- **정정:** socket connect와 AAP handshake는 원인과 복구 의미가 다르므로 connect 8초와
  handshake 10초로 나눴다.
- BLE fallback이 fresh AAP 값을 덮지 않는 것만 생각했으나 주소 불일치 calibration 경로도
  exact confidence를 후퇴시킬 수 있음을 발견했다.
- **정정:** snapshot 저장 입구와 calibration 입구 양쪽에서 fresh AAP source를 우선한다.
- 이 시점의 완료는 코드·설치 완료다. Samsung Android 17 공개 API의 실제 연결과 Pro 3
  battery 수신은 물리 연결 시험 전까지 완료로 간주하지 않는다.

### 2026-09-04 — 큰 연결 카드 피드백 반영

- 작은 시스템 알림은 연결 성공을 한눈에 확인하기 어렵다는 실사용 피드백으로 ADR-009를
  재개방했다. full-screen activity 대신 사용자가 켠 특별 권한 아래 6초 application overlay를
  선택했다.
- 표시 조건을 UI 밖의 순수 policy로 분리했고 정상·권한 없음·화면 꺼짐·잠금·DND·값 없음·preview
  일곱 경우를 고정해 JVM 전체 54 assertions를 통과했다.
- SM-F971N preview에서 카드가 `[42,110]-[1206,648]`에 표시되고 왼쪽 87%·오른쪽 94%가
  잘리지 않았다. 6초 자동 제거와 X 즉시 제거 뒤 type 2038 window가 남지 않았다.
- screenshot이 앱 본문의 status bar 겹침을 드러내 `systemBars()` inset을 추가했다. overlay와
  무관한 화면 회귀도 같은 시각 QA에서 발견해 설계에 되먹임했다.
- 양쪽을 case에 10초 넣었다 다시 착용한 실제 시험에서 19:18:11 remote 연결 뒤 type 2038
  카드가 자동 생성되고 19:18:17 제거됐다. 진단 `크게 1 · 알림 대체 0`, AAP automatic 5,
  위젯 왼쪽 86%·오른쪽 94%로 service 경로와 세션 중복 방지가 함께 확인됐다.

### 2026-09-04 — Apple 시각 기준을 확인한 뒤 재설계

- **정정:** 첫 큰 카드는 MaterialPods의 “놓치지 않는 크기”만 참고하고 Apple의 실제 화면을
  확인하지 않은 자체 남색 상단 카드였다. 사용자가 Apple 제품의 화면과 너무 다르다고 정확히
  지적했다. 기능 검증 성공을 시각 설계 성공으로 확대하지 않는다.
- Apple 공식 지원의 현재 open-lid/nearby 사용 장면과, 2025 AirPods 4 paired-battery popup,
  2025 AirPods Pro 3 및 2026 AirPods first-pair 촬영을 분리해 봤다. 연결된 배터리 장면에는
  Connect 버튼이 없고 밝은 하단 시트 안에 제품과 배터리만 남는다.
- 다음 구현은 overlay 수명·권한·fallback을 건드리지 않고 renderer만 밝은 하단 시트로 교체한다.
  좌우가 다른 exact percent는 Apple처럼 임의로 한 숫자로 합치지 않는다.
- 첫 Canvas art와 한 줄 battery label은 screenshot에서 각각 평면적인 제품, percent 잘림을
  만들었다. 로고 없는 독립 투명 render와 glyph→percent→part 세로 계층으로 바꿨다. 최종 light
  preview는 frame `[31,1157]-[1216,1907]`, 값 84/93/case 83, navigation gap 26px로 통과했다.

### 2026-09-04 — 미리보기 시간 부족 피드백 반영

- 처음에는 실제 연결과 명시적 미리보기 모두 동일한 6초 자동 종료를 사용했다.
- 첫 원격 조작은 고정 좌표가 버튼을 빗나가 창 자체가 뜨지 않았고, 정확한 버튼으로 다시 띄운 뒤에도
  사용자가 무엇을 확인해야 하는지 파악하는 사이 시트가 사라졌다.
- **정정:** 현재 AirPods 연결 여부는 미리보기 조건이 아니다. 저장된 마지막 snapshot을 렌더하고,
  명시적 미리보기만 X 또는 앱 종료까지 유지한다. 실제 자동 연결의 6초·DND·잠금 계약은 그대로다.
- **후속 정정:** 위 분리판 설치 직후 사용자가 실제 자동 카드 역시 직접 닫을 때까지 남아야 한다고
  명시했다. 자동과 preview 모두 timeout을 제거하고 본문 tap 또는 X로 닫는다. 연결 해제/service
  종료 정리와 DND·잠금·화면 꺼짐 표시 제한은 그대로 유지한다고 당시 판단했다. DND 부분은
  다음 날의 실제 무표시로 반증되어 ADR-026이 대체하며, 잠금·화면 꺼짐만 유지된다.

### 2026-09-04 — 정적 제품을 360도 turntable로 교체

- 사용자는 지속 카드에서 이어버드와 case 그림이 정적인 점을 바로 지적했다.
- Apple paired-battery 실촬영에서는 지속 회전을 명확히 확인하지 못했다. **정정:** 이 motion을
  Apple 사실로 귀속하지 않고 사용자가 원하는 경험과 MaterialPods에서 관측한 6초 turntable 구조로
  기록한다.
- MaterialPods APK의 영상은 조사만 하고 복사하지 않았다. 로고 없는 기존 독립 제품 render를 기준으로
  light/dark 16시점 시트를 새로 만들고 65-frame animated WebP 네 개로 가공했다.
- 첫 16시점 생성물은 transparency 요청에도 checkerboard가 실제 배경으로 들어가 폐기했다. 단색
  테마별 시트를 다시 생성해 배경 사각형 노출 위험을 줄였다.
- **후속 폐기:** 8.15초 실기기 영상을 직접 펼쳐 보니 각 keyframe 자체의 형상이 달랐고 morph 중
  이어버드 복제·늘어남과 case lid 변형이 명확했다. pixel 변화량을 자연스러움의 근거로 삼은 판단을
  철회한다. APK는 정적 안전판으로 되돌리고, 다음 경로는 단일 3D mesh 회전으로 제한한다.

### 2026-09-04 — 단일 3D mesh turntable 실기기 통과

- polyman의 `Airpods Pro With Magsafe Charging Case Ios15` CC BY 4.0 glTF와 attribution을 함께
  보존했다. 단순 parametric 모델은 형태 품질이 부족해 APK에 넣지 않았다.
- 첫 glTF re-parent 구현은 상위 0.01 scale을 잃어 100배 확대와 중심 이중 이동을 만들었다.
  **정정:** hierarchy를 바꾸지 않고 원래 world matrix에 중심 yaw matrix를 적용한다.
- case 단독 렌더는 원본의 저해상도 stand-in earbuds를 검게 드러냈다. 이를 숨기고 같은 원본의 상세
  bud hierarchy를 case well에 배치해 하나의 product transform으로 회전시켰다.
- 320×320·180 frames·33ms·무한 loop WebP 두 개를 만들었다. SM-F971N의 8.006초 녹화는 242 frames,
  약 30fps였고 16-frame contact sheet에서 형상 변형과 loop 되감김이 없었다. 본문 tap·X는 각각
  overlay 1→0, 재개방 뒤 1을 확인했다.
- **후속 조명 정정:** 형상·모션 통과를 재질 품질 통과로 확대했던 판단을 철회한다. 최초 렌더의 흰
  플라스틱 외곽이 검정으로 눌렸고, 전체 노출을 올린 두 preview는 반대로 굴곡을 날렸다. 원래 재질과
  노출을 복원하고 약한 lower fill만 추가한 세 번째 안을 4방향 preview→8방향 전체 frame→SM-F971N
  8.014초 영상 순서로 확인했다. 기존 강한 그림자 자산은 실패 이력으로 남기고 APK만 교체했다.
- **후속 하이라이트 정정:** lower-fill판도 큰 흰 면이 250~255에 붙는 문제가 남았다. Apple Support
  104989의 iOS 26 연결 카드 원본과 직접 나란히 보고, 넓은 중간 회색·제한된 모서리 반사·검은 vent의
  분리를 새 기준으로 삼았다. tone→light→base/roughness→specular IOR 순서로 후보를 좁혀 Q를 선택했고,
  기본 renderer 값도 Q로 바꿨다. 180-frame sheet와 SM-F971N 7.986초 영상에서 clipping과 flash가
  없는 것을 확인했다. Apple reference는 검수용으로만 보존하며 APK에는 포함하지 않는다.

### 2026-09-05 — 수면 DND의 무음 카드 억제 폐기

- 자정의 실제 연결에서 AAP socket과 최신 battery 상태는 정상이었지만 큰 카드가 나타나지 않았다.
  화면은 Awake, overlay 권한은 allow였고 유일한 억제 입력은 `zen_mode=1`이었다.
- automatic overlay가 DND를 이유로 false가 된 뒤 connection notification ID 2102도 Samsung DND에
  `intercepted`됐다. 수면 모드를 존중하려던 이중 억제가 사용자에게 아무 표면도 남기지 않았다.
- **정정:** 화면이 켜지고 잠금이 풀린 동안의 카드는 소리·진동·화면 깨우기가 없는 시각 표면이므로
  DND 중에도 허용한다. 화면 꺼짐·잠금·권한 없음·값 없음 경계는 유지한다.
- pure policy 65 assertions, APK signature/policy 검사를 통과해 00:06:50 덮어 설치했다. 설치 뒤에도
  DND와 overlay 권한은 그대로였다. 00:16:19 새 ACL의 automatic AAP는 handshake timeout이었지만,
  같은 연결에서 명시적 battery 재시도가 성공해 `zen_mode=1`과 type-2038 카드 1개를 동시에 확인했다.
  큰 카드 진단은 8→9, fallback은 2 그대로였다. DND 시각 경로는 완료하되 자동 첫 응답의 일시적 실패는
  task 14.2.3에서 별도로 관찰한다.

### 2026-09-04 — 공개 API·오디오 비간섭 예측 반증

- 실기기 진단은 `class=1, builder=1, type=0`으로 public builder의 type 3 거부를 재현했다.
- Android 공식 `BluetoothSocketSettings.Builder` 소스도 `TYPE_RFCOMM`과 `TYPE_LE`만 허용하고,
  LE PSM을 `0x80..0xFF`로 제한한다. 따라서 BR/EDR `0x1001`을 공개 API로 연다는 ADR-014의
  전제는 틀렸다.
- 개인용 APK에서 한 메서드만 여는 non-SDK fallback을 통제 시험했다. socket type 3 연결은
  성공했지만 약 5초 뒤 ACL reason 8로 A2DP/HFP까지 끊겼고, 자동 재연결의 두 번째 시도도
  reason 20으로 종료됐다.
- **안전 정정:** P-AAP-4가 반증된 즉시 자동 AAP 시작과 bypass dependency를 제거한 APK를
  설치했다. 다음 연결은 BLE fallback만 사용하므로 이 disconnect를 반복하지 않는다.

### 2026-09-04 — protocol 순서 통제 재시험 3회 성공

- MaterialPods를 삭제한 뒤 우리 앱만 있는 조건에서 UI가 명시적으로 시작하는 30초 실험
  경로를 따로 만들었다. ACL receiver와 boot/자동 경로에는 experiment extra가 없다.
- 첫 실패에서 한 번에 보냈던 handshake, enable, InitExt, 추정 battery request를 분리했다.
  handshake만 flush하고 status 0을 확인한 뒤 enable 두 개와 InitExt를 각각 flush했으며,
  battery request `0x0003`은 제거했다.
- 18:18:41 type 3 socket 연결 뒤 handshake, post-handshake 활성화, battery 수신이 모두
  진행됐다. 앱과 위젯은 왼쪽 98%, 오른쪽 92%, 케이스 unknown을 같은 snapshot으로 표시했다.
- 1~30초의 13개 표본에서 ACL과 A2DP가 계속 connected였고 Bluetooth event history에도 새
  disconnect가 없었다. 30초에 실험 socket만 자동으로 닫힌 뒤에도 오디오는 connected였다.
- **판정:** 첫 단절의 원인은 transport 자체보다 초기 protocol 순서 또는 근거 없는 request였을
  가능성이 커졌다. 어느 한 packet이 원인인지는 아직 분리하지 않았고, 한 번의 성공으로 자동
  경로를 되살리지는 않는다.
- 두 번째 시험 전 18:23:47 remote-user disconnect와 18:27:30 새 ACL connection을 system
  history로 확인했다. 음악을 AirPods A2DP에서 실제 재생한 채 18:28:38부터 다시 실행했고,
  30초 모든 표본에서 playback `started`와 ACL connected가 유지됐다. 새 값은 왼쪽 95%,
  오른쪽 92%였으며 v2 experiment/handshake/activation/window counter가 각각 2가 됐다.
- 세 번째 시험 전 18:31:24 disconnect와 18:32:02 새 ACL connection을 확인했다. 왼쪽만
  착용하고 오른쪽을 열린 case 안에 둔 실제 상태에서 왼쪽 92%, 오른쪽 92% `충전 중`,
  case 84%가 표시됐다. 음악과 ACL/A2DP는 다시 30초 전체를 유지했고 앱·위젯의 값과 charging
  표시가 일치했다. 이로써 세 독립 연결 반복성과 wire 좌우 mapping을 함께 확인했다.
- 검증 기준 충족 뒤 자동판을 구현했다. `ConnectionSessionStore`가 새 ACL session마다 automatic
  AAP claim을 한 번만 내주고, receiver의 verified start만 그 claim을 사용할 수 있다. widget
  refresh에는 자동 권한이 없고 앱 버튼은 명시적 manual 경로다. 47개 core assertion, APK
  signature/policy를 재검사하고 덮어 설치했으며 설치 중 A2DP playback은 유지됐다.
- 자동판 설치 뒤 사용자가 앱 버튼을 누르지 않고 Bluetooth 설정에서 AirPods를 연결했다. 새 ACL
  직후 18:43:15 AAP socket이 한 번만 열렸고 진단은 automatic 1, manual 0, window close 1이었다.
  배터리 push 뒤 위젯은 왼쪽 90%, 오른쪽 98%, case unknown으로 갱신됐으며 media playback과
  A2DP는 유지됐다. bounded 자동 battery 경로의 최종 실측을 통과했다.
- 동시에 케이스/착용만으로 Galaxy가 연결을 시작하지 않는 별도 UX를 발견했다. system history는
  18:43:12 System UI의 수동 connect만 기록했고 그 전 remote/local attempt는 없었다. A2DP
  connection policy 100, recent/active device 상태는 정상이므로 다른 Apple host 경쟁과 AirPods의
  마지막 host 선택을 먼저 검증한다.
- iPhone과 iPad Bluetooth는 이미 꺼져 있었고 Mac Bluetooth만 남은 조건이었다. Mac Bluetooth를
  끄자 AirPods가 18:51:36과 18:52:04에 Galaxy로 `Connection request`를 보내 remote-initiated
  ACL을 만들었다. 정확한 Apple switching 내부 결정은 보이지 않지만, 관측 가능한 변경점과
  결과가 반복돼 Mac host 경쟁이 원인이라는 해석을 강하게 지지한다.
- 두 remote ACL 모두 우리 automatic claim을 거쳐 direct battery를 시작했다. 누적 진단은
  automatic 3, manual 0이었고 최신 snapshot은 왼쪽 88%, 오른쪽 96%, case unknown이었다.
  Galaxy automatic connection과 battery automation이 순서대로 함께 작동함을 확인했다.
