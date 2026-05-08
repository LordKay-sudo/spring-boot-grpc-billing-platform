# spring-boot-grpc-billing-platform

Multi-tenant billing platform built with Spring Boot 4, gRPC, and Protobuf.

## Milestone 1 Scope
- mono-repo skeleton (`proto`, `services`, `docs`, `infra`)
- v1 protobuf contract for usage ingestion
- Spring Boot 4 gRPC ingestion service
- local run instructions and smoke test command

## Milestone 2 Scope
- `rating-service` and `invoicing-service` gRPC services
- inter-service flow from ingestion -> rating -> invoicing
- expanded protobuf contracts for rating and invoice creation
- service-level unit tests for core RPC methods

## Milestone 3 Scope
- TLS-ready gRPC server and client configuration knobs
- Prometheus + tracing dependencies and correlated log pattern
- protobuf contract governance checks in CI with Buf
- security and observability baseline documentation (`docs/security-observability.md`)

## Milestone 4 Scope
- resilience with gRPC deadlines and graceful degradation in ingestion flow
- failure-focused tests for downstream dependency outages
- grpcurl smoke scripts for all services under `scripts/smoke`
- operational runbook and failure simulation guidance (`docs/runbook.md`)

## Repository Layout
- `proto/` shared protobuf contracts
- `usage-ingestion-service/` first runnable service
- `rating-service/` usage rating RPC service
- `invoicing-service/` invoice creation RPC service
- `docs/` architecture and design notes
- `infra/` local infrastructure definitions

## Prerequisites
- Java 21+
- Internet access (Gradle Wrapper downloads Gradle automatically)

## Run Usage Ingestion Service
```powershell
cd "usage-ingestion-service"
.\gradlew bootRun
```

Default gRPC port: `9090`

## Generate protobuf code
```powershell
cd "usage-ingestion-service"
.\gradlew generateProto
```

## Smoke test with grpcurl
```powershell
grpcurl -plaintext -d "{\"tenantId\":\"tenant-1\",\"meterId\":\"api-calls\",\"idempotencyKey\":\"key-001\",\"quantity\":10,\"occurredAtEpochMs\":1715068800000}" localhost:9090 billing.v1.UsageIngestionService/IngestUsage
```

## Smoke scripts
```powershell
.\scripts\smoke\grpcurl-rating.ps1
.\scripts\smoke\grpcurl-invoicing.ps1
.\scripts\smoke\grpcurl-usage.ps1
```
