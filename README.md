# beadv4_4_bugzero_BE
[2026 프로그래머스 백엔드 심화 4기] 버그제로 팀의 RareGo 레포지토리입니다.

## Infra Quickstart (Day 1)

1) `.env.example`를 `.env`로 복사한 뒤 값들을 채워주세요.
2) 인프라만 띄우기:

```
docker compose up -d db redis kafka elasticsearch prometheus grafana
```

필요하면 애플리케이션까지:

```
docker compose up -d
```

### 기본 접속 정보
- MySQL: `localhost:${DB_PORT:-3306}`
- Redis: `localhost:${REDIS_PORT:-6379}`
- Kafka: `localhost:${KAFKA_HOST_PORT:-29092}`
- Elasticsearch: `localhost:${ELASTICSEARCH_PORT:-9200}`
- Prometheus: `http://localhost:${PROMETHEUS_PORT:-9090}`
- Grafana: `http://localhost:${GRAFANA_PORT:-3001}` (기본 계정: `admin` / `admin`)
