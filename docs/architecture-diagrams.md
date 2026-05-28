# 아키텍처 다이어그램

이 문서는 현재 구현 기준으로 운영 관점에서 필요한 흐름을 정리한다. 핵심은 Redis 원자 처리, 상태 전이, Outbox 기반 비동기 복구 흐름이다.

## 대기열 진입

```mermaid
sequenceDiagram
    autonumber
    actor Client as 클라이언트
    participant API as 대기열 API
    participant App as EnterQueueUseCase
    participant Redis as Redis Lua
    participant Outbox as Outbox 저장소

    Client->>API: POST /api/v1/queues/enter
    API->>App: 대기열 진입 요청
    App->>Redis: enqueueOrGetExisting(queueId, userId)
    Redis->>Redis: user index로 중복 진입 확인
    alt 기존 엔트리 존재
        Redis-->>App: 기존 token과 상태 반환
    else 신규 진입
        Redis->>Redis: sequence 증가
        Redis->>Redis: entry hash 저장
        Redis->>Redis: waiting zset 추가
        Redis->>Redis: user index 저장
        Redis-->>App: CREATED 반환
        App->>Outbox: ENTERED event 저장
    end
    App->>Redis: waiting rank 조회
    App-->>API: token, status, position
    API-->>Client: 200 OK
```

## 대기열 승격과 만료

```mermaid
sequenceDiagram
    autonumber
    participant Scheduler as 만료/승격 스케줄러
    participant App as ExpireAndPromoteUseCase
    participant Redis as Redis Lua
    participant Outbox as Outbox 저장소

    Scheduler->>App: 만료 및 승격 처리 요청
    App->>Redis: expireActiveEntries(batchSize)
    Redis->>Redis: active-expiry zset에서 만료 token 조회
    Redis->>Redis: ACTIVE entry를 EXPIRED로 전이
    Redis->>Redis: active zset과 user index 정리
    Redis-->>App: 만료 건수

    App->>Redis: promoteWaitingEntries(maxActiveCount, batchSize)
    Redis->>Redis: active slot 계산
    Redis->>Redis: waiting 앞 순번부터 ACTIVE 전이
    Redis->>Redis: active/active-expiry zset 추가
    Redis-->>App: 승격된 entry 목록

    loop 승격된 entry별 처리
        App->>Outbox: ADMITTED event 저장
    end
    App-->>Scheduler: 만료/승격 처리 결과
```

## Outbox 발행 성공

```mermaid
sequenceDiagram
    autonumber
    participant Dispatcher as Outbox Dispatcher
    participant Outbox as Outbox 저장소
    participant Kafka as Kafka
    participant Consumer as Audit Consumer
    participant Audit as Audit History

    Dispatcher->>Outbox: PENDING/FAILED event 조회
    Outbox-->>Dispatcher: 발행 대상 목록
    Dispatcher->>Outbox: PROCESSING claim
    Outbox-->>Dispatcher: claim 성공
    Dispatcher->>Kafka: lifecycle event 발행
    Kafka-->>Dispatcher: 발행 성공
    Dispatcher->>Outbox: PUBLISHED 전이
    Kafka->>Consumer: event consume
    Consumer->>Audit: event_id 기준 insertIfAbsent
    Audit-->>Consumer: 저장 또는 중복 무시
```

## Outbox 발행 실패

```mermaid
sequenceDiagram
    autonumber
    participant Dispatcher as Outbox Dispatcher
    participant Outbox as Outbox 저장소
    participant Kafka as Kafka

    Dispatcher->>Outbox: PENDING/FAILED event 조회
    Outbox-->>Dispatcher: 발행 대상 목록
    Dispatcher->>Outbox: PROCESSING claim
    Outbox-->>Dispatcher: claim 성공
    Dispatcher->>Kafka: lifecycle event 발행
    Kafka-->>Dispatcher: 발행 실패
    alt 재시도 가능
        Dispatcher->>Outbox: FAILED, retry_count 증가, next_retry_at 갱신
    else 최대 재시도 초과
        Dispatcher->>Outbox: DEAD 전이
    end
```

## ERD

```mermaid
erDiagram
    QUEUE_LIFECYCLE_OUTBOX {
        bigint id PK
        string event_id
        string event_type
        string queue_token
        bigint user_id
        string status
        bigint sequence
        datetime occurred_at
        string reason
        string payload
        string publish_status
        int retry_count
        datetime next_retry_at
        datetime published_at
        string last_error_message
        datetime created_at
        datetime updated_at
    }

    QUEUE_LIFECYCLE_AUDIT_HISTORY {
        bigint id PK
        string event_id
        string event_type
        string queue_token
        bigint user_id
        string status
        bigint sequence
        datetime occurred_at
        string reason
        datetime received_at
        datetime created_at
    }
```

Redis entry와 queue 자료구조는 Redis key-value 구조이므로 ERD에는 포함하지 않는다. 관련 key 설계는 [Redis 자료구조와 Lua 원자 처리](redis-queue-design.md)를 참고한다.

## 상태 다이어그램

### 대기열 엔트리

```mermaid
stateDiagram-v2
    [*] --> WAITING: 진입
    WAITING --> ACTIVE: 승격
    ACTIVE --> EXPIRED: 만료
    WAITING --> CANCELLED: 취소
    ACTIVE --> CANCELLED: 취소
    EXPIRED --> [*]
    CANCELLED --> [*]
```

### Outbox 이벤트

```mermaid
stateDiagram-v2
    [*] --> PENDING: event 저장
    PENDING --> PROCESSING: dispatcher claim
    FAILED --> PROCESSING: 재시도 claim
    PROCESSING --> PUBLISHED: Kafka 발행 성공
    PROCESSING --> FAILED: 발행 실패 및 재시도 가능
    PROCESSING --> DEAD: 최대 재시도 초과
```
