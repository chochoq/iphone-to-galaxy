# 접힌 곡선 아이콘 — 2026-09-14

접힌 아이보리 케이스가 두 이어버드를 감싸는 모습으로 새로 그렸습니다.
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
Use case: stylized-concept.
Asset type: ONE new square full-bleed opaque mobile launcher icon artwork, no presentation board.

Primary request: an AirPods companion app icon, third member of a tactile sculptural icon family. The other two icons depict an ivory calendar page folding into a duvet on pale lavender, and an ivory feed ribbon curling upward with a blue reverse on pale blue. Match their quiet, beautifully folded paper / matte porcelain visual language. This icon should be an original folded-paper reinterpretation of a wireless earbud case, not a product photograph or a generic earbuds glyph.

Subject: exactly TWO clearly recognizable short-stem, silicone-tip ivory wireless earbuds nestled side by side in an open, low and wide charging-case-shaped cradle. The case itself is made of one broad thick ivory sheet gently folded into a curved enclosure: a smooth rounded front lip curls over subtly at one corner, revealing a muted sage-green reverse. A broad curved open lid behind the two earbuds is part of the same sculptural object, with a softly shaded sage inner face. Keep the familiar low oval rounded-rectangle proportions of an earbud charging case, not a shopping bag, bed, box of tissues or toothbrush cup. Two earbuds must remain distinct, with small dark speaker vents and short stems disappearing into the case. No extra floating earbuds. The continuous folded case is the dominant composition, and the earbud pair is clearly visible above its front lip. Gentle asymmetry in the folded lip only; balanced earbud spacing.

Style and materials: precise, refined editorial miniature sculpture, warm ivory uncoated paper with a very fine matte porcelain finish, broad continuous rounded planes and restrained depth, no busy texture. Stylized but immediately recognizable earbuds, NOT glossy photoreal hardware. Small details are reduced for legibility at 48px. Broad soft upper-left lighting, delicate contact shadow underneath, calm inviting mood. Ivory exterior, muted deep sage-green inner curves, very pale mint backdrop. Background fills the whole square to every edge, opaque and uncluttered.

Composition: near-frontal slightly elevated three-quarter view, a single integrated object centered, filling around 68% of canvas width and height with balanced empty margins on all sides. Ensure the ENTIRE earbud-and-case silhouette fits comfortably inside the central circular area. Same scale and visual weight as a calendar-duvet and a curled feed icon. No pre-rounded tile, no framing, no border, no platform.
Avoid: letters, numbers, brand logos, Apple symbol, text labels, battery badge, battery percentage, battery bars, lightning bolts, signal waves, music notes, sparkles, glow, metallic trim, glass, glaring highlights, inflated toy/plastic emoji look. No separate decorative symbol behind the case.
Output only the finished single square icon artwork.
```
