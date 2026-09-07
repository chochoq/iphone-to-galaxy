# 에어팟 한눈에

Samsung Galaxy에서 선택한 AirPods가 연결된 동안 Bluetooth battery를 읽어 앱, 알림,
홈 화면 widget에 표시하는 로컬 전용 개인 앱이다.

## 검증 환경

- Samsung Galaxy Z Fold8 (`SM-F971N`)
- Android 17
- One UI 9.0

이 환경에서 직접 검증했다. 다른 Galaxy, Android 및 One UI 버전에서는 Bluetooth 내부 API,
overlay와 foldable 화면 레이아웃이 다를 수 있다.

## 현재 상태

- JVM core tests: 262,649 assertions 통과
- APK build/sign/policy check: 통과
- SM-F971N 설치와 4×1 widget: 완료
- 명시적 AAP 시험: 독립 연결 3회에서 배터리 수신과 ACL/A2DP 30초 유지
- 실제 case/charging: 왼쪽 착용·오른쪽 case에서 좌우 92%·오른쪽 충전·case 84% 확인
- 자동 AAP: 연결당 한 번·30초 제한 구현·설치, 무버튼 자동 battery 갱신 실측 완료
- 선행 Bluetooth 연결: Mac의 AirPods 자동 전환 제한 뒤 Galaxy remote 자동 연결 실측 완료
- 큰 연결 카드: Apple 실제 화면 기반 밝은 하단 시트로 재설계. 실제 연결과 미리보기 모두
  본문 tap 또는 X까지 유지되고, 연결 해제 때 정리됨. 생성 keyframe 보간 애니메이션은 실기기에서
  형상 변형이 확인돼 철회했으며, CC BY 4.0 단일 3D 모델을 직접 렌더한 180-frame·약 30fps
  turntable로 교체해 SM-F971N의 8초 영상과 본문/X 닫기를 검증함. 화면을 보는 중의 무음 카드는
  수면 DND에도 표시하도록 수정·설치했으며 새 물리 연결 1회 확인이 남음

## 재현 명령

```sh
./test-core.sh
./build.sh
./verify-apk.sh
```

Android SDK Platform 36, Build Tools 36.0.0, JDK, `curl`과 `unzip`이 필요합니다. SDK가 기본 위치에
없다면 `ANDROID_SDK_ROOT`를 지정합니다. build는 LSPosed HiddenApiBypass 6.1 AAR을 Maven Central에서
내려받아 고정 SHA-256을 검사합니다. 생성 APK는 `.local/` debug key용이며 공개 배포 서명이 아닙니다.

설계와 판단 이력은 [docs/design/INDEX.md](docs/design/INDEX.md), 세분화된 구현 상태는
[docs/tasks/TASKS.md](docs/tasks/TASKS.md), 기기 시험 결과는 `docs/results/`에 있다.

설계 문서에는 비교 과정의 텍스트 기록을 보존하지만, 개인정보와 재배포 권리가 불명확한 iPhone
촬영·Apple 공식 캡처·다른 앱 APK 및 자산은 이 저장소에 포함하지 않는다.

## 신뢰 경계

앱에는 Internet, 위치, accessibility, microphone 권한이 없다. 선택적인 overlay 권한은
화면 사용 중 연결 카드와 사용자가 직접 누른 미리보기에만 쓴다. 카드는 본문 tap 또는 X로 닫히며
연결 해제 때 정리된다. Bluetooth 주소와 raw packet은 UI와 로그에 출력하지
않는다. CAPod(GPL-3.0), Podsify(Apache-2.0),
GreenPods(MIT)는 공개 protocol 동작을 교차 확인하는 근거로만 살폈고 GPL 코드를 복사하지
않았다. 제품 turntable은 polyman의 `Airpods Pro With Magsafe Charging Case Ios15` CC BY 4.0
모델을 바탕으로 하며 정확한 표시는 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)에 있다.

## 원상복구

앱에서 감시와 큰 연결 카드를 끄고 다른 앱 위 표시·알림·근처 기기 권한을 철회한다. 위젯을 제거하고
앱을 삭제한다. Bluetooth pair는 자동으로 지우지 않는다. 위험과 호환성 범위는
[문제 문서](../../problems/airpods-on-galaxy.md)에 있다.
