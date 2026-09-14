# 공휴일 수면 연장

<img src="res/drawable-nodpi/ic_launcher_art.png" width="64" height="64" alt="공휴일 수면 연장 아이콘">

**[APK 다운로드](https://github.com/chochoq/iphone-to-galaxy/releases/download/v0.0.1/holiday-sleep-0.0.1.apk)** · [설치 안내](../../docs/INSTALL.md)

Android 15 이상 · 캘린더·방해 금지·알람 및 리마인더 권한 필요

평일에 법정공휴일이 오면 사용자가 정한 시간 동안 방해 금지를 이어 주는 Android 앱입니다.
휴대폰에 동기화된 Samsung 또는 Google의 대한민국 공휴일 캘린더를 읽으며, 공휴일 정보를 받기
위해 인터넷에 연결하지 않습니다.

앱의 방해 금지 규칙은 다음과 같이 설정했습니다.

- 사용자가 맞춘 알람은 울립니다.
- 같은 번호가 15분 안에 다시 전화하면 벨소리가 울립니다.
- 전화 연결, 통화 중 마이크와 Bluetooth는 바꾸지 않습니다.
- 휴대폰을 다시 켜거나 시간과 시간대가 바뀌면 다음 실행 시각을 다시 잡습니다.

## 앱에서 시간 바꾸기

<p>
  <a href="../../docs/images/sleep-home.png"><img src="../../docs/images/sleep-home.png" width="220" alt="자동 연장과 시간 구간 설정"></a>
  <a href="../../docs/images/sleep-time.png"><img src="../../docs/images/sleep-time.png" width="220" alt="시작·종료 시간 휠 편집"></a>
  <a href="../../docs/images/sleep-seconds.png"><img src="../../docs/images/sleep-seconds.png" width="220" alt="초 단위까지 시간 조절"></a>
</p>

자동 연장 설정 · 시간 고르기 · 초 단위 조절 — [캡처 환경](../../docs/images/)

1. ‘연장 시간’을 눌러 시작·종료 시간을 고릅니다. 휠이나 ‘숫자로 입력’을 쓸 수 있습니다.
2. ‘저장’을 누르면 두 시간이 함께 저장되고 다음 실행 일정도 바뀝니다.
3. 저장하지 않고 돌아가려면 ‘취소’를 누릅니다.

기본값은 07:00:15–12:00입니다. 시·분만 바꾸면 기존 초는 그대로 유지합니다.
초도 바꾸려면 ‘초까지 조정’을 켜거나 숫자 입력에 초까지 적습니다.
같은 날 안에서 종료가 시작보다 늦어야 합니다. 자정을 넘기는 평소 일정과 주말 일정은
Samsung 모드에서 설정합니다.

자동 연장을 꺼둔 채 시간을 저장해도 기능이 저절로 켜지지는 않습니다. Samsung 수면 모드와
시간이 자동으로 맞춰지는 것도 아닙니다. **기상 알람을 만들거나 늦추는 앱이 아니며**, 시계 앱의
‘공휴일에는 알람 끄기’는 별도로 설정해야 합니다.

캘린더·권한·다음 예약은 ‘권한 및 상세 정보’에서 확인합니다.
휴대폰의 대한민국 공휴일 캘린더가 동기화돼 있어야 합니다.

[설계와 작업 기록](docs/design/INDEX.md) · [실기기 화면 검증](docs/results/2026-09-08-calm-interface.md)
· [아이콘 제작 기록](assets/icon/README.md)

## 확인한 환경

- Samsung Galaxy Z Fold8 (SM-F971N)
- Android 17
- One UI 9.0

다른 기기와 One UI에서는 캘린더를 찾는 방법과 방해 금지 규칙이 함께 적용되는 방식이 다를 수
있습니다.

## 빌드

~~~sh
sh test-core.sh
./build.sh
~~~

Android SDK Platform 36, Build Tools 36.0.0과 JDK가 필요합니다. SDK가 기본 위치에 없다면
ANDROID_SDK_ROOT를 지정합니다. build/holiday-sleep.apk는 개발자의 테스트용 키로 서명됩니다.
공식 APK는 [GitHub Releases](https://github.com/chochoq/iphone-to-galaxy/releases/tag/v0.0.1)에서 받습니다.
[폰에서 설치하기](../../docs/INSTALL.md) · [배포용 서명과 업데이트](../../docs/RELEASING.md)

Android 15 이상이 필요합니다. 이전 빌드의 설치 조건은 Android 8 이상이었지만,
실제로 사용하는 방해 금지 API의 버전에 맞춰 첫 APK 배포에서 바로잡았습니다.

## 권한과 삭제 방법

- 캘린더 읽기: 오늘이 대한민국 공휴일인지 휴대폰 안에서 확인합니다.
- 방해 금지 설정: ‘공휴일 수면 연장’이라는 자동 규칙을 켜고 끕니다.
- 정확한 알람과 부팅 알림 받기: 시작·종료 시각에 실행하고 재부팅 뒤 다시 예약합니다.

삭제하기 전 앱을 열어 ‘자동 연장 사용’을 끄고, 방해 금지 자동 규칙도 꺼졌는지 확인합니다.
기본 Samsung 수면 모드는 이 앱과 별개이므로 삭제되지 않습니다. 설정 순서는
[문제 문서](../../problems/holiday-aware-sleep.md)에 자세히 적었습니다.
