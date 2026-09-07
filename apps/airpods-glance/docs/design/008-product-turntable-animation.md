# 008 — 연결 시트 제품 회전 애니메이션

> 2026-09-06 관측 정정: 아래 초안에서는 Apple 촬영의 지속 회전을 확인하지 못했다고 기록했다.
> 같은 AirPods 4 영상을 15.5~19.5초 확대 연속 프레임으로 재검토하자 한 쌍이 함께 회전하는 것이
> 확인됐다. 실제 두 이어버드가 케이스 안에 있어도 화면은 이어버드와 닫힌 케이스를 따로 보여 준다.
> 과거의 미확인을 Apple의 비회전 근거로 쓰지 않는다.
> [실제 비교 결과와 상태 표시 제약](../results/2026-09-06-ios-motion-and-case-state-review.md).

## 1. 발견과 근거의 경계

Apple paired-battery 촬영을 다시 프레임 단위로 확인했을 때 제품 그림의 지속 회전은 명확히 관측되지
않았다. 따라서 “Apple 화면이므로 반드시 돈다”고 쓰지 않는다. 반면 사용자가 앞서 사용한 MaterialPods
APK에는 제품 상태별 MP4가 있고, 보관 APK의 `pro_2_both_alternative_day.mp4`는 550×550, 72fps,
6초 길이의 전면→측면→후면→반대 측면→전면 360도 turntable이었다. 이 자산은 구조와 속도만
관측했으며 우리 APK로 복사하지 않는다.

사용자는 현재 정적 제품이 어색하고 “이미지가 뱅글뱅글 돌아야 한다”고 명시했다. 따라서 회전은 Apple
동작에 대한 추정이 아니라 사용자가 선택한 제품 경험이며, MaterialPods에서 확인한 6초 turntable을
속도 기준으로 삼는다.

## 2. 사용자 시나리오

### S-016 — 지속 카드에서 제품을 입체적으로 확인한다

1. AirPods 연결 뒤 카드가 하단에서 나타난다.
2. 제목과 배터리 값은 고정되고, 이어버드 **한 쌍이 공통 축으로 함께 회전**한다.
   ~~열린 케이스~~는 2026-09-07 실기기 관측으로 폐기하고 **닫힌 케이스**를 표시한다.
   케이스 자체의 지속 회전은 아직 iOS 실측으로 확정한 사실이 아니라 우리 카드의 기존 동작이다.
3. 약 6초에 한 바퀴를 완성하고 카드가 열린 동안 자연스럽게 반복한다.
4. 사용자가 카드 본문이나 X를 누르면 시트와 두 애니메이션이 함께 종료된다.
5. case 값이 unknown이면 case 영상과 decoder 자체를 만들지 않고 이어버드만 중앙에서 돈다.

### S-017 — 시스템 애니메이션을 끈 사용자가 카드를 본다

1. Android 개발자/접근성 설정의 animator duration scale이 0이다.
2. 제품은 첫 정적 프레임으로 보이고 제목·배터리·닫기 동작은 그대로다.
3. 장식 모션을 위해 사용자의 전역 motion 선택을 무시하지 않는다.

## 3. best practice 반복 검증

### BP-A — 평면 PNG의 Z축 회전을 3D 제품 회전이라고 부르지 않는다

- 1차: 단일 PNG의 `rotation`은 제품 전체가 바람개비처럼 돌 뿐 측면과 후면을 만들지 않는다.
- 2차: `rotationY`만 쓰면 90도에서 종잇장처럼 폭이 0이 된다.
- 3차: MaterialPods 실물은 여러 각도가 들어간 6초 제품 영상이다.
- 결론: 독립적으로 만든 16개 핵심 yaw 시점을 사용하고, 각 시점 사이 세 프레임을 보간해 65프레임
  animated WebP로 만든다.

### BP-B — 움직이는 것과 읽는 것을 분리한다

- 제품만 회전하며 기기명, 배터리 glyph, percent, part label은 움직이지 않는다.
- 이어버드와 case는 같은 약 5.98초 loop를 사용해 서로 다른 속도로 시선을 흔들지 않는다.
- 카드와 자식 art의 click listener를 나누지 않아 어디를 눌러도 기존 dismiss가 동작한다.

### BP-C — 지속 카드가 숨은 decoder를 남기지 않게 한다

- animated drawable은 view가 window에 attach될 때 시작한다.
- card tap, X, 연결 해제, service destroy로 view가 detach되면 `Animatable.stop()`을 호출한다.
- case unknown이면 view를 생성하지 않으므로 보이지 않는 case animation도 실행되지 않는다.

## 4. 자산과 구현 계약

- 원본 생성 시트: `design-assets/generated-turntables/`
- 앱 자산: `res/drawable-nodpi/airpods_{pair,case}_turntable_{light,dark}.webp`
- 각 앱 자산: 320×320, 65 frames, frame delay 92ms, 무한 loop, 약 5.98초/회전
- 밝은 배경은 `#F2F2F7`, 어두운 배경은 `#2C2C2E`를 기준으로 별도 생성해 흰 사각형을 피한다.
- 첫 16방향 생성에서는 투명 배경 대신 checkerboard가 실제 픽셀로 들어갔다. 이를 통과시키지 않고
  테마별 단색 원본을 다시 만들었다.
- 기존 투명 정적 PNG는 전역 animation scale=0일 때의 fallback과 생성 기준으로 보존한다.

## 5. 의존성 우선 구현과 예측

1. 먼저 16시점의 제품 정합성과 light/dark 자산을 확인한다.
2. animated WebP의 frame/loop/duration을 파일 수준에서 검증한다.
3. attach/detach를 소유하는 `ProductTurntableView`를 만든다.
4. 마지막에 기존 renderer의 두 `ImageView`만 교체해 배터리·수명·권한 계약을 건드리지 않는다.

| 예측 | 반증 조건 | 피드백 |
|---|---|---|
| 두 제품이 약 6초에 부드럽게 360도 회전한다. | 각도가 튀거나 역방향으로 되감긴다. | 핵심 프레임 순서와 loop 경계 재생성 |
| 카드 배경과 영상 경계가 보이지 않는다. | 밝거나 어두운 정사각형이 제품 뒤에 보인다. | 배경 matte/edge feather를 다시 생성 |
| 본문/X 직후 motion이 사라진다. | 창 제거 뒤 decoder 또는 drawable callback이 남는다. | detach stop과 owner cleanup 추적 |
| 글자 읽기는 안정적이다. | 배터리나 제목까지 흔들린다. | art container 외 animator 여부 점검 |

이 설계에서 중요한 느낌은 “움직임을 추가했다”보다 정적 제품이 연결 순간의 생기를 잃고 있었다는
사용자의 지적이다. 다만 움직인다는 이유만으로 Apple과 같다고 포장하지 않고, 실제 확인된 MaterialPods의
turntable 문법과 사용자가 원하는 감각을 우리 자산·수명주기에 맞게 다시 만든다.

## 6. 실기기 반증 — 생성 프레임 보간안을 폐기한다

0초·1.5초·3초·5.7초 정지 캡처의 changed-pixel 검사는 animation이 재생된다는 사실만 보였다.
사용자가 웃으며 직접 확인해 보라고 한 뒤 8.15초 실기기 영상을 0.5초 간격으로 펼쳐 보니, 이어버드가
각도 사이에서 늘어나고 복제되며 case 뚜껑과 내부도 다른 물체처럼 변했다. 이는 자연스러운 turntable이
아니다. 내가 수치 검사를 시각 품질의 대리값으로 사용한 판단이 잘못이었다.

**설계 정정:** 16시점 이미지 생성 + morph 방식은 폐기한다. 실패한 생성 시트와 WebP는 회귀 방지용
rejected 자료로 보존하되 APK에서는 제거한다. 다음 구현은 하나의 3D mesh만 회전시켜 모든 프레임의
topology와 부품 수를 고정한다. 실기기 screenrecord를 한 회전 전체로 직접 펼쳐 보고, 어떤 두 프레임
사이에서도 새 부품·형태 변화·lid angle 변화가 없는지를 통과 조건으로 삼는다.

## 7. 단일 3D 원본의 선택과 네 각도 게이트

### 7.1 출처와 사용 경계

Sketchfab의 `Airpods Pro With Magsafe Charging Case Ios15` 모델을 사용한다. 저자는 polyman이고
라이선스는 CC BY 4.0이다. 공개 GitHub 저장소에 원본 glTF·texture·정확한 저작자 표시가 함께 보존돼
있어 로그인 우회 없이 내려받았다. 원본과 `LICENSE.txt`는
`design-assets/3d/external/polyman-airpods-pro/`에 함께 둔다. 앱의 third-party notice에도 모델명,
저자, 원문 URL, 라이선스 URL을 기록한다.

### 7.2 네 각도에서 먼저 반증한다

전체 애니메이션을 만들기 전에 이어버드 쌍과 열린 case를 0°·90°·180°·270°로 렌더했다. 이 단계에서
다음 두 구현 오류를 발견했다.

1. glTF 상위 노드의 0.01 scale을 잃고 제품 root를 빈 pivot에 다시 parent하면 크기가 100배가 되고
   중심도 두 번 이동했다. root hierarchy를 바꾸는 방식을 철회하고, 원래 world matrix를 중심점 전후의
   yaw matrix로 감싸는 방식으로 바꿨다.
2. case 원본에는 저해상도 이어버드 stand-in mesh가 포함돼 있었다. 투명 배경에서 이를 단독 렌더하면
   검은 이어버드가 나타났다. 이 mesh를 숨기고, 같은 라이선스 원본의 상세 이어버드 hierarchy를 복제해
   case well 안으로 0.028m 내린 뒤 case와 하나의 product transform으로 회전시킨다.

수정한 네 각도 접촉 시트에서는 이어버드 개수, case body, 열린 lid 각도와 상세 mesh가 각도 사이에서
고정된다. 이는 아직 실기기에서 모션이 자연스럽다는 최종 판정은 아니다. 30fps·약 6초 한 바퀴를 만든
뒤 APK에 넣기 전/후로 전체 loop를 영상과 contact sheet로 다시 직접 본다.

### 7.3 최종 렌더 계약 정정

- ~~16 key views + morph, 65 frames, 92ms~~ — 폐기
- 한 glTF mesh hierarchy, 320×320 RGBA, 180 frames, frame당 33ms, 약 5.94초
- pair와 case는 같은 각도·같은 frame index를 사용한다.
- frame 179 다음에는 frame 0이 오며 마지막 프레임을 중복하지 않는다.
- 시스템 animator scale이 0이면 기존 정적 PNG를 사용한다.

## 8. 실기기 최종 판정

두 WebP는 각각 320×320, alpha, 180 frames, 33ms, infinite loop이며 합계 약 1.6MiB다. SM-F971N에서
카드를 먼저 띄운 뒤 기록한 8.006초 screenrecord는 242 video frames로 약 30fps였다. 16개 시점으로
펼친 화면에서 pair와 case만 회전하고 제목과 배터리는 고정됐으며, 이전 방식의 morph·이어버드 복제·lid
형상 변화는 없었다. frame 179→0 변화량도 평상시 0→1과 비슷해 loop 경계가 별도로 튀지 않았다.

본문 tap과 X 각각 뒤 type 2038 window가 1→0이 됐고 재개방 뒤 1을 확인했다. 최종 설치 APK에는
CC BY 4.0 attribution을 `res/raw/third_party_notices.txt`로 함께 넣는다. 남은 제품 수준 검증은 저장
snapshot preview가 아니라 AirPods의 다음 실제 새 연결에서 자동 카드가 같은 renderer로 한 번 뜨는지다.

## 9. 사용자 시각 판정 뒤 조명 설계 정정

단일 3D mesh라서 형상은 안정됐지만, 그것만으로 제품 이미지가 자연스러운 것은 아니었다. 최초 실기기
렌더는 흰 플라스틱 아랫면과 case 가장자리가 거의 검정으로 눌렸다. source material의 약한 emission,
`AgX - Medium High Contrast`, -0.35 exposure와 위쪽 세 방향에만 둔 area light가 겹친 결과다. 사용자가
색과 그림자가 이상하다고 웃은 시점에서 앞선 “3D turntable 통과” 판정은 **형상·모션에만 유효하고
조명 품질에는 유효하지 않았음**을 명시한다.

조명 수정은 다음 세 번의 preview gate를 거쳤다.

1. emission·ambient·exposure를 함께 올리고 아래 보조광을 16으로 둔 첫 안은 검정을 없앴지만 제품의
   굴곡과 경계까지 흰색으로 날렸다. 그림자 제거를 전체 노출 증가로 해결하는 안을 폐기했다.
2. 대비와 노출을 일부 되돌리고 아래 보조광을 10으로 낮춘 두 번째 안도 작은 카드 크기에서는 여전히
   평평하고 과노출이었다. 이 scene scale에서는 10도 주광에 가까운 세기임을 확인했다.
3. 기존 emission 0.28, high-contrast look, -0.35 exposure, ambient 0.28과 세 주광 18을 복원하고
   아래쪽 넓은 area light만 2로 추가했다. 0°·90°·180°·270° 뒤 8개 중간 각도를 다시 펼쳐 보니 외곽의
   검은 띠는 연회색 곡면 음영으로 바뀌고 이어팁 안쪽·센서·case 내부 틈만 어둡게 남았다.

**수정된 시각 계약:** 흰 플라스틱을 새하얀 silhouette로 만들지 않으며, 외부 곡면에는 검정으로 뭉친
띠를 허용하지 않는다. 제품 고유의 검은 vent와 실제 가려진 case 내부는 보존한다. 수치 histogram은
이 계약의 대리값으로 사용하지 않고, 투명 PNG의 카드 배경 합성본과 실기기 한 회전 영상을 직접 본다.

## 10. Apple 공식 연결 카드 대조 뒤 하이라이트 설계 정정

9장의 lower fill 수정은 검은 외곽을 해결했지만, 사용자의 다음 실기기 판정에서 반대쪽 문제가 드러났다.
이어버드 머리와 case 뚜껑의 넓은 면이 250~255 RGB의 순백으로 붙어 형상 경계가 사라졌다. 따라서
9장의 v3를 최종 재질로 유지하지 않고 **그림자 통과·하이라이트 실패인 중간 결과**로 보존한다.

추론 대신 Apple Support의 현재 문서 `Connect your AirPods and AirPods Pro to your iPhone`
(`https://support.apple.com/en-us/104989`)와 그 문서가 직접 제공하는 iOS 26 연결 카드 이미지를 기준으로
삼았다. reference는 `design-assets/reference/apple-official/`에만 두며 APK에는 포함하지 않는다. 공식
이미지에서 확인한 문법은 다음과 같다.

- 카드의 제품은 순백 silhouette가 아니라 220대~낮은 230대의 넓은 중간 회색으로 읽힌다.
- 밝은 부분은 전체 면을 덮지 않고 모서리와 작은 곡면에 좁고 부드럽게 남는다.
- case 내부도 검정 덩어리가 아니라 회색 gradient이며, 실제 검정은 earbud vent처럼 제품 부품인 곳에
  집중된다.
- 위 값은 서로 다른 이미지/색공간의 절대 명세가 아니라 현재 렌더의 250~255 clipping을 반증하고
  시각 비교를 반복하기 위한 표본이다.

후보 A~C는 tone mapping과 exposure만 바꿨으나 흰 면이 여전히 배경과 붙었다. D~F는 emission과 주광을
낮췄지만 case 앞면에 긴 순백 반사 띠가 남았다. G~O에서 공유 white-plastic material의 base tone과
roughness를 분리했으며, P~R에서 `Specular IOR Level`까지 낮춰 직접 반사광을 억제했다. 최종 Q는 다음
값이다.

- AgX Medium Low Contrast, exposure -0.10
- world 0.45, 세 main area light 5, lower fill 5
- white-plastic base scale 0.30, roughness 0.75, specular IOR level 0.05
- baked emission 0.08

렌더러는 이 변수를 CLI로 노출하고 Q를 기본값으로 고정한다. 옵션을 생략한 4방향 재렌더의 RGBA 픽셀이
선택 Q와 모두 같음을 확인해, 다음 작업자가 명령 옵션을 잊어 v3 조명으로 회귀하지 않게 한다. 최종 판정은
180-frame 8방향 sheet와 SM-F971N의 7.986초 실기기 녹화에서 넓은 white clipping·가로 반사 띠·각도별
점광 flash가 없는지 직접 보는 방식이다.
