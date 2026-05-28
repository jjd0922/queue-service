# ADR 0003: 종료 상태 조회 정책

## 배경

Redis entry hash는 active 만료 이후에도 `EXPIRED` 상태로 남는다. 조회 정책이 active/waiting에 없는 token을 모두 not found로 처리하면 저장 상태와 API 응답 정책이 어긋난다.

## 결정

entry hash가 존재하고 상태가 `EXPIRED` 또는 `CANCELLED`이면 상태 조회 API는 terminal 상태를 반환한다.

응답 정책은 다음과 같다.

- `EXPIRED`: `position`, `aheadCount` 없음
- `CANCELLED`: `position`, `aheadCount` 없음
- 존재하지 않는 token: not found
- entry는 있지만 waiting/active/terminal 어디에도 해당하지 않는 비정상 상태: not found

## 결과

- 클라이언트는 token의 최종 상태를 확인할 수 있다.
- terminal 상태에 순번 정보가 없다는 정책을 API 문서와 테스트로 고정해야 한다.
