# ADR-0004 — 공개 저자 alias와 package namespace를 유지한다

- 상태: 채택
- 날짜: 2026-09-07

## 맥락

개인정보 감사에서 GitHub 소유자 `chochoq`와 Android package namespace `com.chocho`가 개인 식별
가능한 문자열로 남아 있음을 별도로 알렸다. 이를 중립 namespace로 바꾸면 공개 저자와 설치된 앱의
identity가 갈라지고, 기존 설치본의 update 경로도 끊어진다.

## 결정

저장소 소유자는 `chochoq`가 자신의 GitHub nickname이므로 공개되어도 괜찮다고 확인했다.
`chochoq`와 기존 `com.chocho...` package namespace를 의도적인 공개 저자 식별자로 유지한다.
커밋 email은 계속 GitHub noreply 주소만 사용한다.

## 결과

이 alias를 집·회사 위치, Bluetooth 주소, serial, 개인 제품명, email 같은 제거 대상 사생활 정보와
구분한다. package를 중립 이름으로 복사해 서로 다른 앱을 만들지 않으며, 향후 rename이 필요하면
Android application ID migration으로 별도 설계한다.
