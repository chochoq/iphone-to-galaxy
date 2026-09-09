# 달과 별 앱 아이콘 — 개발용

> 2026-09-08 개발 당시의 기록입니다. 아래 ‘공개본 미반영’ 표시는 당시 상태를 뜻합니다.
> 이번 선별 소스에는 포함되지만 원본 기기 자료는 공개하지 않습니다.

사용자가 수면 연장 앱에 맞는 앱 아이콘을 그려달라고 요청했다. iOS 계열 파란 배경에 흰 초승달과
작은 별을 사용했다. 기존 앱에는 manifest icon 지정이 없어서 Android 기본 아이콘이 표시되었다.

## 제작과 파일

내장 image_gen 도구를 사용했다. imagegen 스킬에 따라 최초 생성 후 가장자리 투명도를 확인하고,
배경만 불투명한 파랑으로 채우는 편집을 한 번 더 했다. CLI나 별도 API 키는 쓰지 않았다.
실제 사용하는 최종 원본은 `../../res/drawable-nodpi/ic_launcher_art.png`다.
이미지를 생성 도구 저장 폴더에만 남기지 않고 정본 리소스로 복사했다.

Android adaptive icon이 최종 모서리 모양을 자른다. 원본에 둥근 사각 테두리를 미리 굽지 않는다.
배경 레이어에 전체 그림을 두며 foreground는 투명하다. 전경·배경이 따로 움직이는 입체 효과는 없다.
Android 13 이상 테마 아이콘에는 별도의 직접 그린 단색 벡터를 제공한다. 테마 사용 시 파랑 대신
사용자 시스템 팔레트로 보이는 것은 정상이다. Apple 이미지·서체·상표는 번들하지 않았다.

## 최초 생성 프롬프트

Use case: logo-brand. Asset type: production Android launcher icon artwork for a Korean holiday sleep-extension app, with the quiet, polished simplicity of a native iOS utility. Generate one square 1024x1024 bitmap, NOT a phone mockup. Full-bleed smooth blue background, subtle vertical gradient from iOS-like bright azure #168BFF near the top to rich system blue #0065DC near the bottom, no purple. A single large luminous white crescent moon, subtly rounded and softly dimensional with restrained pearly shading, and one small simple white four-point star close to its upper opening. The moon and star form a single balanced centered composition fully contained in the central 58 percent of the image, allowing generous safe-area margins for Android adaptive icon masks. Strong crisp silhouette legible at 48px. Calm, elegant and restrained; no glossy glare, no hard shadow, no excessive 3D extrusion. Edge-to-edge blue square background with NO pre-rounded icon corners, NO outer white margin, NO tile border. No text, letters, clock numbers, calendar grids, watermarks, Apple logo, device frame or additional objects. Deliver only the finished icon artwork.

## 최종 배경 수정 프롬프트

Edit target: supplied moon-and-star icon. Keep the white crescent and white four-point star, their positions and subtle shading exactly as they are. Change ONLY the background: replace ALL transparency and ragged blue edges with a perfectly smooth, fully opaque blue gradient covering EVERY pixel to all FOUR corners of the square canvas. The whole image must be an opaque square blue field, not a circle or isolated icon tile. No black corners, no white corners, no alpha holes, no vignette or fuzzy perimeter. Match the existing center blue smoothly to edges, with very subtle darker blue toward bottom. No rounded corners, no border, no new objects, no text. Production Android adaptive launcher artwork; Android will apply its own outer mask.
