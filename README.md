# queue-service

Queue lifecycle event 발행/소비, audit 적재, 관측성(메트릭/로그/대시보드)까지 포함한 샘플 서비스입니다.

## 로컬 실행 (Docker Desktop)

### 1) 사전 준비
- Docker Desktop 실행
- 권장 리소스: CPU 4core+, Memory 8GB+
- Docker Desktop에서 `Use Docker Compose V2` 활성화

### 2) 전체 스택 실행
```powershell
docker compose up -d --build
```

실행 컴포넌트:
- `queue-api` (Spring Boot): `http://localhost:8080`
- `mysql`: `localhost:3306`
- `redis`: `localhost:6379`
- `kafka`: `localhost:9092`
- `prometheus`: `http://localhost:9090`
- `grafana`: `http://localhost:3000` (`admin/admin`)

### 3) 기본 동작 확인
```powershell
curl -X POST "http://localhost:8080/api/v1/queues/enter" `
  -H "Content-Type: application/json" `
  -d "{\"queueId\":\"default\",\"userId\":1001}"
```

### 4) 메트릭/대시보드 확인
- Prometheus scrape: `http://localhost:8080/actuator/prometheus`
- Grafana 대시보드: `Queue / Queue Lifecycle Observability`

## 관측 포인트
- Consumer lag: `queue_lifecycle_consumer_lag`
- 처리량: `queue_lifecycle_consumer_consumed_total`
- 실패율 계산용: `queue_lifecycle_consumer_failure_total`
- 중복 무시: `queue_lifecycle_consumer_duplicate_ignored_total`
- 재시도/소진: `queue_lifecycle_consumer_retry_total`, `queue_lifecycle_consumer_retry_exhausted_total`
- DLT 전송: `queue_lifecycle_consumer_dlt_published_total`

로그는 `traceId`, `eventId`, `topic`, `partition`, `offset`를 공통 포맷으로 출력합니다.

## 스키마 관리
- Flyway 마이그레이션 사용
- 위치: `queue-api/src/main/resources/db/migration`
