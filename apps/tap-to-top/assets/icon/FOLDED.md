# 접힌 곡선 아이콘 — 2026-09-14

긴 피드가 위로 휘어 올라가는 모습으로 새로 그렸습니다.
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
Create ONE original mobile app icon artwork. Square full-bleed opaque bitmap, no captions, no letters or numbers, no presentation board.
App: "Tap to Top", one tap smoothly scrolls a long feed back toward its beginning. Create an original visual identity from the actual movement of a scrolling feed. NOT an arrow pointing up, NOT a chevron, NOT a glyph plus a gradient tile.

One sculptural continuous feed ribbon, sweeping vertically UPWARDS in a tall open reverse-S curve, as a long ivory page is gently scooped upward and folds back over the top. The broad face is warm white paper; the inside/reverse is a vivid deep ultramarine blue. A small number of broad recessed pale-blue horizontal content bands are visible ONLY on the lower front face, suggesting a feed without legible text. The upper portion is broad, clean, rounded and curls BACK gracefully, open not wound into a tight roll; the dark blue inner curve reveals the motion. A slender coral sliver along the top leading edge is the end-of-feed marker, integrated into the sheet, not a separate symbol. One coherent object, not a stack of cards. Big purposeful asymmetric planes and a recognizable S-shaped silhouette, rising from bottom-left to top-right, but fundamentally vertical. Lower end is rounded and free, no plinth or platform.

Art direction: disciplined premium editorial sculpture, crisp paper surfaces with subtle satin shading, not clay/toy emoji, not a toilet paper roll, not a book, not a scroll with wooden handles. No circles, sparkles, cursor, hand, arrows, top bars, upload imagery, cloud imagery, metallic bevels, glow or inflated marshmallow forms. The curved sheet is the whole visual design, no ornamental badge.

Compose as a clean near-front three-quarter view. Main object fits within the central 70% of the canvas width and height so rounded-square and circular mobile launcher masks preserve the whole silhouette. Background very pale cool blue, uniform edge to edge, with only a delicate contact shadow. No pre-rounded square or frame. Restrained soft directional lighting. Strong blue-white contrast and broad surface areas that remain distinctive at 48px. Original, elegant and memorable app icon craft.
```
