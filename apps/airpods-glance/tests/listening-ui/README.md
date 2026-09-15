# 소음 제어 화면 검사

제품의 View 클래스만 담는 별도 검사 앱이다. Bluetooth 권한은 없고 실제 제어 controller도
연결하지 않는다. 폰 전체 글자 크기를 바꾸지 않고 검사 Activity의 Configuration만 바꾼다.

## 실행 순서

1. 제품 빌드 디렉터리와 아직 없는 검사 출력 경로를 넘겨
   `node tools/build-listening-ui-test.mjs <product-build-dir> <new-test-output-dir>`로 빌드한다.
2. 검사 APK를 설치한다. 패키지는 `com.chocho.listeninguitest`이며 본앱과 다르다.
3. **먼저 홈으로 이동한다:** `adb shell input keyevent KEYCODE_HOME`.
4. `adb shell am instrument -w com.chocho.listeninguitest/.Checks`를 실행한다.
   최종 `PASS 3415 checks`와 `INSTRUMENTATION_CODE: -1`을 모두 확인한다.
   D-022부터 적응형 대기/확인 상태를 더해 12개 너비·글씨 조합 × 8개 상태를 검사한다.
5. 제거 전에 검사 앱의 외부 파일 디렉터리
   `/sdcard/Android/data/com.chocho.listeninguitest/files/`에서 `glyph-review.png`와
   `glyph-metrics.txt`를 `adb pull`로 로컬 검증 폴더에 보관한다. 렌더 자료는 직접 열어 확인한다.
6. 끝나면 검사 앱만 `adb uninstall com.chocho.listeninguitest`로 제거한다.
   본앱은 삭제하지 않는다. 새로 만든 검사 데이터는 이 삭제로 지워지며 재빌드로 다시 생성할 수 있다.
7. 본앱을 다시 열고, 개인 기기명이 있는 실제 화면 자료는 비공개 artifacts에만 보관한다.

## 홈으로 먼저 가는 이유 — 2026-09-15 정정

에어팟이 연결된 본앱 위에서 처음 검사를 실행하자, 검사 Activity가 끝날 때마다 아래의 본앱이
잠깐 전면으로 복귀했다. 검사 앱에는 Bluetooth 권한이 없어도 **본앱의 onResume 읽기**는 실행됐다.
이때 짧은 window_open/close 쌍이 남았으며 모드 변경 쓰기는 없었다.

따라서 ‘검사 앱에 권한이 없다’를 ‘폰 전체에서 Bluetooth 읽기가 없었다’와 혼동하면 안 된다.
홈에서 다시 실행한 2,384개 검사는 통과했고, 그 실행 구간에는 본앱의 새 연결이 없었다.
이 재실행을 화면 검사 근거로 삼는다. 화면 검사는 실제 Bluetooth 전환·음악 청취의 대체 증거가 아니다.

이 숫자는 D-021 당시 여섯 상태 검사 기록이다. D-022의 적응형 활성화를 포함한 여덟 상태 검사는
홈에서 시작해 3,354개를 통과했다. 두 실행을 같은 검사 수로 덮어쓰지 않는다.

## D-023 — 실제 아이콘 렌더 검사 61개 추가

`GlyphChecks`는 제품 View의 아이콘을 Android Canvas로 26/68/156px에 직접 그린다.
도형 누락·잘림·색·공통 사람 실루엣과 장식 분리를 검사한다. 그림을 복제한 시험용 painter는 없다.
첫 버전은 큰 크기의 도형 분리만 요구해 3,409개를 통과했지만, 26px에서 일부 장식이 어깨에 닿았다.
이를 수정하고 세 크기 모두 분리 조건으로 검사한 최종 결과는 **3,415개**다.
검사 통과는 시각적 의미나 아름다움의 증거를 대신하지 않는다. 공식 참조와 실기 화면 비교를 따로 남긴다.

## 문서용 선택 상태 자료

같은 검사 APK에 `.Gallery` instrumentation이 있다. 홈에서
`adb shell am instrument -w com.chocho.listeninguitest/.Gallery`를 실행하면 현재 제품 View의
노캔·적응형·주변음 선택 상태를 각각 PNG로 출력한다. 72개 검사와 종료 코드 -1을 확인한다.
위의 외부 파일 디렉터리에 `air-listening-anc.png`, `air-listening-adaptive.png`,
`air-listening-transparency.png`, `gallery-evidence.txt`가 생긴다. 제거 전에 가져온다.

이 경로는 controller를 연결하지 않으며 모드 상태는 시험용 입력이다. 개인정보·배터리 값은 넣지 않는다.
제품 코드의 소음 제어 영역 렌더이지 실제 Bluetooth 전환 캡처나 전체 MainActivity 캡처가 아니다.
문서에서도 그 차이를 표시한다. 실행에 필요한 빌드 입력은 현재 제품 `app-classes.jar`이며
이전 APK의 빌드 디렉터리를 재사용해 오래된 UI를 내보내지 않도록 확인한다.
