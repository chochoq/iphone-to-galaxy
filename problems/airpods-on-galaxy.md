# Galaxy에서 AirPods 상태를 한눈에 보기

## 1. iPhone에서 익숙했던 경험

AirPods 케이스를 열거나 이어버드를 착용하면 큰 제품 카드와 왼쪽·오른쪽·케이스 배터리가 보이고,
기기 상태에 맞는 제품 구성이 자연스럽게 움직인다. Galaxy에서는 표준 Bluetooth 연결과 오디오는
되지만 같은 시각 피드백과 배터리 구성이 기본 제공되지 않는다.

## 2. 기존 앱을 떠난 이유

기존 앱은 빠른 출발점이지만 연결할 때 광고나 반복 안내가 나타나는 경험이 있었다. 이 프로젝트는
광고·분석·인터넷 권한 없이 현재 기기에서 필요한 배터리, 위젯, 알림과 연결 카드만 제공하는지
검증하기 위해 직접 구현했다. 다른 앱의 소스나 디자인 자산을 복사하지 않았다.

## 3. 선택한 해결책

[`apps/airpods-glance`](../apps/airpods-glance/)는 페어링·연결된 AirPods를 로컬에서 식별하고 BLE
advertisement와 명시적 AAP 배터리 읽기를 상태 snapshot으로 합친다. 앱, 4×1 위젯, ongoing
notification과 사용 중 화면의 큰 무음 overlay가 같은 snapshot을 사용한다. 큰 카드는 본문이나
X를 누르거나 연결을 끊을 때까지 유지된다.

제품 애니메이션은 생성 이미지 프레임 사이를 변형하지 않는다. 하나의 CC BY 4.0 3D 모델을 고정
카메라로 180장 렌더해 turntable로 재생한다. 케이스만 열어 감지되는 상태와 이어버드 사용 상태의
구성·회전 정책은 별도 상태 모델로 둔다. 구체적인 iOS 관찰, 반례와 수정은
[설계 인덱스](../apps/airpods-glance/docs/design/INDEX.md)에 보존했다.

## 4. 권한과 신뢰 경계

Bluetooth scan/connect, 알림, connected-device foreground service, 부팅 수신과 선택적 다른 앱 위
표시를 쓴다. 인터넷·위치·마이크·접근성 권한은 없다. Bluetooth 주소와 raw packet은 UI나 로그에
출력하지 않는다. 앱 실행 중 서버 통신과 telemetry가 없다.

AAP 직접 읽기는 Android 공개 동작만으로 충분하지 않을 때 HiddenApiBypass와 숨은 Bluetooth API
fallback을 쓰는 실험 기능이다. 자동 연결 세션마다 한 번, 최대 30초로 제한했지만 OS 업데이트로
깨지거나 Bluetooth 안정성에 영향을 줄 가능성을 일반 앱보다 엄격히 봐야 한다.

## 5. 알려진 한계

- AirPods·case의 firmware와 광고 시점에 따라 일부 배터리가 늦거나 unknown일 수 있다.
- Mac·iPhone·iPad의 자동 전환 우선권은 이 앱이 통제하지 않는다. 다른 Apple 기기가 먼저 잡으면
  Galaxy에 자동 연결되지 않을 수 있다.
- 방해 금지 중에도 화면이 켜지고 잠금 해제된 경우 카드는 무음으로 보이지만 화면을 깨우거나 잠금
  위에 표시하지 않는다.
- Galaxy Z Fold8 이외 기기와 다른 AirPods 세대는 미검증이다.

## 6. 원상복구

앱에서 자동 감시와 큰 연결 카드를 끈 뒤, 다른 앱 위 표시·알림·근처 기기 권한을 철회한다. 홈 화면
위젯을 제거하고 앱을 삭제한다. Bluetooth 페어링 자체는 별개이므로 AirPods 연결 기록까지 지우고
싶을 때만 Bluetooth 설정에서 페어링을 해제한다.

## 7. 공개 수준

JVM core 262,649 assertions, APK permission policy, `SM-F971N` 실연결·음악 유지·배터리 표시를
확인했다. 그러나 숨은 API와 단일 AirPods/단일 Galaxy 조합에 의존하므로 초기 공개는 설치 파일보다
source-first인 `실험적·단일 기기 검증`으로 분류한다.
