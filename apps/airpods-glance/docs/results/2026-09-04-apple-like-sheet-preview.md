# Apple 실제 화면 기반 연결 시트 preview — 2026-09-04

## 1. 사용자 장면과 수정 이유

남색 상단 카드는 자동 연결·한 번 표시·6초 제거를 통과했지만 사용자가 Apple 제품의 실제 화면과
디자인이 너무 다르다고 지적했다. 첫 renderer는 Apple popup을 확인하지 않고 기능 계층만 설계한
것이므로 기능 성공과 시각 성공을 분리하고 다시 조사했다.

## 2. 근거와 구현 전 예측

Apple 공식 지원의 open-lid/nearby 장면, 2025 AirPods 4 paired-battery 촬영, 2025 AirPods Pro 3와
2026 AirPods pairing 촬영을 확인했다. 반복 구조는 밝은 하단 시트, 뒤 화면 dim, 중앙 title,
우측 X, 큰 제품 그림, 그림 아래 작은 초록 battery였다. 우리 연결 장면에는 Connect 버튼을 넣지
않고 좌우 exact percent만 분리하면 Apple 문법과 데이터 진실성을 함께 보존할 것으로 예측했다.

## 3. 구현과 중간 실패 이력

첫 수정은 Canvas path로 이어버드와 case를 그렸다. 하단 시트 구조는 맞았지만 제품이 평면적이고
case가 너무 크게 보였다. 또한 `왼쪽 84%` 한 줄 TextView의 측정 폭이 battery glyph 폭으로 줄며
화면에는 `왼쪽`만 보였다. 이를 완성으로 처리하지 않고 다음처럼 바꿨다.

- built-in 이미지 생성 기능으로 로고·문자·배경 없는 이어버드와 열린 case 제품 render를 각각
  만들었다. 공식 Apple bitmap은 복사하지 않았다.
- 생성 원본 alpha를 보존하고 동일 512×512 canvas에서 이어버드 content 450×360, case 400×330
  이내로 정규화했다.
- battery를 glyph→큰 percent→작은 component name의 세로 계층으로 바꾸고 각 값에 78dp 폭을
  주었다.
- case component의 percent가 unknown이면 제품 묶음 전체를 숨기고, 값이 있으면 두 번째 묶음을
  만든다.

최종 프로젝트 자산은 `res/drawable-nodpi/airpods_pair_render.png`와
`res/drawable-nodpi/airpods_case_render.png`다.

## 4. 최종 실기기 결과

- SM-F971N light-mode preview frame: `[31,1157]-[1216,1907]`
- gesture navigation 영역 시작: y=1933, 시트와 26px 간격
- 표시값: 왼쪽 84%, 오른쪽 93%, case 83%
- 시각 계층: dim된 앱 → 밝은 하단 surface → 중앙 기기명/X → 이어버드·case → battery/percent
- 잘림: title, 제품, 세 percentage, component name 모두 없음
- case unknown이던 직전 preview에서는 case 묶음이 사라지고 이어버드가 중앙 배치됨
- 6초 뒤 type 2038 window 0: `apple_sheet_auto_dismiss_pass`
- X 직후 type 2038 window 0: `apple_sheet_close_pass`
- JVM 59 assertions, APK build/sign/policy check 통과

최종 화면 캡처는 `build/airpods-apple-sheet-preview-v3.png`에 있다. 캡처 순간 화면 위쪽에 뜬
안전 안내문자는 별도 Samsung system notification이며 우리 overlay frame이나 dim의 일부가 아니다.

## 5. 남은 반증

사용자가 실제 화면에서 Apple 연결 화면에 가까워졌는지 확인해야 한다. 그 피드백 뒤 양쪽을 case에
넣었다 다시 착용하는 새 ACL 1회에서 새 renderer가 자동으로 한 번 나타나고, 6초 뒤 제거되며,
AAP·위젯·오디오가 회귀하지 않는지 확인한다. dark mode는 구현돼 있지만 이번 light screenshot만으로
시각 통과를 주장하지 않는다.
