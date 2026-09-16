# 세 앱 아이콘

<p>
  <img src="../apps/holiday-sleep/res/drawable-nodpi/ic_launcher_folded_v3.png" width="112" height="112" alt="달력 이불로 표현한 공휴일 수면 연장 아이콘">
  <img src="../apps/tap-to-top/res/drawable-nodpi/ic_launcher_folded_v3.png" width="112" height="112" alt="휘어 올라가는 피드로 표현한 맨 위로 톡 아이콘">
  <img src="../apps/airpods-glance/res/drawable-nodpi/ic_launcher_folded_v3.png" width="112" height="112" alt="접힌 케이스로 표현한 에어팟 한눈에 아이콘">
</p>

공휴일의 휴식은 달력 이불, 스크롤은 휘어 올라가는 피드, AirPods는 두 이어버드를 감싼 케이스로 표현했습니다.
아이보리색 면과 접힌 곡선을 공통으로 쓰고 앱마다 색을 달리했습니다. v0.0.2부터 사용하는 아이콘입니다.

## 자산과 출처

세 앱의 `res/drawable-nodpi/ic_launcher_folded_v3.png`는 이 프로젝트에서 이미지 생성 도구로 만든
1254×1254 불투명 PNG입니다. 같은 폴더의 이전 `ic_launcher_art*.png`도 같은 방식으로 제작했습니다.
Apple·Samsung 공식 이미지나 제3자 3D 모델의 렌더가 아닙니다.

아이콘의 라이선스는 [파일별 범위](../LICENSES/README.md)를 따릅니다.
에어팟 연결 카드의 회전 제품 모델은 런처 아이콘과 출처가 다르며,
[제3자 고지](../apps/airpods-glance/THIRD_PARTY_NOTICES.md)를 확인해야 합니다.

## Android에 적용한 방식

바깥 모서리는 Android가 자릅니다. 원본에는 둥근 테두리를 넣지 않았습니다.
Adaptive icon에서 그림이 확대되어 잘리지 않도록 배경 XML에 네 변 1/6의 여백을 두고,
바깥을 비슷한 배경색으로 채웠습니다. 전경은 투명하므로 레이어별 독립 시차 효과는 없습니다.

`ic_launcher_folded_background`와 직접 작성한 단색 벡터 `ic_launcher_folded_mono`를 사용합니다.
테마 아이콘을 켜면 원본 색 대신 시스템 팔레트가 적용됩니다.

## 확인 범위

Fold8·Android 17·One UI 9.0의 시스템 앱 정보 화면에서 새 아이콘을 확인했습니다.
별도 Android 렌더에서는 작은 크기의 여백과 컬러·단색 자산을 검사했습니다.
다른 런처·아이콘 팩과 실제 폰의 테마 아이콘 표시는 추가 확인이 필요합니다.
