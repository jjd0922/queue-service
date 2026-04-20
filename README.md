# Queue Service

대기열 진입/승격/만료 흐름을 Redis로 처리하고, lifecycle 이벤트를 Kafka로 발행/소비하여 Audit 이력을 저장하는 서비스.  
운영 관측을 위해 메트릭(Prometheus)과 대시보드(Grafana), 추적 가능한 로그 포맷을 포함.

## 1. 시스템 개요

핵심 흐름:
- 사용자 요청으로 대기열 엔트리 생성 또는 기존 엔트리 조회
- Worker가 대기열 승격/만료 배치 처리
- lifecycle 이벤트를 Kafka로 발행
- Consumer가 이벤트를 Audit 테이블에 멱등 저장
- 운영 지표를 Prometheus/Grafana로 확인

주요 속성:
- 멱등성: `event_id` 기준 중복 무시
- 장애 대응: retry/backoff + DLT
- 관측성: lag, 처리량, 실패율, 중복 무시 건수, 처리 지연

## 2. 모듈 구조

- `queue-api`
  - REST API, 요청/응답 DTO, 전역 예외 처리, trace 필터
- `queue-application`
  - 유스케이스, 포트(in/out), 서비스 오케스트레이션
- `queue-domain`
  - 엔티티/도메인 이벤트/도메인 예외
- `queue-infrastructure`
  - Redis/Kafka/JDBC 어댑터, Worker, 외부 연동 설정

## 3. 기술 스택

- Java 17, Spring Boot 3.x
- Redis, Kafka(KRaft), MySQL 8
- Flyway
- Micrometer + Prometheus
- Grafana
- Docker Compose

## 4. 로컬 실행 가이드

사전 조건:
- Docker Desktop 실행
- Docker Compose V2 활성화

실행:
```powershell
docker compose down -v
docker compose up -d --build
```

접속 정보:
- API: `http://localhost:8081`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000` (`admin/admin`)
- MySQL: `localhost:3307`
- Redis: `localhost:6379`
- Kafka(호스트 실행 앱): `localhost:9094`
- Kafka(컨테이너 간): `kafka:9092`

## 5. 로컬 앱 직접 실행 시 환경 변수

```powershell
$env:QUEUE_API_PORT="8081"
$env:QUEUE_DB_URL="jdbc:mysql://localhost:3307/queue_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:QUEUE_DB_USERNAME="root"
$env:QUEUE_DB_PASSWORD="root"
$env:QUEUE_KAFKA_BOOTSTRAP_SERVERS="localhost:9094"
$env:QUEUE_KAFKA_CONSUMER_GROUP="queue-lifecycle-audit-v1"
```

## 6. 기능 확인 예시

```powershell
curl -X POST "http://localhost:8081/api/v1/queues/enter" `
  -H "Content-Type: application/json" `
  -d "{\"queueId\":\"default\",\"userId\":1001}"
```

확인 포인트:
- 응답의 `token`, `status`, `outcome`
- Consumer 로그의 `traceId`, `eventId`
- MySQL audit 적재 여부

## 7. 운영 관측 가이드

Prometheus scrape endpoint:
- `http://localhost:8081/actuator/prometheus`

핵심 메트릭:
- lag: `queue_lifecycle_consumer_lag`
- 처리량: `queue_lifecycle_consumer_consumed_total`
- 성공/실패: `queue_lifecycle_consumer_success_total`, `queue_lifecycle_consumer_failure_total`
- 중복 무시: `queue_lifecycle_consumer_duplicate_ignored_total`
- 재시도: `queue_lifecycle_consumer_retry_total`, `queue_lifecycle_consumer_retry_exhausted_total`
- DLT: `queue_lifecycle_consumer_dlt_published_total`
- 처리 지연: `queue_lifecycle_consumer_processing_latency`

Grafana:
- 기본 대시보드: `Queue Lifecycle Observability`
- 권장 확인 순서: 처리량 -> 실패율 -> lag -> 중복 무시

## 8. 데이터/스키마 관리

- Flyway 기반 마이그레이션 적용
- 경로: `queue-api/src/main/resources/db/migration`

## 9. 장애 대응 체크리스트

- `Unknown database 'queue_db'`
  - DB 포트/URL이 `3307` 기준인지 확인
- Kafka `UnknownHostException: kafka`
  - 로컬 실행 시 `localhost:9094` 사용 여부 확인
- `Port already in use`
  - API 기본 포트 `8081` 충돌 여부 확인
- Prometheus 수집 실패
  - `targets`에서 `queue-api`가 `UP`인지 확인
