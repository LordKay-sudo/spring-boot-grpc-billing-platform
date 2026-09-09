package com.lordkay.billing.invoicing.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lordkay.billing.invoicing.service.InvoiceAggregationService;
import com.lordkay.billing.proto.v1.CreateInvoiceRequest;
import com.lordkay.billing.proto.v1.CreateInvoiceResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvoicingGrpcServiceTest {

	@Mock
	private InvoiceAggregationService invoiceAggregationService;

	@InjectMocks
	private InvoicingGrpcService service;

	@Test
	void createInvoiceReturnsAggregatedInvoice() {
		when(invoiceAggregationService.applyRatedUsage(validRequest())).thenReturn(CreateInvoiceResponse.newBuilder()
			.setInvoiceId("inv-123")
			.setTenantId("tenant-1")
			.setTotalMinor(240)
			.setCurrencyCode("USD")
			.setStatus("DRAFT")
			.setBillingPeriodKey("2024-05")
			.build());
		List<CreateInvoiceResponse> responses = new ArrayList<>();

		service.createInvoice(validRequest(), responseObserver(responses, null));

		assertThat(responses).hasSize(1);
		assertThat(responses.getFirst().getStatus()).isEqualTo("DRAFT");
		assertThat(responses.getFirst().getTotalMinor()).isEqualTo(240);
		assertThat(responses.getFirst().getBillingPeriodKey()).isEqualTo("2024-05");
		verify(invoiceAggregationService).applyRatedUsage(validRequest());
	}

	@Test
	void createInvoiceRejectsInvalidRequest() {
		List<CreateInvoiceResponse> responses = new ArrayList<>();
		List<Throwable> errors = new ArrayList<>();

		service.createInvoice(CreateInvoiceRequest.newBuilder()
			.setTenantId("tenant-1")
			.setUsageEventId("evt-1")
			.setAmountMinor(240)
			.setCurrencyCode("USD")
			.setBillingPeriodKey("")
			.build(), responseObserver(responses, errors));

		assertThat(responses).isEmpty();
		assertThat(errors).hasSize(1);
		assertThat(((StatusRuntimeException) errors.getFirst()).getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
	}

	private CreateInvoiceRequest validRequest() {
		return CreateInvoiceRequest.newBuilder()
			.setTenantId("tenant-1")
			.setUsageEventId("evt-1")
			.setAmountMinor(240)
			.setCurrencyCode("USD")
			.setBillingPeriodKey("2024-05")
			.build();
	}

	private StreamObserver<CreateInvoiceResponse> responseObserver(List<CreateInvoiceResponse> responses, List<Throwable> errors) {
		return new StreamObserver<>() {
			@Override
			public void onNext(CreateInvoiceResponse value) {
				responses.add(value);
			}

			@Override
			public void onError(Throwable t) {
				if (errors != null) {
					errors.add(t);
				}
				else {
					throw new AssertionError("No error expected", t);
				}
			}

			@Override
			public void onCompleted() {
				// no-op
			}
		};
	}
}
