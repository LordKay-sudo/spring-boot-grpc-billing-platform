package com.lordkay.billing.usageingestion.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import com.lordkay.billing.proto.v1.UsageEventRequest;
import com.lordkay.billing.proto.v1.UsageEventResponse;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class UsageIngestionGrpcServiceTest {

	@Test
	void ingestUsageReturnsAcceptedResponse() {
		UsageIngestionGrpcService service = new UsageIngestionGrpcService();
		List<UsageEventResponse> responses = new ArrayList<>();

		service.ingestUsage(UsageEventRequest.newBuilder()
			.setTenantId("tenant-1")
			.setMeterId("api-calls")
			.setIdempotencyKey("key-123")
			.setQuantity(42)
			.setOccurredAtEpochMs(1715068800000L)
			.build(), new StreamObserver<>() {
				@Override
				public void onNext(UsageEventResponse value) {
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
		assertThat(responses.getFirst().getStatus()).isEqualTo("ACCEPTED");
		assertThat(responses.getFirst().getUsageEventId()).isNotBlank();
	}
}
