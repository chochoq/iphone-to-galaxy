# 라이선스 범위

Copyright © 2026 chochoq

이 저장소에는 우리가 만든 코드와 문서뿐 아니라, 원래 라이선스를 그대로 따라야 하는 외부
저작물도 들어 있습니다. 파일 종류에 따라 다음 조건을 적용합니다.

## 우리가 만든 코드

앱 소스, 테스트, 빌드 도구, 자동 검사, Android XML 리소스와 GitHub 설정처럼 실행이나 빌드에
쓰이는 파일은 [GNU General Public License v3.0 or later](../LICENSE)를 따릅니다.
SPDX 식별자는 `GPL-3.0-or-later`입니다.

이 라이선스는 상업적 이용과 판매를 금지하지 않습니다. 다만 수정한 프로그램을 다른 사람에게
배포하면, 그 사람에게도 대응하는 소스와 같은 GPL 권리를 제공해야 합니다.

## 우리가 쓴 문서

README, 문제 해결 기록, 설정 방법, 설계·작업·시험 기록과 직접 만든 도표·문서용 화면 녹화는
[Creative Commons Attribution-ShareAlike 4.0 International](CC-BY-SA-4.0.txt)을 따릅니다.
SPDX 식별자는 `CC-BY-SA-4.0`입니다.

문서를 복사하거나 고쳐서 공유할 때는 저작자를 표시하고, 변경 여부를 밝히며, 같은 라이선스로
공유해야 합니다.

## 생성형 AI로 만든 앱 아이콘

세 앱의 `res/drawable-nodpi/ic_launcher_art*.png`는 이 프로젝트에서 이미지 생성 도구로
제작한 아이콘입니다. Apple이나 Samsung의 공식 아이콘을 내려받은 파일이 아닙니다.
제공할 수 있는 권리 범위에서 `CC-BY-SA-4.0`으로 공유하며, AI 생성물의 저작권 인정 여부는
지역에 따라 다를 수 있습니다. 아이콘을 연결하는 Android XML은 위 코드 라이선스를 따릅니다.

제작 과정과 사용한 프롬프트는 각 앱의 `assets/icon/README.md`에 있습니다.
에어팟 앱의 런처 아이콘과 아래의 제3자 3D 제품 렌더는 출처가 서로 다릅니다.

## 외부 저작물과 예외

다음 네 파일은 polyman의 3D 모델을 바탕으로 만든 렌더이며
[`CC BY 4.0`](CC-BY-4.0.txt)을 그대로 따릅니다.

- `apps/airpods-glance/res/drawable-nodpi/airpods_case_closed_render_v3.png`
- `apps/airpods-glance/res/drawable-nodpi/airpods_case_closed_turntable_3d_v3.webp`
- `apps/airpods-glance/res/drawable-nodpi/airpods_pair_render.png`
- `apps/airpods-glance/res/drawable-nodpi/airpods_pair_turntable_3d.webp`

정확한 저작자, 원본 주소와 변경 내용은
[`THIRD_PARTY_NOTICES.md`](../apps/airpods-glance/THIRD_PARTY_NOTICES.md)에 있습니다.

[`연결 카드 데모`](../docs/images/airpods-card-demo.gif)에 보이는 제품 렌더에도 같은 예외가
적용됩니다. 화면 녹화·편집과 포함된 제품 렌더의 출처는 [영상 안내](../docs/images/)에 구분했습니다.

AirPods Glance가 빌드할 때 내려받는 HiddenApiBypass 6.1은
[`Apache-2.0`](Apache-2.0.txt)을 따릅니다. 해당 바이너리는 이 저장소에 넣지 않았습니다.
라이선스와 고지는 만들어지는 APK에도 포함됩니다.

`LICENSE`, `LICENSES/*.txt`와 제3자 고지는 각각 적혀 있는 원래 조건을 따릅니다. Apple과
Samsung의 상표는 각 소유자에게 있으며, 프로젝트 라이선스가 상표 사용권을 주지는 않습니다.

파일의 출처나 적용 조건이 분명하지 않다면 재사용하기 전에 Issue로 확인해 주세요.
