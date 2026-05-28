# 모니터링과 운영 지표

## 엔드포인트

```text
Health               /actuator/health
Prometheus 지표      /actuator/prometheus
```

## 요청 추적

- 요청마다 `X-Trace-Id`를 사용한다.
- header가 없으면 서버가 UUID를 생성한다.
- trace id는 response header와 MDC에 포함한다.

## 주요 지표

| 지표 | 의미 |
| --- | --- |
| `http_server_requests_seconds_*` | API 요청 수, 지연 시간, 응답 상태 |
| `queue_lifecycle_consumer_consumed_total` | Kafka audit consumer 수신 수 |
| `queue_lifecycle_consumer_success_total` | Kafka audit consumer 처리 성공 수 |
| `queue_lifecycle_consumer_failure_total` | Kafka audit consumer 처리 실패 수 |
| `queue_lifecycle_consumer_retry_total` | consumer retry 수 |
| `queue_lifecycle_consumer_dlt_published_total` | DLT 발행 수 |
| `queue_lifecycle_consumer_lag` | topic/partition/group별 lag |
| `queue_lifecycle_outbox_fetched_total` | dispatcher가 조회한 outbox event 수 |
| `queue_lifecycle_outbox_claimed_total` | dispatcher가 claim한 event 수 |
| `queue_lifecycle_outbox_claim_skipped_total` | claim 실패로 skip한 event 수 |
| `queue_lifecycle_outbox_published_total` | Kafka 발행 성공 event 수 |
| `queue_lifecycle_outbox_failed_total` | Kafka 발행 실패 event 수 |
| `queue_lifecycle_outbox_dead_total` | DEAD 처리 event 수 |
| `queue_lifecycle_outbox_dispatch_latency` | outbox event 처리 latency |

## Grafana

Docker Compose 실행 시 Grafana와 Prometheus provisioning이 함께 올라온다.

```text
Prometheus   http://localhost:9090
Grafana      http://localhost:3000
Login        admin / admin
```
