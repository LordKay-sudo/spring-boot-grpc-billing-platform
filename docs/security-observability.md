# Security and Observability Baseline

This milestone introduces baseline production controls for service-to-service communication and platform telemetry.

## Security
- gRPC server TLS configuration keys are defined in each service:
  - `spring.grpc.server.security.enabled`
  - `spring.grpc.server.security.certificate-chain`
  - `spring.grpc.server.security.private-key`
- Usage ingestion gRPC clients now support TLS to downstream services:
  - `billing.rating.tls.enabled`
  - `billing.rating.tls.trust-cert`
  - `billing.invoicing.tls.enabled`
  - `billing.invoicing.tls.trust-cert`

Local development defaults to plaintext; production can be switched to TLS by config.

## Observability
- Prometheus metrics export enabled via Micrometer registry dependency.
- OpenTelemetry tracing bridge dependency is enabled.
- Trace and span ids are included in log level pattern for correlation.
- Each service exposes actuator endpoints for health, info, metrics, and prometheus.

## Contract Governance
- CI enforces protobuf quality and compatibility:
  - `buf lint` on every push and pull request
  - `buf breaking --against '.git#branch=main'` on pull requests
