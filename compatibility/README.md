# 호환성

## 개인 기준 환경

| 역할 | 기기 | OS | 의미 |
|---|---|---|---|
| 사용성 기준 | iPhone 14 Pro | iOS 26.6.1 | 익숙했던 iPhone 경험의 출처 |
| 구현 검증 | Galaxy Z Fold8 (`SM-F971N`) | Android 17 / One UI 9.0 | 현재 직접 시험한 범위 |

## 기록 규칙

- `지원` 대신 `직접 검증`이라고 씁니다.
- foldable은 커버 화면과 펼친 화면을 별도 항목으로 기록합니다.
- 화면 확대, 글자 크기, 내비게이션 방식과 밝은·어두운 테마가 UI에 영향을 주면 함께 적습니다.
- OS 업데이트 결과는 이전 행을 덮어쓰지 않고 날짜별 이력으로 추가합니다.

## 2026-09-07 직접 검증 행렬

| 해결책 | 기기·OS | 확인한 것 | 화면 상태 | 판정 |
|---|---|---|---|---|
| Tap to Top 1.4.1 | Z Fold8 / Android 17 / One UI 9.0 | X·YouTube·Chrome·Samsung 설정의 단일 위쪽 동작과 중간 가로채기 | 당시 활성 화면; cover/main 구분 기록 없음 | 단일 기기 검증, 화면별 재시험 필요 |
| Holiday Sleep | 동일 | 한국 공휴일 캘린더 인식, 전용 DND 규칙, 일정 재예약 | 화면 비의존 background 동작 | 단일 기기 검증 |
| AirPods Glance | 동일 | 페어링·실연결, 음악 유지, 좌·우·case 배터리, 4×1 위젯, 지속 overlay, turntable | 당시 활성 화면; fold 상태 전체 행렬 없음 | 실험적·단일 기기 검증 |
| 회의·수면·생활 루틴 | 동일 | 실제 사용 구성 | 메뉴 export 미포함 | 단일 기기 구성, 공개 재현 단계 보강 필요 |
| One Hand Operation+ | 동일 | 양쪽 handle 사용 구성 | 오른손 중심 사용 | 방향별 mapping 재확인 필요 |

빈 화면 상태는 실패가 아니라 `미기록/미검증`입니다. 이후 커버와 메인 화면을 시험하면 같은 행을
덮어쓰지 않고 날짜와 display state가 있는 새 행을 추가합니다.

## 호환성 제보 최소 항목

```text
Galaxy 모델 / 지역:
Android / One UI:
앱 또는 레시피와 version:
커버·메인 화면 / 접힘 상태:
gesture navigation 또는 버튼:
화면 확대 / 글자 크기 / theme:
예상 결과:
실제 결과:
원상복구 가능 여부:
```

serial, Bluetooth 주소, 전화번호, 위치, SSID와 account name은 적지 않습니다.
