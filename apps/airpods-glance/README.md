# 에어팟 한눈에

Galaxy에 연결된 AirPods의 배터리를 앱, 알림과 홈 화면 위젯에서 확인하는 개인용 Android
앱입니다. AirPods가 새로 연결되면 큰 제품 화면도 띄울 수 있습니다. 광고와 분석 기능은 없으며
앱을 사용하는 동안 서버에 접속하지 않습니다.

## 확인한 환경

- Samsung Galaxy Z Fold8 (SM-F971N)
- Android 17
- One UI 9.0

이 기기에서 AirPods 연결과 음악 재생 유지, 왼쪽·오른쪽·케이스 배터리, 4×1 위젯과 큰 연결 화면을
확인했습니다. 다른 Galaxy, Android와 One UI에서는 Bluetooth 내부 기능과 화면 배치가 달라질 수
있습니다.

## 지금까지 확인한 결과

- JVM 핵심 검사 262,649개를 통과했습니다.
- APK 빌드, 서명과 권한 검사를 통과했습니다.
- AirPods를 세 번 새로 연결해 AAP 배터리 수신과 30초 동안의 음악 연결 유지를 확인했습니다.
- 왼쪽 이어버드를 착용하고 오른쪽을 케이스에 둔 상태에서 좌우 92%, 오른쪽 충전 중, 케이스 84%를 확인했습니다.
- Mac의 AirPods 자동 전환을 제한한 뒤 Galaxy에 자동으로 연결되는 것을 확인했습니다.
- 연결 화면은 본문이나 닫기 버튼을 누를 때까지 남고, AirPods 연결이 끊어지면 사라졌습니다.
- 프레임 사이를 변형해 만든 첫 애니메이션은 제품 모양이 일그러져 폐기했습니다. 현재 영상은 CC BY 4.0 3D 모델을 180장에서 직접 렌더링했습니다.
- 수면 모드 중 무음 연결 화면은 설치를 마쳤지만 새 물리 연결로 한 번 더 확인해야 합니다.

## 다시 빌드하고 검사하기

~~~sh
./test-core.sh
./build.sh
./verify-apk.sh
~~~

Android SDK Platform 36, Build Tools 36.0.0, JDK, curl과 unzip이 필요합니다. SDK가 기본 위치에
없다면 ANDROID_SDK_ROOT를 지정합니다.

빌드할 때 LSPosed HiddenApiBypass 6.1을 Maven Central에서 내려받고 SHA-256 값이 맞는지
확인합니다. APK 안에는 HiddenApiBypass의 Apache 2.0 전문과 AirPods 렌더의 CC BY 4.0 전문,
저작자 고지도 함께 들어갑니다.

만들어진 APK는 각 개발자의 확인용 키로 서명되며 공개 배포용이
아닙니다.

설계와 판단은 [설계 인덱스](docs/design/INDEX.md), 구현 항목은 [작업 기록](docs/tasks/TASKS.md),
기기에서 시험한 내용은 docs/results/에 있습니다.

비교에 사용한 iPhone 촬영물과 Apple 공식 이미지, 다른 앱의 APK는 재배포 권리를 확인할 수 없어
저장소에 넣지 않았습니다.

## 권한과 개인정보

인터넷, 위치, 접근성과 마이크 권한은 요청하지 않습니다. 다른 앱 위 표시 권한은 큰 연결 화면을
보여주는 기능을 켰을 때만 필요합니다. Bluetooth 주소와 원시 패킷은 화면이나 로그에 남기지
않습니다.

CAPod, Podsify와 GreenPods는 AirPods 통신 방식을 교차 확인하는 자료로만 살펴봤으며 코드는
복사하지 않았습니다.

제품 회전 영상은 polyman의 ‘Airpods Pro With Magsafe Charging Case Ios15’ 3D 모델을 바탕으로
만들었습니다. 저작자와 라이선스는
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)에 표시했습니다.

되돌리려면 앱에서 감시와 큰 연결 화면을 끕니다. 다른 앱 위 표시, 알림과 근처 기기 권한을
철회하고 위젯을 지운 뒤 앱을 삭제합니다. Bluetooth 페어링은 자동으로 삭제하지 않습니다.
기능의 위험과 한계는 [문제 문서](../../problems/airpods-on-galaxy.md)에 있습니다.
