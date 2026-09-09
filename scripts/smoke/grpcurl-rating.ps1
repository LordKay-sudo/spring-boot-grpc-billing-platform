param(
    [string]$TargetHost = "localhost",
    [int]$Port = 9091
)

grpcurl -plaintext `
  -d "{\"usageEventId\":\"evt-smoke-001\",\"tenantId\":\"tenant-1\",\"meterId\":\"api-calls\",\"quantity\":20}" `
  "$TargetHost`:$Port" `
  billing.v1.RatingService/RateUsage
