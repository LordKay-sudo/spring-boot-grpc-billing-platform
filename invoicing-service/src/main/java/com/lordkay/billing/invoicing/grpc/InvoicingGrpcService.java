package com.lordkay.billing.invoicing.grpc;

import com.lordkay.billing.invoicing.service.InvoiceAggregationService;
import com.lordkay.billing.proto.v1.CreateInvoiceRequest;
import com.lordkay.billing.proto.v1.CreateInvoiceResponse;
import com.lordkay.billing.proto.v1.InvoicingServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

@Service
public class InvoicingGrpcService extends InvoicingServiceGrpc.InvoicingServiceImplBase {

	private final InvoiceAggregationService invoiceAggregationService;

	public InvoicingGrpcService(InvoiceAggregationService invoiceAggregationService) {
		this.invoiceAggregationService = invoiceAggregationService;
	}

	@Override
	public void createInvoice(CreateInvoiceRequest request, StreamObserver<CreateInvoiceResponse> responseObserver) {
		try {
			validate(request);
			CreateInvoiceResponse response = invoiceAggregationService.applyRatedUsage(request);
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		}
		catch (RuntimeException ex) {
			responseObserver.onError(ex);
		}
	}

	private void validate(CreateInvoiceRequest request) {
		if (request.getTenantId().isBlank()
			|| request.getUsageEventId().isBlank()
			|| request.getCurrencyCode().isBlank()
			|| request.getBillingPeriodKey().isBlank()) {
			throw Status.INVALID_ARGUMENT.withDescription("tenant_id, usage_event_id, currency_code, and billing_period_key are required")
				.asRuntimeException();
		}
		if (request.getAmountMinor() <= 0) {
			throw Status.INVALID_ARGUMENT.withDescription("amount_minor must be greater than zero").asRuntimeException();
		}
	}
}
