# 2026-09-07 공개 staging 검증

## 예상

1. 개인 room 전체가 아니라 `iphone-to-galaxy/`만 Git 후보가 된다.
2. 세 앱은 사용자 절대 경로나 기존 keystore 없이 debug build된다.
3. AirPods Glance의 core와 APK 권한 gate가 그대로 통과한다.
4. Git index에는 개인정보, APK, dependency binary, key, 로그와 영상이 없다.

## 실행과 관찰

공개 staging에서 다음을 실행했다.

```sh
./apps/tap-to-top/build.sh
./apps/holiday-sleep/build.sh
./apps/airpods-glance/test-core.sh
./apps/airpods-glance/build.sh
./apps/airpods-glance/verify-apk.sh
```

- Tap to Top: APK 생성, v3 signature 검증 통과
- Holiday Sleep: APK 생성, v2/v3 signature 검증 통과
- AirPods Glance: HiddenApiBypass 6.1 SHA-256 통과, 262,649 assertions 통과, APK 생성·v3 signature 및
  forbidden permission/manifest policy 통과
- compile warning: Java 8 bootstrap classpath 공통 warning, Holiday ConditionProvider와 AirPods
  ConnectionReceiver deprecated API note. 현재 build 실패는 아니며 API 교체 가능성을 후속 조사한다.

Git index는 139개 파일, 약 5 MiB다. runtime 제품 표현에 필요한 PNG 두 개와 animated WebP 두 개만
binary로 추적한다. `build/`, `.local/`, `deps/`는 ignored이며 APK, AAB, AAR, JAR, JKS, keystore,
idsig, MP4/MOV/WebM과 log는 0개다. Markdown 상대 링크, shell syntax, GitHub YAML parse가 통과했다.

개인 문자열 검사에서 원본 AirPods 문서의 개인 제품명과 사용자 호칭, build script의 macOS 절대 SDK
경로가 발견돼 일반 표현과 공통 SDK 탐색으로 바꿨다. 집·회사 주소와 Bluetooth 주소는 공개 후보에
들어오지 않았다. 검사 자체가 금지 패턴을 포함하므로 검사 스크립트 파일만 text scan에서 제외한다.

추가 binary metadata 검사에서 animated WebP는 animation/transparency 외 EXIF·XMP·ICC feature가
없었다. 두 static PNG에는 Blender가 넣은 camera·render date metadata가 있었으나 개인 위치나 작성자
정보는 없었다. 그래도 최소 공개 원칙에 따라 pixel 0개 변경을 확인하며 metadata를 제거했다.

첫 private GitHub CI는 전체 build를 통과했지만 checkout/setup actions의 Node 20 폐기 경고가
나왔다. 공식 latest release를 API로 확인해 checkout v7.0.1, setup-java v6.0.0,
setup-android v4.0.1의 immutable commit SHA로 고정했고 Dependabot monthly 확인을 추가했다.
수정 뒤 두 번째 private GitHub clean-runner 검증은 1분 4초에 public-tree 검사, 세 앱 build,
262,649 assertions와 AirPods APK policy를 모두 통과했고 이전 Node 20 annotation도 사라졌다.

## 피드백 분석과 설계 반영

예상과 실제는 일치했다. 공개 repository와 private room을 별도 디렉터리로 둔 결정은 유지한다.
빌드가 재현돼 source-first 공개는 가능하지만, 앱 공개 license와 GitHub 소유권은 기술 검사가 대신
결정할 수 없다. 따라서 commit/push 전 남은 gate는 다음 두 가지다.

1. 코드 Apache-2.0 / 문서 CC BY 4.0 제안을 소유자가 확정한다.
2. GitHub 계정·저장소 URL과 최초 공개를 source-only로 할지 정한다.

회의 루틴의 녹음 시작과 One Hand Operation+의 방향별 mapping은 기기에서 다시 읽은 기록이 없어
추측하지 않았다. 공개 문서에는 `재확인 필요`로 남겼으며 이는 build gate와 별개의 콘텐츠 보강이다.
