# 소음 제어 위젯·순서 편집기 검사

실제 제품 View/RemoteViews를 `com.chocho.listeningwidgetrender`라는 별도 패키지에서 실행합니다.
요청 권한은 없으며 Bluetooth 명령을 보내지 않습니다. 테스트 폰이나 격리 에뮬레이터를 사용하세요.
개인 제품 앱의 설정을 초기화하거나 테스트 데이터를 넣지 않습니다.

저장소 루트에서 먼저 앱을 빌드한 뒤 존재하지 않는 출력 폴더를 지정합니다.
SDK Build Tools 36.0.0·Android 36 플랫폼과 JDK가 필요합니다. SDK 위치는 `ANDROID_SDK_ROOT`로 지정할 수 있습니다.

```sh
sh apps/airpods-glance/build.sh
sh apps/airpods-glance/test-listening-widget.sh
sh apps/airpods-glance/tests/listening-widget-render/build.sh /tmp/listening-widget-render-test
adb -s YOUR_TEST_DEVICE install /tmp/listening-widget-render-test/test.apk
adb -s YOUR_TEST_DEVICE shell am instrument -w com.chocho.listeningwidgetrender/.OrderChecks
adb -s YOUR_TEST_DEVICE shell am instrument -w com.chocho.listeningwidgetrender/.Checks
adb -s YOUR_TEST_DEVICE shell am instrument -w com.chocho.listeningwidgetrender/.DocGallery
```

- `OrderChecks`: 36개 크기/글씨/높이/선택 조합, 실제 터치 정렬, 다중 터치·취소, 접근성 액션,
  키보드, 저장·재생성·외부 설정 충돌을 검사합니다.
- `Checks`: 320개 RemoteViews 화면과 설정 분리, 최소 터치 영역, 아이콘 비트맵, 저장/취소를 검사합니다.
- `DocGallery`: 실제 제품 UI의 위젯 비교 4장과 설정 예시 2장을 출력합니다. 실제 연결 시험은 아닙니다.

`am instrument`의 종료 코드만 보지 말고 출력에 `PASS`가 있고 `FAIL`이 없는지 확인하세요.
테스트는 테스트 패키지 안의 설정을 기본값으로 되돌립니다. 결과 파일은 아래에 있습니다.

```sh
adb -s YOUR_TEST_DEVICE pull /sdcard/Android/data/com.chocho.listeningwidgetrender/files/readme-images ./listening-readme-images
adb -s YOUR_TEST_DEVICE pull /sdcard/Android/data/com.chocho.listeningwidgetrender/files/order-checks ./listening-order-checks
adb -s YOUR_TEST_DEVICE uninstall com.chocho.listeningwidgetrender
```

마지막 명령은 검사용 앱과 그 앱의 테스트 설정만 삭제합니다. 제품 앱은 삭제하지 않습니다.
TalkBack 액션 API 검사는 실제 음성 탐색 사용자 시험과 다릅니다. 삼성 홈·잠금·실물 AirPods 전환도 별도 확인이 필요합니다.
