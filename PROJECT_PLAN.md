# gRPC Billing Platform (Spring Boot 4) - Milestone Build Plan

## Project Goal
Build a production-style, gRPC-first multi-tenant billing platform that demonstrates:
- contract-first API design with Protobuf
- schema evolution discipline with Buf
- secure service-to-service communication (TLS/mTLS + JWT at edge)
- observable microservices (metrics, tracing, logs)
- practical resilience patterns (timeouts, retries, idempotency)

This project is designed to look and feel like a real internal platform service instead of a tutorial app.

## Portfolio Positioning
Use this as your flagship backend project for recruiters and senior-level interviews.

Primary outcomes to showcase:
- system design depth (multi-service architecture)
- operational maturity (SLO-style metrics and dashboards)
- API governance (backward compatibility checks in CI)
- reliability under failure (tested retry/deadline behaviors)

## Tech Stack
- Java 21
- Spring Boot 4.x
- Spring gRPC
- Protobuf (proto3 or Editions)
- grpc-java ecosystem tooling
- PostgreSQL (billing state, invoice data)
- Redis (idempotency and short-lived workflow state)
- Kafka or RabbitMQ (optional, for usage ingestion decoupling)
- OpenTelemetry + Micrometer + Prometheus + Grafana
- Testcontainers (integration tests)
- Buf CLI (`buf lint`, `buf breaking`)
- Docker Compose for local orchestration
- grpcurl for smoke testing

## Architecture (Minimum Practical Slice)
Services:
- `usage-ingestion-service`
  - receives usage events via gRPC
  - validates tenant, meter, idempotency key
  - persists raw usage events
- `rating-service`
  - maps events to pricing rules
  - calculates billable amounts
  - emits rated usage records
- `invoicing-service`
  - aggregates rated usage by billing cycle
  - generates invoices and line items
  - exposes invoice retrieval APIs
- `tenant-service` (lightweight)
  - tenant metadata and plan assignment

Supporting components:
- shared `proto/` contracts module
- API gateway/edge adapter (optional REST facade)
- observability stack and dashboards

## Milestone Roadmap

## Milestone 1 - Foundation and Contracts
Milestones:
- set up mono-repo structure (`proto`, `services`, `infra`, `docs`)
- define v1 protobuf contracts for core billing flows
- enable code generation for Spring services/clients
- implement first gRPC server (`usage-ingestion-service`)
- commit local developer workflow (`docker compose up`, seed scripts)

Deliverables:
- runnable local environment with one end-to-end gRPC call
- baseline README with architecture diagram and quickstart
- CI workflow with build + unit tests

Senior-level criteria:
- clear API boundaries and field numbering discipline
- reserved fields/ids where future changes are expected
- proto package naming and versioning conventions documented

## Milestone 2 - Core Domain and Reliability
Milestones:
- implement `rating-service` and `invoicing-service`
- add synchronous inter-service gRPC calls with deadlines
- add retries with backoff for safe operations
- implement idempotency key handling in ingestion/rating path
- store billing aggregates and invoice snapshots

Deliverables:
- complete happy-path flow: usage -> rated usage -> invoice draft
- integration tests across services (Testcontainers)
- sequence diagrams for normal and retry paths

Senior-level criteria:
- documented error taxonomy and status code mapping
- deterministic idempotent handling for duplicate requests
- deadline propagation and timeout strategy explained

## Milestone 3 - Security, Observability, and Contract Governance
Milestones:
- configure TLS for local secure transport
- add mTLS between internal services
- add JWT validation at edge-facing service/API
- instrument tracing and metrics (OpenTelemetry/Micrometer)
- add Buf lint and breaking checks to CI

Deliverables:
- Grafana dashboard (latency, error rate, throughput per RPC)
- traces showing cross-service call chain
- CI fails on incompatible proto changes

Senior-level criteria:
- zero-trust service communication narrative
- concrete compatibility policy in repo docs
- SLO-ish target and error budget discussion in README

## Milestone 4 - Hardening, DX, and Portfolio Polish
Milestones:
- add chaos/failure scenarios (timeouts, unavailable dependency)
- validate fallback and partial failure behavior
- add grpcurl smoke-test scripts and Makefile/PowerShell tasks
- write architecture decisions (ADRs) for key tradeoffs
- produce a polished public README and demo assets

Deliverables:
- demo script with 3 scenarios: normal, retry, degraded dependency
- performance snapshot (p95 latency at given load)
- release tags and changelog

Senior-level criteria:
- explicit tradeoff documentation (consistency vs latency)
- production-minded runbook snippets (health, alerts, rollback)
- clear "what I would do next in production" section

## Minimum Impressive Version (MIV) Scope
Ship this if time is tight and still look senior:
- 3 services only (`usage-ingestion`, `rating`, `invoicing`)
- one secure channel setup (TLS) plus optional mTLS
- full observability for at least one end-to-end path
- Buf CI checks for lint and compatibility
- one chaos test case proving graceful degradation
- polished diagrams, ADRs, and demo video/gif in README

If all above is complete and clean, it is portfolio-worthy.

## Suggested Repository Structure
```text
grpc-billing-platform/
  proto/
    billing/v1/*.proto
    buf.yaml
    buf.gen.yaml
  services/
    usage-ingestion-service/
    rating-service/
    invoicing-service/
    tenant-service/
  infra/
    docker-compose.yml
    grafana/
    prometheus/
    certs/
  scripts/
    smoke/
      grpcurl-usage.ps1
      grpcurl-invoice.ps1
  docs/
    architecture.md
    adr/
      0001-contract-versioning.md
      0002-retry-and-deadline-policy.md
  .github/workflows/
    ci.yml
  README.md
```

## Interview-Ready Storyline
When presenting this project:
- Problem: internal billing systems require strict contracts and reliability.
- Why gRPC: strong contracts, performance, and typed cross-service communication.
- Challenges solved: schema evolution safety, retries/timeouts, distributed observability.
- Outcome: predictable APIs, operational transparency, and safer iteration.

## Next Immediate Build Steps
1. scaffold repo folders and Gradle/Maven parent build
2. define first protobufs for `IngestUsage`, `RateUsage`, `CreateInvoice`
3. implement `usage-ingestion-service` unary RPC and integration test
4. wire Buf lint and compatibility checks into CI
5. add basic dashboards and one grpcurl smoke script
