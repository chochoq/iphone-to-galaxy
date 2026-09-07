# 제품 turntable 실기기 preview — 2026-09-04

## 결과

- 자산 네 개 모두 320×320, 65 frames, 92ms/frame, infinite loop
- 한 회전: 약 5.98초
- 시스템 animator duration scale: 1.0
- 0→1.5초 changed pixels: 103,749
- 1.5→3초 changed pixels: 93,016
- 0→5.7초 changed pixels: 85,079
- 5.7초 뒤 application overlay type 2038: 1개, 지속 표시 정상
- 시간차 제품 contact sheet: `build/turntable-motion-contact.png`

위 pixel 차이는 상태 표시줄 시계 변화가 없는 수 초 안에 같은 overlay를 촬영한 결과이며, contact
sheet에서 제목은 고정되고 이어버드와 case 시점만 달라지는 것을 함께 확인했다. 따라서 animated
resource가 첫 프레임에 멈춘 것은 아니다.

## 생성·가공 이력

기존 로고 없는 투명 이어버드·열린 case render를 정체성 기준으로 삼아 built-in 이미지 생성 기능으로
각각 4×4, 16 yaw views를 만들었다. 핵심 프롬프트는 `frame 1 front, each next frame +22.5 degrees,
same model/scale/camera/lid angle, no logo/text/borders`이며 light `#F2F2F7`와 dark `#2C2C2E` 배경을
각각 요구했다. 첫 이어버드 transparency 시트는 checkerboard가 실제 픽셀로 생성되어 폐기했고,
테마별 단색판을 다시 생성했다.

최종 생성 시트는 `design-assets/generated-turntables/` 네 파일에 저장했다. 각 핵심 각도 사이를 세
프레임씩 보간한 뒤 `img2webp`로 65-frame animation을 만들었고, 앱은
`res/drawable-nodpi/airpods_{pair,case}_turntable_{light,dark}.webp` 네 파일을 소비한다.

MaterialPods의 보관 APK에서는 제품별 550×550, 72fps, 6초 MP4 구조와 yaw 순서만 관측했다. 해당
영상·Lottie·제품 자산은 복사하지 않았다. Apple paired-battery 실촬영에서는 지속 회전을 명확히
확인하지 못했으므로 이 motion을 Apple 사실로 귀속하지 않는다.

## 남은 피드백

사용자가 실제 화면에서 회전 속도와 중간 형태를 판정해야 한다. 밝은 시트의 흰 제품 대비, 프레임 사이
형태 변화, loop 경계가 눈에 걸리면 핵심 각도 또는 배경 matte를 다시 만든다. 본문 tap과 X 각각의
detach/WindowManager 0개, dark-mode screenshot은 그 뒤 별도로 확인한다.

## 후속 정정 — 이 결과를 자연스러운 회전 성공으로 사용하지 않는다

사용자가 웃으며 애니메이션을 직접 확인해 보라고 한 뒤 8.15초 실기기 영상을 0.5초 간격으로 다시
펼쳤다. 이어버드가 늘어나고 복제됐으며 case lid와 내부 형상도 key view 사이에서 바뀌었다. 위의
changed-pixel 수치는 재생 중이라는 사실만 보였고 자연스러움을 증명하지 못했다. 네 WebP는 APK에서
철회해 `design-assets/rejected-turntable-webp/`에 보존했다. 현재 결과는 실패 이력이며, 대체 구현과
실기기 통과 결과는 `2026-09-04-3d-product-turntable.md`를 따른다.
