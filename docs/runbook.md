# Operational Runbook

## Local startup order

1. Start PostgreSQL: `cd infra && docker compose up -d`
2. Start `rating-service` on gRPC `9091` / HTTP `8081`
3. Start `invoicing-service` on gRPC `9092` / HTTP `8082`
4. Start `usage-ingestion-service` on gRPC `9090` / HTTP `8080`

## Health endpoints

- `usage-ingestion-service`: `http://localhost:8080/actuator/health`
- `rating-service`: `http://localhost:8081/actuator/health`
- `invoicing-service`: `http://localhost:8082/actuator/health`

## Smoke checks

- `scripts/smoke/grpcurl-rating.ps1`
- `scripts/smoke/grpcurl-invoicing.ps1`
- `scripts/smoke/grpcurl-usage.ps1`

## Async rating

By default, `billing.async-rating.enabled=true`. Ingest persists the usage event and returns `QUEUED` without blocking on rating or invoicing.

A scheduled worker polls `PENDING` usage events every 2 seconds and runs rating + invoicing in the background.

To disable async rating for debugging:

```properties
billing.async-rating.enabled=false
```

## Idempotency check

Run the usage smoke script twice without changing `idempotencyKey`. The second response should return the same `usageEventId` and message indicating a duplicate idempotency key.

## Period aggregation check

Run the usage smoke script twice with different `idempotencyKey` values but the same `occurredAtEpochMs` month. Both responses should reference the same `invoiceId`, and the second response should show a higher invoice total in the invoicing service database.

## Degradation behavior

`usage-ingestion-service` degrades gracefully when rating or invoicing is unavailable:

- rating failure → `ACCEPTED_WITH_DEGRADATION`, rated amount `0`, empty invoice id
- invoicing failure after successful rating → `ACCEPTED_WITH_DEGRADATION`, rated amount preserved, empty invoice id

Usage events are still persisted before downstream calls.

## Failure simulation

1. Stop `rating-service` and call the usage smoke script.
2. Observe `ACCEPTED_WITH_DEGRADATION`.
3. Restore `rating-service` and confirm normal `ACCEPTED` flow resumes.
4. Repeat with `invoicing-service` stopped to verify partial degradation.

## Rollback note

If a release introduces failures in ingestion orchestration, revert to the previous tagged milestone branch and redeploy the prior stable image.
