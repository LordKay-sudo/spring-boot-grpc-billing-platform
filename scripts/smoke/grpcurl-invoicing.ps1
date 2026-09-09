param(
    [string]$TargetHost = "localhost",
    [int]$Port = 9092
)

grpcurl -plaintext `
  -d "{\"tenantId\":\"tenant-1\",\"usageEventId\":\"evt-smoke-001\",\"amountMinor\":60,\"currencyCode\":\"USD\",\"billingPeriodKey\":\"2024-05\"}" `
  "$TargetHost`:$Port" `
  billing.v1.InvoicingService/CreateInvoice
