# 상태별 제품 motion 재설계 결과 — 2026-09-07

> **같은 날 후속 정정:** 아래 v2의 `8방향 통과`는 정규화한 외부 방향별 비교에서 반증됐다.
> 90도 측면 seam의 lid/body 중심이 4px 어긋나 있었다. 운영 자산은 -1mm seam 보정을 적용한
> v3로 교체했으며, 최신 판정은
> [iOS/현재 이미지 재비교 결과](2026-09-07-ios-current-image-comparison.md)를 따른다.

## 설계 → 태스크 → 구현

- 설계 012에 표시 battery 상태와 물리 placement를 분리했다.
- S-018~S-022, BP-018~BP-021, 64조합 불변 조건, 의존성 우선 구현 순서와 실패 환류 지점을 추가했다.
- 태스크 18은 pure composition policy → closed geometry gate → asset encode → overlay → Galaxy 순서로 수행했다.
- single-bud iOS 외관은 미확정이므로 candidate로 보존하고 운영 artwork 분기에 연결하지 않았다.

## 구현 중 발견과 설계 수정

1. 기존 110도 lid 근사는 정면만 정상이고 회전 중 y 중심 4.7mm 어긋남과 z 관통이 있었다.
2. lid/body 전체 bounds의 z 접촉을 강제하자 내부 mesh 최저점 때문에 큰 공백이 생겼다.
3. 정면의 검증된 높이는 유지하고 y depth 중심만 맞췄다.
4. 새 후보를 0/45/90/135/180/225/270/315도에서 직접 검사해 형상 gate를 통과시켰다.
5. 이 발견을 설계 012.14와 태스크 18.2.1에 먼저 반영한 뒤 full render를 수행했다.

## 결과 예측 대 실제 테스트

| 예측 | 테스트 | 결과와 해석 |
|---|---|---|
| unknown이 제품을 만들지 않는다 | L/R/C·motion·두 asset gate 64조합 | 통과; placement는 입력에 없음 |
| 실패 asset은 정적으로 낮아진다 | asset verified false 조합 | 통과; motion=false도 우선 |
| closed case가 전 방향에서 유지된다 | 8방향 320px preview | 통과; 앞선 두 실패 후보는 거부 기록 |
| WebP가 6초에 끊김 없이 돈다 | 180 unique RGBA, duration/alpha/clipping/seam | 통과; seam 4.652, 내부 최대 19.147 |
| static fallback이 첫 frame과 같다 | decoded pixel 비교 | 통과 |
| 실제 Android 카드에서 pair/case가 함께 유지된다 | SM-F971N 8.013초, 248-frame 녹화와 16표본 | 통과; lid 부유 미관측 |
| single bud가 iOS와 같다 | 실기기 iOS 한쪽 시나리오 | 미실시; pair fallback 유지 |
| animation scale 0이 정적이다 | pure policy는 통과, Galaxy 전역 설정 변경 | 전역값 1.0만 확인; 실기기 0 미실시 |

## 패키지와 안전성

- core tests: 262649 assertions.
- APK 서명/권한 정책 검사 통과. 인터넷·위치·마이크·접근성 권한 없음.
- 실패한 independent/open/bad-closed runtime 자산 5개는 앱 `res`에서 설계 보관 폴더로 이동했다.
- 최종 APK에는 joint pair animation/static과 closed case v2 animation/static만 포함된다.
- APK 크기 4,392,119 bytes; SHA-256
  `45f6df3eb5748fc44df7f98ed71beb0c9737d7ce6f15b94eafe6501422c7dd39`.
- Galaxy 덮어 설치 성공; lastUpdateTime 2026-09-07 10:59:09.
- Bluetooth 요청, AAP 연결 횟수, 30초 창, 오디오 정책은 변경하지 않았다.

## 남은 검증

사용자와 함께 iOS에서 한쪽만 꺼낸 경우와 양쪽을 모두 꺼낸 경우의 artwork 구성을 확인해야 한다.
그 결과가 들어오면 설계 012.3/012.10을 먼저 정정하고 single candidate를 확정 또는 폐기한다.
Galaxy의 animator scale 0 실측은 전역 사용자 설정을 바꾸므로 자동으로 수행하지 않았다.
