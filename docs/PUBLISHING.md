# GitHub 공개 전략

## 1. 첫 공개의 정체성

저장소 이름은 `iphone-to-galaxy`를 제안한다. 소개 문장은 다음 한 문장으로 고정한다.

> iPhone 장기 사용자가 익숙한 행동을 Galaxy에서 기본 설정부터 직접 앱까지 안전하고 되돌릴 수
> 있게 번역하며, 성공과 실패를 함께 기록하는 오픈소스 프로젝트.

앱 모음, iOS theme 또는 Apple UI 복제 프로젝트로 소개하지 않는다. 검색 입구는 `tap status bar to
scroll`, `holiday sleep`, `AirPods battery on Galaxy` 같은 문제이고 앱은 여러 해결 수준 중 마지막
수단이다.

## 2. 0.1 공개 범위

### 포함

- 문제 지도와 해결 단계
- iPhone 14 Pro / iOS 26.6.1 경험 기준
- Galaxy Z Fold8 `SM-F971N` / Android 17 / One UI 9.0의 단일 기기 검증 경계
- Tap to Top, Holiday Sleep, AirPods Glance의 buildable source
- 세 앱의 설계, 세분화 task, 반증과 실기기 결과
- 수면·회의·생활 루틴, 한손 제스처와 재난문자 field notes
- 권한·data·rollback, 호환성 제보와 CI

### 제외

- APK release와 production signing key
- private room, raw chat, device log와 screen recording
- 집·회사 위치, SSID, Bluetooth 주소, 개인 제품명과 account
- Apple 공식 이미지·촬영 영상, MaterialPods와 다른 앱의 APK·자산
- 개발 중 임시 입력기 `routine-input-helper`

임시 입력기는 사용자 가치가 있는 독립 도구가 아니라 설정을 돕던 개발 장비다. 문제와 안전 경계를
다시 정의하기 전에는 공개 앱 개수를 늘리기 위해 넣지 않는다.

## 3. 저장소 공개 순서

1. 소유자가 코드·문서 라이선스를 확정한다.
2. `LICENSE-PROPOSAL.md`를 실제 전문과 `NOTICE`로 교체한다.
3. GitHub에 public 빈 저장소를 만들고 private vulnerability reporting을 켠다.
4. 현재 `main`을 push하고 CI가 깨끗한 runner에서도 통과하는지 확인한다.
5. repository description, topics와 social preview를 설정한다.
6. `v0.1.0-source-preview` tag를 만든다. APK asset은 첨부하지 않는다.
7. 첫 issue로 다른 Galaxy의 compatibility report를 요청한다.

권장 topics: `android`, `samsung`, `galaxy`, `one-ui`, `iphone-migration`, `good-lock`,
`accessibility`, `airpods`, `open-source`.

## 4. 운영 구조

Issue label은 문제 종류와 검증 상태를 섞지 않는다.

- 종류: `habit`, `recipe`, `app`, `bug`, `security`, `documentation`, `compatibility`
- 해결 수준: `native`, `good-lock`, `third-party`, `adb-automation`, `custom-app`
- 검증: `reproduced`, `implemented`, `device-verified`, `multi-device-verified`, `experimental`
- 환경: `fold-cover`, `fold-main`, `bar-phone`, `one-ui-9`

첫 milestone은 `0.2 multi-device evidence`다. 목표는 새 앱이 아니라 Fold8 외 Galaxy 2개 이상에서
Tap to Top, Holiday Sleep과 AirPods Glance의 성공·실패 행을 받는 것이다.

## 5. APK 배포 gate

소스를 공개했다고 APK를 바로 배포하지 않는다. 다음이 충족될 때 별도 release를 검토한다.

1. 영구 production key의 보관·복구·rotation 정책
2. 설치 파일 hash와 재현 가능한 release build
3. 최소 두 Galaxy 모델의 설치·권한·rollback 검증
4. 접근성 고지와 AirPods 숨은 API의 experimental 표시
5. 업데이트 경로와 이전 서명 호환성
6. Android의 외부 배포·개발자 확인 정책 재검토

## 6. 수요를 읽는 지표

Star 수보다 다음 순서를 우선한다.

1. 다른 기기의 재현 가능한 compatibility report
2. 실제 적용 뒤 rollback까지 성공한 보고
3. 같은 iPhone 습관 issue의 반복
4. 문서로 해결된 비율과 직접 앱이 필요했던 비율
5. 설치·업데이트를 한 번에 해 달라는 반복 요청

마지막 요구가 쌓일 때에만 진단 마법사나 쉬운 배포판을 다음 제품 가설로 올린다.

## 7. 공개 커뮤니케이션

첫 글은 “Galaxy를 iPhone처럼 꾸몄다”가 아니라 “익숙한 행동을 어떤 해결 수준으로 번역했는지”를
보여준다. 성공한 세 앱만 나열하지 않고 반복 플릭, DND fallback, AirPods 이미지 상태 추정이 어떻게
반증됐는지 한 가지씩 보여주면 프로젝트의 차별점인 검증 과정이 드러난다.

기본 문서는 한국어로 시작하되 README 첫 부분에 짧은 English summary를 둔다. 다기기 제보가
생기기 전에는 전체 1만 줄의 설계 문서를 성급히 번역하지 않는다.
