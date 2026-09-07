# 005 — 예측·검증·피드백 루프

## 1. 구현 전 예측

| ID | 예측 | 반증 조건 | 반영 위치 |
|---|---|---|---|
| P-001 | Android가 bonded AirPods의 identity address로 BLE 결과를 resolve한다. | 연결 중 유효 Apple frame이 있지만 주소 일치가 한 건도 없음 | ADR-007/008과 identity policy 재설계 |
| P-002 | public BLE frame으로 좌·우·케이스가 10% 단위로 나온다. | 연결 중 type 07/19/prefix 01 frame이 없거나 Pro 3에서 필드 위치가 다름 | protocol decoder와 범위 재설계 |
| P-003 | CAPod·Podsify의 좌우 가설이 실제 한쪽 충전 상태와 맞는다. | 오른쪽만 케이스에 넣었는데 왼쪽 charging으로 표시 | 좌우 flip 규칙 정정 및 이력 보존 |
| P-004 | 연결 broadcast에서 connected-device FGS를 시작할 수 있다. | Android 17에서 background start exception으로 서비스가 거부됨 | receiver 대신 사용자 시작/companion 전략 검토 |
| P-005 | `neverForLocation` scan에서 Apple frame이 수신된다. | 권한 정상인데 raw Apple frame 0건, 다른 scanner에서는 수신 | 위치 권한을 곧장 추가하지 않고 pending-intent scan/flag 비교 |
| P-006 | 연결 중 filtered scan의 배터리 비용은 사용자 수용 범위다. | 1시간 청취에서 앱이 비정상적인 상위 배터리 소비자가 됨 | scan mode와 duty cycle 수정 |
| P-007 | 위젯은 service의 저장 snapshot 갱신 직후 1초 안에 바뀐다. | store는 갱신됐지만 위젯이 수동 클릭 전까지 낡음 | widget broadcast 경로 수정 |
| P-008 | 연결 세션당 popup 한 번이다. | HFP/A2DP 중복 이벤트나 service 재시작에서 재노출 | session token 지속 방식 수정 |

## 2. 테스트 층

### T-1 순수 JVM

- 길이/type/length/prefix 거부
- 0, 10, unknown, invalid nibble
- 좌우 flip 두 방향
- 좌·우·케이스 charging flag
- snapshot equality와 stale 판정
- low-battery threshold state machine

### T-2 Android 설치 검사

- manifest 권한에 INTERNET, LOCATION, ACCESSIBILITY, MICROPHONE이 없음
- 선택적인 SYSTEM_ALERT_WINDOW는 6초 연결 카드 외 경로에서 사용하지 않음
- Bluetooth와 알림 권한 거부/허용 화면
- app widget picker에 `에어팟 한눈에` 노출
- 4×1과 축소 레이아웃 렌더링
- 재부팅 전후 캐시 표시

### T-3 실기기 프로토콜

- 양쪽 out-of-case
- 왼쪽만 case
- 오른쪽만 case
- case cable/wireless charging
- 같은 연결에서 10분 이상 갱신
- 3회 reconnect
- 회사처럼 다른 Bluetooth 기기가 보이는 환경에서 identity 오인 없음

### T-4 기존 자동화와 회귀

- 회의 모드가 Bluetooth를 끌 때 service와 ongoing notification 종료
- 수면 DND에서 화면이 켜지고 잠금이 풀렸다면 무음 연결 카드는 보이되 소리·진동은 내지 않음
- `맨 위로 톡` 접근성 overlay와 큰 카드가 겹쳐도 양쪽 서비스가 crash하지 않음
- MaterialPods가 함께 있던 단계와 제거 뒤 단계에서 새 앱이 모두 동작함

## 3. 피드백 기록 형식

각 실기기 시험은 `docs/results/YYYY-MM-DD-*.md`에 다음 순서로 남긴다.

1. 사용자 장면과 물리적 이어버드 상태
2. 구현 전 예측
3. 실제 화면과 진단값
4. 예측과 다른 점
5. 원인 후보와 배제 근거
6. 설계 문서의 어떤 문장을 정정했는지
7. 다음 시험에서 반증할 내용

실패를 “수정 완료”로 압축하지 않는다. 좌우가 뒤집혔다면 어떤 해석을 왜 사용했고 어느
물리 시험이 그것을 깨뜨렸는지 남겨야 다음 모델에서 같은 오류를 재현할 수 있다.

## 4. 종료 기준

1차 버전 완료는 APK가 설치되는 시점이 아니다. 다음을 모두 만족해야 한다.

- 사용자의 AirPods에서 좌·우·케이스 값이 3회 연결에 걸쳐 재현됨
- 한쪽 충전 시험으로 좌우 해석 확인
- 위젯에서 같은 snapshot과 freshness 표시
- 연결 중 알림 하나, 세션 popup 하나, 연결 해제 뒤 ongoing 알림 0개
- 인터넷·위치·마이크·접근성 권한 0개, 선택 overlay 권한은 6초 카드에만 사용
- 회의 모드 Bluetooth off 회귀 통과
- MaterialPods 제거 전후 모두 새 앱 단독 동작 확인
