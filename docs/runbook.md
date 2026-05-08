# Operational Runbook

## Local startup order
1. Start `rating-service` on `9091`.
2. Start `invoicing-service` on `9092`.
3. Start `usage-ingestion-service` on `9090`.

## Health endpoints
- `usage-ingestion-service`: `http://localhost:8080/actuator/health`
- `rating-service`: `http://localhost:8080/actuator/health`
- `invoicing-service`: `http://localhost:8080/actuator/health`

If running all services locally at once, set unique `server.port` values per service for actuator HTTP.

## Smoke checks
- `scripts/smoke/grpcurl-rating.ps1`
- `scripts/smoke/grpcurl-invoicing.ps1`
- `scripts/smoke/grpcurl-usage.ps1`

## Degradation behavior
`usage-ingestion-service` is configured to degrade gracefully if rating or invoicing is unavailable:
- response status becomes `ACCEPTED_WITH_DEGRADATION`
- rated amount defaults to `0`
- invoice id is empty

This keeps usage intake available during downstream incidents.

## Failure simulation
- Stop `rating-service` and call usage ingestion smoke script.
- Observe `ACCEPTED_WITH_DEGRADATION` response.
- Restore `rating-service` and confirm normal `ACCEPTED` flow resumes.

## Rollback note
If a release introduces failures in ingestion orchestration, revert to the previous tagged milestone branch and redeploy the prior stable image.
