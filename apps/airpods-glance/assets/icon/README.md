# 런처 아이콘 v1 — 2026-09-08 개발용

> 2026-09-08 개발 당시의 기록입니다. 아래 ‘공개본 미반영’ 표시는 당시 상태를 뜻합니다.
> 이번 선별 소스에는 포함되지만 원본 기기 자료는 공개하지 않습니다.

## 1. 의도와 설계

사용자가 이번에 수정한 두 앱의 아이콘도 요청했다. iOS 계열 설정 화면과 맞추되,
에어팟은 밝은 바탕과 흰 이어버드, 맨 위로 톡은 파랑 바탕과 흰 상단 이동 기호로 구분한다.
문자는 넣지 않는다. 이 요청은 앞선 ‘아이콘은 나중에’ 범위를 이번에 변경한 것이다.

### 1.1.1 작은 크기와 시스템 마스크를 먼저 고려한다

이미지 생성 스킬의 내장 image_gen으로 앱별 원본을 생성했다. CLI나 별도 API 키는 쓰지 않았다.
완성 그림은 불투명한 정사각형이고, 바깥 모서리는 Android adaptive icon이 자른다.
원본을 generated_images에만 두지 않고 아래 앱 리소스로 복사했다. 에어팟은 첫 생성의
이어버드 폭이 넓어 안전 여백을 확보하는 편집을 한 번 더 했다. 알림용 기존 단색 아이콘은 보존한다.

### 1.1.2 리소스를 연결하고 기존 동작은 건드리지 않는다

사용 파일: [ic_launcher_art_v1.png](../../res/drawable-nodpi/ic_launcher_art_v1.png).
원본 실측은 1254×1254 RGB이며 alpha가 없다. 프롬프트의 1024 요청과 실제 출력 크기는 다르다.
API 26 adaptive icon과 API 33 테마용 단색 벡터를 추가했다. 테마 색을 켜면 시스템 팔레트가
표시되는 것이 정상이다. 전체 아트가 배경 레이어에 있으므로 전경·배경의 독립 시차 효과는 없다.

### 1.1.3 검증과 되돌리기

APK를 기존 서명으로 빌드하고 삭제 없이 덮어 설치한다. 이미지 교체 전 APK는 room의
비공개 artifacts/android-icons-20260908에 보관한다. 클래스 동작과 권한을 바꾸지 않는 범위다.
결과는 아래 검증 이력에 덧붙인다. 공개본 수정·커밋·배포는 하지 않는다.

### 1.1.4 검증 결과 — 완료

두 APK 모두 빌드·v3 서명 검증과 이전 설치본 서명 일치 검사를 통과했다.
삭제 없이 Galaxy Z Fold8 / Android 17 / One UI 9.0에 덮어 설치했다.
시스템 앱 정보 화면에서 새 아이콘의 실제 작은 크기와 모서리 마스크를 직접 확인했다.
에어팟 코어 263,039개·맨 위로 톡 코어 7,395개 검사를 통과했다.
에어팟 APK 권한 정책 검사 통과. 맨 위로 톡 접근성 서비스는 이번 업데이트 후에도 Bound 상태이며
Binding/Crashed 목록은 비어 있었다. 앱 동작·권한·저장값을 수정하는 소스 변경은 하지 않았다.
테마 단색 아이콘은 리소스 빌드로 검증했으며 사용자의 시스템 테마를 바꿔 시험하지 않았다.
기존 아이콘 리소스와 교체 전 APK는 보존했다. 커밋·공개본 반영·배포 없음.

## 2. 실제 사용 프롬프트

### 2.1 최초 생성

Use case: stylized-concept. Asset type: finished production Android launcher icon artwork for an AirPods battery companion, part of a refined iOS-inspired personal utility family. Generate ONE square 1024x1024 opaque bitmap, not a mockup. Full-bleed very light cool pearl grey background (#F2F2F7), smooth and opaque all the way to every square corner. Centered pair of two recognizable white silicone-tip short-stem wireless earbuds, gently facing inward, upright stems, balanced left/right composition. The two earbuds together fit entirely inside the central 56 percent of the canvas in both dimensions, generous empty space for adaptive icon masking. Pearly white ceramic-like bodies, subtle soft dimensional modeling, small dark speaker vents, gentle blue-grey shading sufficient for crisp small-size readability. Quiet, polished native iOS utility aesthetic, front-three-quarter product illustration, broad soft lighting. NOT a photograph of a device on a desk. No charging case, no battery badge, no lettering, no numbers, no Apple logo, no border, no extra objects. No dramatic shadow, chrome, sparkle, glass orb, excessive highlights. IMPORTANT the background fills the entire square without rounded corners, transparency, white framing or an inset icon tile. Android itself will mask the square later. Deliver only this single finished icon artwork.

### 2.2 안전 여백 편집

Edit target: supplied AirPods launcher artwork. Keep exactly the same two earbuds, subtle pearly white materials, inward-facing poses, and light grey background. Change ONLY the composition scale: shrink the complete two-earbud pair to 80% of its current size, centered in the exact same canvas center. The pair must occupy about 54% of the total square canvas width. Extend the existing perfectly smooth pale grey background across the freed space. This is an Android adaptive icon safe area correction, not a new design. Entire square remains opaque to all four corners. No rounded tile, no outline, no text, no extra objects. Retain full resolution.
