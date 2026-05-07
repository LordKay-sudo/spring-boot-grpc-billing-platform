package com.lordkay.billing.invoicing.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import com.lordkay.billing.proto.v1.CreateInvoiceRequest;
import com.lordkay.billing.proto.v1.CreateInvoiceResponse;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class InvoicingGrpcServiceTest {

	@Test
	void createInvoiceReturnsDraftInvoice() {
		InvoicingGrpcService service = new InvoicingGrpcService();
		List<CreateInvoiceResponse> responses = new ArrayList<>();

		service.createInvoice(CreateInvoiceRequest.newBuilder()
			.setTenantId("tenant-1")
			.setUsageEventId("evt-1")
			.setAmountMinor(240)
			.setCurrencyCode("USD")
			.build(), new StreamObserver<>() {
				@Override
				public void onNext(CreateInvoiceResponse value) {
					responses.add(value);
				}

				@Override
				public void onError(Throwable t) {
					throw new AssertionError("No error expected", t);
				}

				@Override
				public void onCompleted() {
					// no-op
				}
			});

		assertThat(responses).hasSize(1);
		assertThat(responses.getFirst().getStatus()).isEqualTo("DRAFT");
		assertThat(responses.getFirst().getTotalMinor()).isEqualTo(240);
		assertThat(responses.getFirst().getInvoiceId()).startsWith("inv-");
	}
}
