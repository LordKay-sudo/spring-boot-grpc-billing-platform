package com.lordkay.billing.usageingestion.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lordkay.billing.proto.v1.UsageEventRequest;
import com.lordkay.billing.proto.v1.UsageEventResponse;
import com.lordkay.billing.usageingestion.service.UsageIngestionOrchestrator;
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
class UsageIngestionGrpcServiceTest {

	@Mock
	private UsageIngestionOrchestrator orchestrator;

	@InjectMocks
	private UsageIngestionGrpcService service;

	@Test
	void ingestUsageReturnsAcceptedResponse() {
		when(orchestrator.ingest(validRequest())).thenReturn(UsageEventResponse.newBuilder()
			.setUsageEventId("evt-1")
			.setStatus("ACCEPTED")
			.setMessage("Usage event accepted for tenant tenant-1")
			.setRatedAmountMinor(126)
			.setInvoiceId("inv-123")
			.build());
		List<UsageEventResponse> responses = new ArrayList<>();

		service.ingestUsage(validRequest(), responseObserver(responses, null));

		assertThat(responses).hasSize(1);
		assertThat(responses.getFirst().getStatus()).isEqualTo("ACCEPTED");
		verify(orchestrator).ingest(validRequest());
	}

	@Test
	void ingestUsageRejectsInvalidRequest() {
		List<UsageEventResponse> responses = new ArrayList<>();
		List<Throwable> errors = new ArrayList<>();

		service.ingestUsage(UsageEventRequest.newBuilder()
			.setTenantId("")
			.setMeterId("api-calls")
			.setIdempotencyKey("key-123")
			.setQuantity(42)
			.setOccurredAtEpochMs(1715068800000L)
			.build(), responseObserver(responses, errors));

		assertThat(responses).isEmpty();
		assertThat(errors).hasSize(1);
		assertThat(errors.getFirst()).isInstanceOf(StatusRuntimeException.class);
		assertThat(((StatusRuntimeException) errors.getFirst()).getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
	}

	private UsageEventRequest validRequest() {
		return UsageEventRequest.newBuilder()
			.setTenantId("tenant-1")
			.setMeterId("api-calls")
			.setIdempotencyKey("key-123")
			.setQuantity(42)
			.setOccurredAtEpochMs(1715068800000L)
			.build();
	}

	private StreamObserver<UsageEventResponse> responseObserver(List<UsageEventResponse> responses, List<Throwable> errors) {
		return new StreamObserver<>() {
			@Override
			public void onNext(UsageEventResponse value) {
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
