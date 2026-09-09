# spring-boot-grpc-billing-platform

Multi-tenant billing platform lab built with **Spring Boot 4**, **gRPC**, and **Protobuf**.

This repo demonstrates contract-first microservice design, protobuf governance in CI, and a realistic usage-ingestion path with **persistence and idempotency**. Rating and invoicing are intentionally simplified stubs to keep the focus on gRPC orchestration and operational patterns.

## What works today

- Three gRPC services: `usage-ingestion-service`, `rating-service`, `invoicing-service`
- Shared protobuf contracts under `proto/` with Buf lint + breaking checks in CI
- Usage ingestion persists events to PostgreSQL and enforces `(tenant_id, idempotency_key)` uniqueness
- Invoicing aggregates rated usage into monthly period invoices per tenant and currency
- Duplicate requests return the stored result without re-rating or re-invoicing
- Input validation returns gRPC `INVALID_ARGUMENT` for bad requests
- Downstream gRPC calls use deadlines; rating and invoicing failures degrade independently
- Async rating via a scheduled worker: ingest returns `QUEUED` immediately, billing runs in the background
- Smoke scripts and an ops runbook for local demos

## What is intentionally out of scope

- Message queues (Kafka/RabbitMQ) for billing workflows
- mTLS, JWT, and production-grade auth
- Grafana dashboards and full OpenTelemetry export wiring
- Multi-module Gradle build (each service is standalone today)

## Architecture

```text
Client --gRPC--> usage-ingestion-service --gRPC--> rating-service
                              |
                              +--gRPC--> invoicing-service
                              |
                              +--PostgreSQL (usage_events)
```

Happy path: validate → persist usage → return `QUEUED` → worker rates and applies to period invoice.

Duplicate path: lookup by idempotency key → return stored response (`QUEUED` while still pending).

Degraded path: persist usage first, then tolerate rating or invoicing outages without losing the intake record.

## Quickstart

### 1. Start PostgreSQL

```powershell
cd infra
docker compose up -d
```

### 2. Start services (separate terminals)

```powershell
cd rating-service
.\gradlew bootRun
```

```powershell
cd invoicing-service
.\gradlew bootRun
```

```powershell
cd usage-ingestion-service
.\gradlew bootRun
```

Ports:

| Service | gRPC | HTTP (actuator) |
|---|---:|---:|
| usage-ingestion-service | 9090 | 8080 |
| rating-service | 9091 | 8081 |
| invoicing-service | 9092 | 8082 |

### 3. Smoke test

```powershell
.\scripts\smoke\grpcurl-usage.ps1
```

Send the same request twice with the same `idempotencyKey` and you should get the same `usageEventId` back.

The first ingest response should be `QUEUED`. After a few seconds, the stored usage event should move to `ACCEPTED` once the async worker completes rating and invoicing.

## Development

Generate protobuf stubs:

```powershell
cd usage-ingestion-service
.\gradlew generateProto
```

Run tests:

```powershell
cd usage-ingestion-service
.\gradlew test
```

## Repository layout

- `proto/` — shared protobuf contracts and Buf config
- `usage-ingestion-service/` — ingestion API, persistence, orchestration
- `rating-service/` — stub rating logic
- `invoicing-service/` — period invoice aggregation with PostgreSQL
- `infra/` — local PostgreSQL via Docker Compose
- `docs/` — architecture, security/observability notes, runbook
- `scripts/smoke/` — grpcurl smoke scripts

## Next steps toward production

1. Process rating/invoicing asynchronously after durable intake
2. Add Testcontainers-backed integration tests in CI
3. Consolidate Gradle modules and shared proto generation
4. Wire TLS/mTLS and service auth for internal calls

See `PROJECT_PLAN.md` for the longer roadmap.
