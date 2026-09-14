# 세 앱 아이콘

<p>
  <img src="../apps/holiday-sleep/res/drawable-nodpi/ic_launcher_folded_v3.png" width="112" height="112" alt="달력 이불로 표현한 공휴일 수면 연장 아이콘">
  <img src="../apps/tap-to-top/res/drawable-nodpi/ic_launcher_folded_v3.png" width="112" height="112" alt="휘어 올라가는 피드로 표현한 맨 위로 톡 아이콘">
  <img src="../apps/airpods-glance/res/drawable-nodpi/ic_launcher_folded_v3.png" width="112" height="112" alt="접힌 케이스로 표현한 에어팟 한눈에 아이콘">
</p>

2026-09-14, v0.0.2. 아이보리색 면과 접힌 곡선을 공통으로 쓰고 앱마다 색과 형태를 달리했습니다.
앱 화면이나 에어팟 연결 카드의 제품 모델을 바꾸는 작업은 아닙니다.

## 1.1.1 기호의 색만 바꾸던 접근에서 출발

이전에는 달과 화살표의 색·재질을 다듬었지만 기본 템플릿 같다는 피드백을 받았습니다.
기호를 유지한 채 장식을 바꾸기보다 기능에서 새 형태를 찾았습니다.
공휴일의 휴식은 달력 이불로, 스크롤은 휘어 올라가는 피드로 표현했습니다.
에어팟은 두 이어버드를 감싸는 케이스로 같은 질감을 이어 갔습니다.

## 1.1.2 시안과 Android 아이콘 사이

그림은 1254×1254 불투명 PNG로 생성했습니다. Android가 바깥 모서리를 자르므로
원본에 둥근 테두리를 그려 넣지 않았습니다.

Adaptive icon은 원본 레이어를 확대해서 표시합니다. 그림을 그대로 넣으면 가장자리가 잘려서,
배경 XML의 네 변에 1/6씩 여백을 주고 바깥은 비슷한 배경색으로 채웠습니다.
테마용 단색 아이콘은 새 형태를 단순화한 벡터입니다. 이전 이미지와 벡터는 기록으로 보존합니다.

## 1.1.3 확인한 것과 남은 것

Galaxy Z Fold8 / Android 17 / One UI 9.0에서 세 앱을 삭제 없이 업데이트하고 실행했습니다.
시스템 앱 정보에 표시된 새 아이콘을 직접 확인했습니다. 앱 식별 정보·기존 권한과
맨 위로 톡 접근성 서비스 연결은 유지됐습니다. 개인 설정이나 캡처는 저장소에 넣지 않았습니다.

별도 Android 15 가상 기기에서 공식 0.0.1 APK를 0.0.2 후보로 업데이트해,
시험용으로 넣은 시간·스크롤·카드 설정이 유지되는 것을 확인했습니다.
실제 Android drawable로 48·72·144px에서 여백과 단색 리소스 18개 항목을 검사하고,
컬러·단색 렌더를 직접 확인했습니다. 사용자 폰의 테마 설정은 바꾸지 않았습니다.
다른 Galaxy나 다른 런처, 사용자 지정 아이콘 팩에서의 표시는 아직 검증하지 않았습니다.

## 제작 기록과 라이선스

내장 이미지 생성 도구로 만든 그림이며 Apple·Samsung 공식 아이콘이 아닙니다.
그림과 Android XML의 라이선스는 [파일별 라이선스 범위](../LICENSES/README.md)를 따릅니다.

- [공휴일 수면 연장 프롬프트](../apps/holiday-sleep/assets/icon/FOLDED.md)
- [맨 위로 톡 프롬프트](../apps/tap-to-top/assets/icon/FOLDED.md)
- [에어팟 한눈에 프롬프트](../apps/airpods-glance/assets/icon/FOLDED.md)
