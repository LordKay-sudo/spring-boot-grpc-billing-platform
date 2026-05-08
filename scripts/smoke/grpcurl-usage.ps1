param(
    [string]$Host = "localhost",
    [int]$Port = 9090
)

grpcurl -plaintext `
  -d "{\"tenantId\":\"tenant-1\",\"meterId\":\"api-calls\",\"idempotencyKey\":\"smoke-usage-001\",\"quantity\":20,\"occurredAtEpochMs\":1715068800000}" `
  "$Host`:$Port" `
  billing.v1.UsageIngestionService/IngestUsage
