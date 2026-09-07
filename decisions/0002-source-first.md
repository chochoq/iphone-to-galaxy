# ADR-0002 — 첫 공개는 source-first로 한다

- 상태: 채택
- 날짜: 2026-09-07

## 맥락

Tap to Top은 접근성, Holiday Sleep은 캘린더·DND, AirPods Glance는 overlay와 숨은 Bluetooth API를
쓴다. 설치는 편하지만 단일 Galaxy에서만 검증한 APK를 먼저 배포하면 권한·서명·업데이트 책임이
문제 설명보다 앞선다.

## 결정

0.1에는 buildable source와 검증 history를 공개하되 GitHub Release APK는 첨부하지 않는다. 각
개발자가 생성하는 debug key는 `.local/`에만 있고 Git에서 제외한다.

## 결과

비개발자의 즉시 설치는 어렵다. 대신 source audit, 다기기 결과와 배포 요구를 먼저 모을 수 있으며
production key와 업데이트 정책을 성급히 고정하지 않는다.
