# iOS·공식 이미지·현재 카드 재비교 결과 — 2026-09-07

## 결론

사용자가 느낀 어색함은 단순 취향이 아니었다. v2 closed case는 정면에서는 닫혀 보였지만 45도와
90도에서 뚜껑과 몸체의 깊이 중심이 달라 외곽이 계단처럼 꺾였다. 조명을 바꾸지 않고 seam 중심을
다시 맞춘 v3로 운영 자산을 교체했다. 이어버드 공동 회전과 기존 Q 조명은 관측 및 이전 사용자 판정과
충돌하지 않아 유지했다.

## 비교 방법과 잘못된 첫 비교

첫 판은 MaterialPods frame을 과도하게 확대하고 투명한 우리 PNG를 검정 배경에 표시했다. 크기와
배경이 달라 case 두께와 pair 간격을 판단할 수 없으므로 폐기했다. 다시 만든 판은 각 frame의 실제
물체 경계를 잘라 같은 높이·흰 배경·같은 yaw에 놓았다.

근거는 다음 순서로 사용했다.

1. 사용자의 iOS 직접 재현과 실제 iPhone 촬영 두 편: 상태 구성과 공동 회전 확인.
2. Apple Support 104989 공식 연결 카드: 흰 플라스틱의 highlight 범위 확인.
3. CC BY 4.0 polyman model: 우리 topology의 정본.
4. MaterialPods 및 공개 Android 구현: 방향별 silhouette 반례만 확인. 라이선스가 불명확해 복사하지 않음.

실제 iPhone 촬영은 카드 제품이 작고 노출·압축되어 있으므로 RGB 절대값과 정확한 easing을 확정하는
자료로 쓰지 않았다. Apple 연결 카드와 battery popup도 서로 다른 표면이므로 상태 규칙을 섞지 않았다.

## 발견 → 설계 수정 → 구현

- renderer 기본 닫힘 각도 40도는 정면 착시만 만들고 측면에서 lid가 떠 있었다.
- 100/105/110/115/120도 sweep에서 110도가 닫힌 seam 높이와 수평을 만들었다.
- v2 90도 alpha mask에서 lid/body mating row는 모두 92px였지만 중심이 4px 달랐다.
- world Y -1mm 추가 보정 뒤 두 row가 같은 92px 구간에 겹쳤고, 8방향 외곽이 연속됐다.
- renderer 기본값을 110도/-1mm로 바꿔 다음 실행이 40도로 회귀하지 않게 했다.
- case runtime resource를 v3로 올리고 static fallback을 v3 frame 0으로 교체했다.
- legacy pair static의 작은 alpha island를 제거하기 위해 운영 WebP frame 0을 fallback 정본으로 삼았다.

## 테스트 피드백

| gate | 결과 | 해석 |
|---|---:|---|
| 기본 실행 vs 명시적 v3 후보, 8방향 RGBA | RMSE 0, 8/8 | 기본값 회귀 없음 |
| case frame | 320×320, 180 unique | 같은 topology의 한 회전 |
| duration/alpha/clipping | 6000ms / RGBA / 0 | 파일 계약 통과 |
| static vs animation frame 0 | RMSE 0 | reduced motion에서 그림이 바뀌지 않음 |
| loop 179→0 vs 보통 0→1 | 0.00745 vs 0.00649 | 같은 2도 이동 규모, 큰 되감김 없음 |
| core | 262,649 assertions | 상태·수명 회귀 없음 |
| APK | v3 signature, policy 통과 | 인터넷·위치·마이크·접근성 권한 추가 없음 |
| SM-F971N | 8.018초, 245 frames | 16표본에서 v2 seam step 미재현 |

설치 APK SHA-256은
`70e79cae73252f837ebe357b33f71aad1aeb7535e6789776b1c3162cfbb38d3b`이고,
설치 시각은 2026-09-07 11:55:24다. 카드 preview는 사용자가 직접 볼 수 있도록 열린 상태로 두었다.

## 아직 모르는 것

- iOS와 완전히 같은 회전 easing 및 자동 반복 횟수
- 좌우 battery가 같을 때 iOS가 합치는 정확한 조건
- 한쪽만 유효할 때 사용하는 artwork
- 촬영 노출을 제거한 iOS battery card의 절대 material tone

이 네 항목은 확인 자료 없이 현재 구현을 바꾸지 않는다.

## 사용자 최종 시각 판정

2026-09-07 사용자가 설치된 Galaxy의 v3 카드를 직접 확인한 뒤 “이제 잘 나온다”고 판정했다.
따라서 110도/-1mm closed-case 형상과 현재 Q 조명의 조합은 운영 시각 기준을 통과한 것으로 확정한다.
이 판정은 아직 실측하지 않은 easing, battery 합침 조건, single-bud 상태까지 확정한다는 뜻은 아니다.
