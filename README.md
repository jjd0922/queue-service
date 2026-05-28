# queue-service

[![CI](https://github.com/jjd0922/queue-service/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/jjd0922/queue-service/actions/workflows/ci.yml)
[![Coverage](https://codecov.io/gh/jjd0922/queue-service/branch/main/graph/badge.svg)](https://codecov.io/gh/jjd0922/queue-service)
[![Java](https://img.shields.io/badge/Java-17-orange)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.2-green)]()
[![Redis](https://img.shields.io/badge/Redis-7-red)]()
[![Kafka](https://img.shields.io/badge/Kafka-3.9.2-black)]()
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)]()

대규모 트래픽 진입 시 사용자를 공정한 순번으로 대기시키고, 제한된 active 사용자만 downstream으로 통과시키는 Spring Boot 기반 대기열 포트폴리오 프로젝트이다.

상세 흐름, 아키텍처 다이어그램, Redis 원자 처리, Outbox relay, 모니터링, 부하 테스트 기준은 [상세문서](#상세문서)를 참고한다.

## 주요 특징

- Redis sorted set 기반 waiting/active queue로 사용자 순번과 입장 가능 상태를 관리한다.
- Lua script로 중복 진입 방지, 대기열 승격, active 만료 처리를 Redis 안에서 원자적으로 수행한다.
- 사용자별 `queue:user-index:{queueId}:{userId}` 키로 같은 사용자의 반복 진입을 멱등 처리한다.
- active TTL과 만료 zset을 분리해 만료 대상 조회와 active 제거를 배치 처리한다.
- Queue lifecycle event를 MySQL Outbox에 먼저 저장하고 polling dispatcher가 Kafka로 발행한다.
- Outbox dispatcher는 `PENDING`/`FAILED` 이벤트를 claim 후 발행하고 실패 시 retry/backoff/`DEAD` 상태로 관리한다.
- Kafka consumer는 lifecycle event를 audit history로 저장하고 duplicate event를 무시한다.
- consumer retry, DLT, outbox dispatch, consumer lag, 처리 지연 시간 지표를 구성한다.
- `X-Trace-Id`, MDC, Actuator/Prometheus 엔드포인트로 요청 추적과 기본 운영 관측성을 구성한다.
- Gradle multi-module과 port 기반 계층 분리로 domain/application/infrastructure/API 책임을 분리한다.

## 빠른 시작

```bash
docker compose up -d
./gradlew :queue-api:bootRun
```

Windows:

```bash
docker compose up -d
.\gradlew.bat :queue-api:bootRun
```

```text
API                  http://localhost:8081
Health               http://localhost:8081/actuator/health
Prometheus 지표      http://localhost:8081/actuator/prometheus
Prometheus           http://localhost:9090
Grafana              http://localhost:3000
```

## 테스트

```bash
./gradlew test
```

Windows:

```bash
.\gradlew.bat test
```

테스트는 도메인 상태 전이, application use case, API 계약, Redis Lua 원자 처리, Kafka consumer, Outbox 저장/dispatcher, audit retention worker를 검증한다.

## 기술 스택

| 영역 | 사용 기술 |
| --- | --- |
| 언어/런타임 | Java 17 |
| 프레임워크 | Spring Boot 3.3.2, Spring Web, Validation, Actuator |
| 대기열 저장소 | Redis 7, Redis Lua script, sorted set/hash/string |
| 이벤트/감사 | Kafka 3.9.2, MySQL 8.0, Flyway, JDBC |
| 아키텍처 | Gradle Multi-Module, Port 기반 계층 분리 |
| 관측성 | Micrometer, Prometheus registry, Grafana, MDC request tracing |
| 테스트 | JUnit 5, AssertJ, Mockito, Testcontainers |
| 로컬 실행 | Docker Compose, Gradle |

## 모듈

```text
queue-service
├── queue-api              # HTTP API, 요청/응답 DTO, 오류 응답, 요청 추적, Flyway migration
├── queue-application      # UseCase, command/query/result DTO, Port 정의
├── queue-domain           # 대기열 엔트리, 상태 전이, lifecycle event 도메인 규칙
├── queue-infrastructure   # Redis adapter/script, Kafka, MySQL audit/outbox, worker, 지표
├── docker                 # Prometheus/Grafana provisioning
├── monitoring             # HTTP request sample, k6 scenario
├── docs                   # 설계 문서와 ADR
└── docker-compose.yml
```

## 주요 API

```http
POST /api/v1/queues/enter
GET /api/v1/queues/{queueName}/entries/{queueToken}
```

대기열 진입은 `queueId`와 `userId`를 기준으로 멱등 처리한다. 동일 사용자가 이미 `WAITING` 또는 `ACTIVE` 상태이면 기존 token과 상태를 반환한다.

```json
{
  "queueId": "product:100",
  "userId": 100000
}
```

상태 조회는 `WAITING`, `ACTIVE`, `EXPIRED`, `CANCELLED`를 반환한다. `EXPIRED`와 `CANCELLED`는 terminal 상태이므로 `position`과 `aheadCount`는 반환하지 않는다.

## 문서

- [아키텍처 다이어그램](docs/architecture-diagrams.md)
- [대기열 라이프사이클](docs/queue-lifecycle.md)
- [Redis 자료구조와 Lua 원자 처리](docs/redis-queue-design.md)
- [라이프사이클 Outbox와 Kafka 감사](docs/lifecycle-outbox.md)
- [모니터링과 운영 지표](docs/monitoring.md)
- [부하 테스트와 측정 기준](docs/load-test.md)
- [ADR 0001: Redis Lua 기반 원자 처리](docs/adr/0001-redis-lua-atomic-queue.md)
- [ADR 0002: 라이프사이클 Outbox 폴링 디스패처](docs/adr/0002-lifecycle-outbox-polling-dispatcher.md)
- [ADR 0003: 종료 상태 조회 정책](docs/adr/0003-terminal-status-query-policy.md)
