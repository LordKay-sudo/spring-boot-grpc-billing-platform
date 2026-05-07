package com.lordkay.billing.usageingestion.grpc;

import com.lordkay.billing.proto.v1.CreateInvoiceResponse;

public interface InvoicingGateway {

	CreateInvoiceResponse createInvoice(String tenantId, String usageEventId, long amountMinor, String currencyCode);
}
