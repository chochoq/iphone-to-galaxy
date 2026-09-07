# 단일 3D 제품 turntable 실기기 결과 — 2026-09-04

## 결과

- 원본: `Airpods Pro With Magsafe Charging Case Ios15` by polyman, CC BY 4.0
- pair/case: 320×320 RGBA animated WebP, 각 180 frames, 33ms/frame, infinite loop
- 크기: pair 753,462 bytes, case 906,258 bytes
- SM-F971N 녹화: 8.006초, 242 video frames, 약 30fps
- 본문 tap: application overlay 1→0
- X tap: application overlay 1→0
- 재개방: application overlay 0→1, 사용자 확인을 위해 열린 상태로 종료

## 직접 시각 검수

렌더 전 0°·90°·180°·270° cardinal contact sheet를 먼저 만들었다. 전체 180-frame 렌더 뒤에는 pair와
case를 나란히 둔 6초 MP4를 0.5초 간격으로, APK 설치 뒤에는 실제 overlay 8초 녹화를 16-frame contact
sheet로 펼쳐 봤다. 이어버드 수, case body, 열린 lid 각도가 프레임 사이에서 바뀌지 않았다. 옆면에서
두 bud가 겹치고 뒷면에서 lid가 seated bud를 가리는 변화는 같은 3D scene의 depth 관계다.

첫 screenrecord는 overlay가 실제로 표시되기 전에 button tap이 들어가 앱 본문만 담겼다. 당시 type 2038
개수 1만 보고 성공으로 닫지 않고 현재 화면 캡처로 card를 확인한 다음, card가 열린 상태에서 screenrecord를
다시 시작했다. 두 번째 영상은 8초 동안 제품 각도가 계속 바뀌고 텍스트와 battery가 고정된 것을 담았다.

## loop 경계 보조 검사

frame 179→0과 평상시 frame 0→1의 RMSE를 비교했다.

| 제품 | 179→0 | 0→1 |
|---|---:|---:|
| pair | 3780.91 | 3788.83 |
| case | 3020.87 | 3057.82 |

경계 이동량이 평상시 2° 이동과 비슷하다. 이 수치는 자연스러움의 증명이 아니라 특이하게 큰 loop jump가
없는지를 보조한다. 최종 판정은 실제 8초 영상과 contact sheet의 직접 검수에 둔다.

## 보존 산출물

- source glTF/license: `design-assets/3d/external/polyman-airpods-pro/`
- Blender scene/180 PNG frames: `design-assets/3d/polyman-turntable-final/`
- pre-APK preview: `build/polyman-turntable-preview.mp4`
- live video/contact sheet: `build/airpods-3d-turntable-visible-live.mp4`,
  `build/airpods-3d-turntable-visible-video-contact.png`
- app resources: `res/drawable-nodpi/airpods_{pair,case}_turntable_3d.webp`

## 후속 조명 정정 — 같은 날 사용자 판정 반영

앞선 결과의 “실기기 통과”는 topology와 motion에는 맞았지만 제품 렌더의 색·그림자까지 통과시킨
판정으로 읽히면 안 된다. 사용자가 직접 본 카드에서 흰 제품 아랫면이 검은 띠처럼 보였고, 이를 조명
실패로 판정했다.

- 첫 수정: 전체 emission·ambient·exposure와 lower fill을 크게 올림 → 검정은 사라졌으나 굴곡도
  사라진 과노출이라 설치하지 않음
- 두 번째 수정: 일부 낮춤 → 여전히 카드 배경에서 제품이 평평하게 뭉쳐 설치하지 않음
- 최종 수정: 원래 재질·노출을 복원하고 lower fill만 energy 2로 추가 → 외곽은 연회색, 실제 cavity와
  검은 부품만 어둡게 유지
- 새 파일: pair 678,848 bytes, case 798,104 bytes; 둘 다 320×320, alpha, 180 frames,
  33ms/frame, infinite loop
- 검증: 0°·90°·180°·270° preview, 전체 렌더의 8-angle sheet, SM-F971N의 8.014초·246-frame
  screenrecord를 순서대로 직접 확인
- 회귀 검사: core 64 assertions, APK v3 signature, permission/policy 검사 통과
- 설치 APK SHA-256: `18e85fc66e2783624854bb1d77568a5b151e571251795d8ea3d5eb82f6781f7d`

새 보존 산출물은 `design-assets/3d/polyman-turntable-soft-final/`, 실기기 영상과 접촉 시트는
`build/airpods-soft-lighting.mp4`, `build/airpods-soft-lighting-contact.png`다. 최초의 강한 그림자 렌더는
실패 이력을 재현할 수 있도록 기존 경로에 남겨 두었다.

## 후속 하이라이트 정정 — Apple 공식 카드와 직접 대조

lower fill판은 검은 외곽을 고쳤으나 넓은 흰 면이 250~255에 붙어 이어버드 머리와 case 뚜껑의 굴곡이
사라졌다. 사용자가 이를 다시 지적한 뒤 Apple Support 104989의 iOS 26 AirPods Pro 연결 카드 이미지를
내려받아 현재 정면 렌더와 나란히 보았다. Apple reference는 몸체를 넓은 연회색으로 두고 반사광을 작은
모서리에 제한했다. reference 파일은 검수용이며 APK에는 넣지 않았다.

tone/exposure만 바꾼 후보, 주광만 줄인 후보, white material tone/roughness 후보를 차례로 기각했다.
특히 주광 감소만으로는 case 앞면의 긴 흰 specular 띠가 남았다. 마지막 세 후보에서 흰 shell의 specular
IOR level을 분리해 0.05까지 낮춘 Q를 선택했다. 선택값은 렌더러 기본값으로 고정했고, 인자를 생략한
재렌더가 선택 Q와 픽셀 단위로 같은지도 확인했다.

- 새 파일: pair 721,252 bytes, case 846,944 bytes
- 파일 계약: 320×320, alpha, 각 180 frames, 33ms/frame, infinite loop
- 실기기: SM-F971N, 7.986초, 246 video frames
- 직접 판정: 넓은 순백 clipping·가로 반사 띠·회전 중 점광 flash 없음; 중간 회색 몸체와 검은 vent 유지
- 자동 검사: core 64 assertions, APK v3 signature, permission/policy 검사 통과
- 설치 APK SHA-256: `9cab5c97d4fb387f31b0e8d327eb6abba574430c03edb4a06ccb65956e374971`

최종 frame과 scene은 `design-assets/3d/polyman-turntable-apple-reference-final/`, 실기기 검수 자료는
`build/airpods-apple-reference-lighting.mp4`와
`build/airpods-apple-reference-lighting-contact.png`에 보존했다. 이전 soft-lighting판도 지우지 않아
“검은 shadow 해결 뒤 white highlight를 놓친” 중간 판단을 다시 추적할 수 있다.
