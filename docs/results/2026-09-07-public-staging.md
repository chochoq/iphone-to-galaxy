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

**2026-09-07 후속 정정:** 위 두 항목은 이 검증을 실행했을 때의 미결정 상태입니다. 이후 저장소와
저자 이름, source-first 범위를 확인했고, 코드 GPL-3.0-or-later와 문서 CC BY-SA 4.0을
선택했습니다. 현재 남은 결정은 비공개 저장소를 언제 공개로 바꿀지입니다.

후속 확인에서 저장소 소유자는 `chochoq`가 자신의 공개 GitHub nickname이라 유지해도 된다고
결정했다. 이에 따라 author alias와 `com.chocho...` namespace는 의도적 공개 정보로 확정했고 개인
email은 noreply로 유지한다. 남은 사람의 결정은 프로젝트 license와 Public 전환 시점이다.

회의 루틴의 녹음 시작과 One Hand Operation+의 방향별 mapping은 기기에서 다시 읽은 기록이 없어
추측하지 않았다. 공개 문서에는 `재확인 필요`로 남겼으며 이는 build gate와 별개의 콘텐츠 보강이다.

## 기존 프로젝트 조사 후 공개 주장 수정

소유자가 개별 기능과 비슷한 GitHub가 이미 많을 것 같다고 지적해 repository search와 현재 README,
license를 다시 확인했다. AirPods on Android에는 LibrePods·CAPod·OpenPods 등 규모와 기능이 훨씬 큰
GPL-3.0 프로젝트가 있고, Podsify는 Apache-2.0으로 battery·widget을 제공한다. 상태 표시줄 tap에는
반복 swipe 기반 `twelvehouse/TapToTop`이 있었다.

따라서 `우리 앱이라서 가치 있다`는 암묵적 전제를 제거했다. README와 문제 문서는 기존 대안을 먼저
보여주고, 우리 구현은 광고 없는 개인 요구, 단일 gesture의 interruptibility, DND·상태 이미지 검증과
실패 history가 남는 사례로 재정의했다. 전체 iPhone→Galaxy 전환 지도와 한국 공휴일 수면 조합은 이번
검색에서 뚜렷한 대응물을 찾지 못했지만 독창성이나 부재를 증명했다고 쓰지 않는다.

## 공개 문서의 한국어를 다시 쓴 결과

소유자가 im-not-ai와 fluent-korean을 읽어보라고 한 뜻은 경쟁 프로젝트를 비교하라는 요청이
아니었습니다. 두 저장소의 한국어 표현을 살펴보고, 우리가 만든 문서에서 인공지능 특유의 말투를
고치라는 요청이었습니다. 첫 답변에서는 이 의도를 잘못 이해했고, 설명을 들은 뒤 작업 범위를
바꿨습니다.

README, 문제 문서 여섯 개, 설정 문서 일곱 개, 앱 README 세 개와 호환성·보안·기여·라이선스·기존
프로젝트 조사 문서를 다시 썼습니다. 추상적인 프로젝트 정의보다 아이폰에서 실제로 불편했던
장면을 먼저 놓았습니다. 한국어로 설명할 수 있는 `battery·widget·popup`, `mapping`, `gate` 같은
명사 나열도 줄였습니다. 기존 앱, 실패한 방법, 현재 선택과 미확인 범위의 순서는 유지했습니다.

im-not-ai의 정량 측정기를 보조 검사로 실행했을 때 README, 기존 프로젝트 조사와 세 가지 핵심 문제
문서가 모두 `low`로 나왔습니다. 이 결과만으로 자연스러운 한국어라고 단정하지 않습니다. 수치가
잡지 못하는 어조와 개인 경험은 사람이 다시 읽어 판단해야 합니다.

첫 커밋 뒤 사용자의 피드백에 따라 긴 문단도 다시 나눴습니다. 소스 코드에서 보기 좋게 줄만
바꾸는 방식이 아니라, GitHub 화면에서도 실제 문단으로 보이도록 내용이 전환되는 곳에 빈 줄을
넣었습니다. README의 호환성 설명, AirPods 앱과 문제 문서, 기존 프로젝트 조사와 수면 안내에서
세 문장 이상 이어지던 문단을 중심으로 손봤습니다.

## 회의 녹음 전제 수정

현재 사용자의 회사는 회의록을 위해 녹음을 권장하지만 다른 회사에는 금지나 별도 동의 절차가
있을 수 있다는 피드백을 받았습니다. 기존 문서는 기기 권한과 Bluetooth 부작용만 다뤘고 회사 규정,
참석자와 녹음 파일의 처리 조건을 빠뜨렸습니다.

회의 문제 문서와 설정 문서에 회사 규정, 참석자 안내·동의, 외부 회의와 비밀유지계약, 개인정보,
저장·접근·보관·삭제 확인 항목을 추가했습니다. 조건이 불명확하면 녹음 앱을 여는 동작을 제외하도록
수정했으며, 특정 지역의 법률상 허용 여부는 확인하지 않았으므로 법률 자문으로 표현하지 않았습니다.
