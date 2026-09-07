# 보안과 개인정보

## 기본 경계

- 앱 실행 중 광고, 분석, crash reporting 또는 원격 서버 통신을 하지 않습니다.
- Bluetooth 주소, raw AirPods packet, 캘린더 일정 내용과 접근성 화면 내용을 외부로 보내지 않습니다.
- 릴리스·debug 서명 키, 기기 로그와 개인 위치는 저장소에 올리지 않습니다.
- 권한은 문제 문서와 앱 README에서 이유·부작용·철회 방법을 설명합니다.

## 민감한 기능

Tap to Top의 접근성 서비스는 다른 앱의 스크롤 가능한 노드를 찾고 한 번의 동작을 실행합니다.
AirPods Glance의 직접 배터리 읽기는 숨은 Bluetooth API fallback을 포함한 실험 기능입니다.
Holiday Sleep은 로컬 캘린더 읽기와 방해 금지 규칙 관리 권한을 사용합니다. 소스를 검토하고
위험을 이해하지 못한 상태에서는 설치하지 않는 편이 안전합니다.

보안 문제를 공개 issue로 올리면 다른 사용자의 정보가 드러날 수 있습니다. GitHub 저장소를
만들 때 private vulnerability reporting을 켠 뒤 그 경로를 이 문서에 추가합니다. 실제 주소가
정해지기 전에는 개인정보·Bluetooth 주소·캘린더 제목·로그 전문을 issue에 첨부하지 마세요.
