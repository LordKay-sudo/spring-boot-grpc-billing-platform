param(
    [string]$TargetHost = "localhost",
    [int]$Port = 9092
)

grpcurl -plaintext `
  -d "{\"tenantId\":\"tenant-1\",\"usageEventId\":\"evt-smoke-001\",\"amountMinor\":60,\"currencyCode\":\"USD\"}" `
  "$TargetHost`:$Port" `
  billing.v1.InvoicingService/CreateInvoice
