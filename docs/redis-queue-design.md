# Redis 자료구조와 Lua 원자 처리

## Redis 키

| 키 | 타입 | 목적 |
| --- | --- | --- |
| `queue:sequence:{queueId}` | string counter | 진입 순번 발급 |
| `queue:waiting:{queueId}` | sorted set | waiting token과 sequence 관리 |
| `queue:active:{queueId}` | sorted set | active token과 sequence 관리 |
| `queue:active-expiry:{queueId}` | sorted set | active token의 만료 시각 관리 |
| `queue:entry:{token}` | hash | token별 상태 snapshot 저장 |
| `queue:user-index:{queueId}:{userId}` | string | 사용자별 중복 진입 방지 |

## Lua 스크립트

### `enqueue-or-get-existing.lua`

- user index에 기존 token이 있고 entry hash가 존재하면 기존 entry를 반환한다.
- user index만 남고 entry hash가 없으면 stale index로 보고 user index를 삭제한다.
- 신규 진입은 sequence 증가, entry hash 저장, waiting zset 추가, user index 저장을 하나의 script에서 처리한다.

### `promote-waiting-entries.lua`

- active zset cardinality로 남은 active slot을 계산한다.
- waiting zset 앞쪽 token부터 batch 단위로 `ACTIVE` 상태로 변경한다.
- waiting zset에서 제거하고 active/active-expiry zset에 추가한다.

### `expire_active_entries.lua`

- active-expiry zset에서 현재 시각 이하 token을 batch 조회한다.
- `ACTIVE` 상태 entry를 `EXPIRED`로 변경한다.
- active/active-expiry zset과 user index를 정리한다.

## 설계 의도

Redis 명령을 애플리케이션에서 여러 번 호출하면 동시 요청에서 중복 진입, 순번 역전, active slot 초과 같은 race condition이 발생할 수 있다. 이 프로젝트는 Redis Lua script를 사용해 주요 변경을 Redis 단일 실행 단위로 묶는다.
