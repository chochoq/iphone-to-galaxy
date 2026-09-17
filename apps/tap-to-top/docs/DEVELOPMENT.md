# 맨 위로 톡 개발 안내

[사용법](../README.md) · [해결하려던 문제](../../../problems/tap-status-bar-to-scroll-top.md)

목표는 긴 목록의 맨 위까지 올라가고 사용자가 중간에 움직임을 잡을 수 있게 하는 것입니다.
외부 앱의 offset과 애니메이터를 직접 소유하지는 못합니다. 0.0.5는 대상 화면의 목적지 동작을
먼저 사용하고, 확인하지 못한 화면에는 기존 한 번의 플릭을 보냅니다.

## 공통 경로

활성 application 창에서만 후보를 찾습니다. 자신의 앱·SystemUI, 가로 목록을 제외하고
가시 면적과 최소 크기를 확인합니다. 검색 깊이 45·노드 600개로 제한합니다.

| 조건 | 요청 |
|---|---|
| AndroidX granular + 한 열 collection + 확인된 정방향 + BACKWARD | AndroidX scroll amount에 양의 무한대 |
| Framework granular + UP | Framework scroll amount에 양의 무한대 |
| 안전한 목적지 경로를 확인하지 못함 | 한 번의 합성 플릭 |
| 요청 거부 / 지원 목록에서 위로 액션 없음 | 추가 플릭이나 이동 재시도 없음 |

두 granular 플래그와 인수 키는 다릅니다. RecyclerView 1.4.0 배포 소스의 BACKWARD +
AndroidX infinity는 smoothScrollToPosition(0)으로 처리됩니다. UP으로 대체하지 않습니다.
역순 목록은 adapter 0이 아래일 수 있어 자식 행 순서도 확인합니다. 수락(true)은 도착 완료가
아니며 제품의 마지막 시도 안내도 이를 구분합니다.

큰 비지원 wrapper 안의 지원되는 한 열 목록이 폭 90%·높이 75% 이상이면 본문 후보로
위임합니다. 여러 패널이나 더 작은 목록까지 일반화하지 않습니다. 빈 WebView 가상 트리는
같은 창에서 120ms 간격으로 최대 두 번 **조회만** 다시 합니다. 이동 요청 뒤에는 반복하지 않습니다.

## 좁게 한정한 홈 피드 보완

- **Instagram:** 검증한 홈은 깊은 위치에서 자식 행 정보를 생략합니다. 정확한 패키지·목록 ID·
  클래스·선택된 홈으로 UNKNOWN만 보완합니다. 관찰된 REVERSE는 허용하지 않습니다.
- **YouTube:** 실제 목록 바깥 coordinator가 더 크게 보입니다. 본문을 찾고 검증한 홈의 상단 바를
  먼저 펼친 뒤 마지막에 단 한 번 목록 목적지를 요청합니다. 반대 순서는 목록을 멈출 수 있었습니다.
  한 행만 보이는 UNKNOWN 보완도 정확한 ID·선택된 홈으로 제한합니다.
- **X:** 공통 목적지 기능이 없는 추천·팔로우 중 홈에서 유일한 selected 홈의 의미 CLICK을
  요청합니다. 숨은 버튼 좌표는 누르지 않습니다. 바가 사라졌으면 표준 UP 한 번 뒤 새 홈을
  확인합니다. 조회는 3회/600ms 제한이며 같은 창·목록만 허용하고 오래된 노드를 재사용하지 않습니다.

X 준비 중 터치·창 변경·설정 변경은 후속 요청을 취소합니다. 이미 상단이면 홈을 누르지 않습니다.
다만 **X 자체 이동 도중 가로채기는 확인하지 못했고**, 홈 재선택 때 새 글이 반영될 가능성도 있습니다.
준비 취소와 애니메이션 중단을 같은 성공으로 세지 않습니다. 고정 홈/Home·추천/For you·
팔로우 중/Following 비교는 제한된 탐색 요소에서만 하며 저장하지 않습니다.

## 채택하지 않은 방식

- 연속 합성 스와이프: 재가속이 반복되고 다음 입력이 사용자 개입을 방해했습니다.
- 숨은 X 홈 좌표 클릭: 게시물을 잘못 열 수 있었습니다. 의미 노드 요청과 구분합니다.
- 무조건 SCROLL_TO_POSITION(0): RV에서는 즉시 점프했습니다. 플랫폼 ListView는 애니메이션
  구현이지만 API 35 시험에서 대부분 행 단위로 이동해 채택하지 않았습니다.
- 여러 열 허용만으로 그리드 지원: 실제 RV의 같은 infinity가 세로에서는 위, 가로에서는 왼쪽,
  역순에서는 아래로 갔습니다. 수락만으로 상단 성공이라고 판단하지 않습니다.

행 정보 누락·한 화면 한 행·그리드 방향·웹의 목적지 지원이 다음 공통 과제입니다.
시험 구조의 통과를 모든 상용 앱 지원으로 표현하지 않습니다.

## 구조와 개인정보

- TapTrigger / ScrollOptions: 탭 구분과 옵션 범위
- ScrollPolicy: 플랫폼 호출이 없는 능력·방향·취소 정책
- TapToTopService: 후보 탐색, 요청, 제한된 재조회와 취소
- AppSettings: 옵션 저장, 새 방식 ON/OFF (기본 ON)
- ScrollAttempt: 프로세스 메모리에 마지막 경로 한 건. 사용 기록 저장 없음
- MainActivity / 공통 SettingsUi: 설정과 권한 안내

접근성으로 창 구조와 resource ID를 확인합니다. 인터넷·사진·마이크·위치 권한은 요청하지 않습니다.
스크롤 세기 60–160%·길이 40–75%는 합성 플릭만 바꾸며 네이티브 속도는 바꾸지 않습니다.
알림창 드래그와 사용자 개입 때의 예약 취소를 유지합니다.

## 검사하기

저장소 루트에서:

~~~sh
sh shared/android-ui/test-core.sh
sh apps/tap-to-top/test-core.sh
sh apps/tap-to-top/build.sh
~~~

순수 검사 9,093개와 별도 API 35의 구조·제품 통합 검사 129개를 통과했습니다.
129개에는 미지원 상황과 방향 반례도 포함하며 지원 앱 수가 아닙니다. 실제 RV 1.4.0에서
10·100·1,000번째부터 도착·중단, wrapper·가로 카드·역순·거부·웹·설정 ON/OFF·두 번 탭을
검사합니다. 기존 WebView 시험 버전은 124.0.6367.219입니다.

통합 검사는 사용자 폰이 아닌 **전용 API 35 AVD TapTop006**에서만 실행합니다.
SDK Manager에서 API 35 시스템 이미지를 준비하고 해당 이름의 폐기 가능한 AVD를 만든 뒤
포트 5580에서 실행하세요. Node.js, curl, unzip, zip, JDK와 ANDROID_SDK_ROOT가 필요합니다.
아래 출력 경로는 비어 있어야 하며 시험 APK는 제품 배포물에 넣지 않습니다.

~~~sh
node apps/tap-to-top/tools/build-scroll-harness.mjs /tmp/scroll-harness
adb -s emulator-5580 shell setprop debug.hwui.drawing_enabled 1
adb -s emulator-5580 install apps/tap-to-top/build/tap-to-top.apk
adb -s emulator-5580 install /tmp/scroll-harness/scroll-test.apk
# 시험 기기의 접근성 설정에서 맨 위로 톡을 켠 뒤 실행합니다.
node apps/tap-to-top/tools/run-scroll-checks.mjs emulator-5580 /tmp/scroll-results \
  settings service regression web webcancel coverage gridcontract legacy motion
~~~

ATD의 렌더링 설정을 바꿨다면 앱을 다시 시작하세요. 일반 UIAutomator는 접근성 서비스를
중단할 수 있어 이 검사는 DONT_SUPPRESS_ACCESSIBILITY_SERVICES를 사용합니다.
실기기 범위와 남은 검증은 [호환성 안내](../../../compatibility/README.md)에 있습니다.

근거: [RecyclerView 1.4.0 소스](https://dl.google.com/dl/android/maven2/androidx/recyclerview/recyclerview/1.4.0/recyclerview-1.4.0-sources.jar),
[Android NodeInfo](https://developer.android.com/reference/android/view/accessibility/AccessibilityNodeInfo),
[Compose 접근성 구현](https://github.com/androidx/androidx/blob/androidx-main/compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/platform/AndroidComposeViewAccessibilityDelegateCompat.android.kt).
현재 Compose 소스를 X에 포함된 정확한 라이브러리 버전의 증거로 취급하지 않습니다.
