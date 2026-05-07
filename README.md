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
