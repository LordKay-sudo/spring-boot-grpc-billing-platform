# Architecture Notes

## Current baseline

- Contract-first APIs with protobuf under `proto/billing/v1`
- Three standalone Spring Boot 4 gRPC services
- Usage ingestion persists events to PostgreSQL via Flyway-managed schema
- Idempotency enforced with a unique constraint on `(tenant_id, idempotency_key)`
- Synchronous orchestration from ingestion → rating → invoicing for demo simplicity

## Design choices

### Persist before downstream calls

Usage is written to the database before rating or invoicing runs. That keeps intake durable even when downstream services fail and gives idempotency a stable anchor.

### Idempotency at the API boundary

Clients supply an `idempotency_key`. Retries with the same tenant + key return the original stored result instead of creating duplicate billable events.

### Graceful degradation

Rating and invoicing failures are handled independently. Ingestion remains available and records the outcome on the stored usage event.

## Known simplifications

- Rating uses hard-coded meter prices in memory
- Invoicing creates a draft invoice per usage event rather than aggregating by billing period
- No message bus or async worker yet
- TLS configuration keys exist but local dev runs plaintext

See `PROJECT_PLAN.md` for the longer-term target architecture.
