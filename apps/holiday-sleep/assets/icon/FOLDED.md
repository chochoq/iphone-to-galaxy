# 접힌 곡선 아이콘 — 2026-09-14

달력 한 장이 포근한 이불로 접히는 모습으로 새로 그렸습니다.
다른 두 앱과 아이보리색 면, 무광 종이·도자기 질감, 부드러운 빛을 맞췄습니다.

## 파일과 출처

[그림 원본](../../res/drawable-nodpi/ic_launcher_folded_v3.png)은 1254×1254 불투명 PNG입니다.
내장 image_gen으로 생성했으며 Apple의 공식 이미지나 제3자 모델 렌더가 아닙니다.
배경·여백은 Android XML에서 배치하고, 테마용 단색 버전은 직접 작성한 벡터입니다.

## 1.1.1 시안에서 런처 리소스로

시안 전체를 기존 adaptive 배경에 바로 넣으면 108:72 확대 때문에 잘립니다.
배경 drawable의 네 변에 1/6 inset을 주어 보통의 72dp 표시 영역에 원본 전체를 맞췄습니다.
바깥은 그림과 유사한 배경색으로 채웁니다. foreground는 투명하며 레이어별 독립 입체 효과는 없습니다.

## 1.1.2 이전 파일과의 관계

기존 ic_launcher_art 계열과 이전 단색 벡터는 기록으로 보존합니다.
현재 런처는 ic_launcher_folded_background 및 ic_launcher_folded_mono를 사용합니다.
기능·권한·사용자 설정은 이 아이콘 작업에서 바꾸지 않습니다.

## 실제 생성 프롬프트

```text
Create ONE original app icon artwork, square, full-bleed opaque bitmap, no text or presentation board.
App: a holiday-aware sleep extension utility. Emotional idea: "the day can wait." A genuinely designed visual metaphor, NOT a stock moon/star icon and NOT a stock calendar icon on a colored gradient.

Build a single, beautifully composed impossible object: a broad ivory calendar leaf becomes a softly folded duvet, as if the day itself is tucked into bed. The upper edge has just two subtly embedded calendar binding slots. The surface flows down into a generous folded-over quilt lip, with a very small blush-coral reverse of the sheet revealed along the curl. A tiny pale pillow is nestled behind the upper fold, part of the same silhouette. No numbered calendar grid. No numerals, letters, logos, moons, stars, clocks, alarm bells, ZZZ, badges.

Art direction: sophisticated tactile editorial design, smooth continuous paper-to-fabric form, not plastic emoji. A near-frontal slightly elevated view, quiet asymmetric composition, large broad ivory curved planes and one rich dusky plum fold/shadow. Precise restrained lighting, matte porcelain-paper surface, no woven texture or seams. No glossy reflections, metallic bevels, inflated marshmallow edges or toy render aesthetic. Limit detail so this is beautiful and recognizable at 48 pixels.
Warm very pale lavender background extending completely to all four edges; no pre-rounded tile, no mockup, no border, no frame. The main object fills about 68% of the square width and height, entirely within the middle 74%, with ample uncluttered margins for rounded and circular launcher masks. The folded ivory surface and plum inside fold, rather than an isolated generic symbol, are the actual composition. Subtle soft contact shadow only. Premium mobile icon craft, calm, memorable, distinctive silhouette.
```
