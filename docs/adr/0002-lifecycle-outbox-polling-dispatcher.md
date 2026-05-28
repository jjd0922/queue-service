# ADR 0002: 라이프사이클 Outbox 폴링 디스패처

## 배경

대기열 lifecycle event를 Kafka에 직접 발행하면 발행 실패 시 event가 유실될 수 있다. Audit consumer는 event를 기반으로 이력을 저장하므로 event 유실은 운영 추적성 저하로 이어진다.

## 결정

application service는 lifecycle event를 MySQL outbox에 저장한다. 별도 dispatcher가 outbox를 polling해 Kafka로 발행하고, 발행 결과에 따라 `PUBLISHED`, `FAILED`, `DEAD` 상태를 기록한다.

## 결과

- Kafka 일시 장애 시 event를 재시도할 수 있다.
- Outbox backlog, failure, dead event를 지표로 관측할 수 있다.
- Redis 상태 변경과 MySQL outbox 저장은 하나의 transaction이 아니므로 완전한 transactional outbox는 아니다.
- dispatcher 지연만큼 Kafka audit 반영은 eventual consistency가 된다.
