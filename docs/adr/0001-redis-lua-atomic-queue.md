# ADR 0001: Redis Lua 기반 원자 처리

## 배경

대기열 진입, 승격, 만료는 여러 Redis key를 동시에 변경한다. 애플리케이션에서 개별 Redis 명령을 순차 실행하면 동시 요청에서 중복 entry 생성, active slot 초과, stale index 잔존이 발생할 수 있다.

## 결정

다음 작업은 Redis Lua script로 처리한다.

- 사용자 중복 진입 확인과 신규 entry 생성
- waiting entry를 active로 승격
- active TTL 만료 entry 정리

## 결과

- Redis 단일 실행 단위로 race condition을 줄인다.
- script 입출력 포맷이 application/adapter와 강하게 연결되므로 테스트로 계약을 고정해야 한다.
- Redis 장애 시 대기열 핵심 기능이 영향을 받는다.
