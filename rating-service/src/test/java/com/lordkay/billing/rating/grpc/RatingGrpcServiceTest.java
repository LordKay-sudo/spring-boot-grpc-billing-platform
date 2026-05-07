package com.lordkay.billing.rating.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import com.lordkay.billing.proto.v1.RateUsageRequest;
import com.lordkay.billing.proto.v1.RateUsageResponse;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RatingGrpcServiceTest {

	@Test
	void rateUsageCalculatesAmountFromQuantityAndMeter() {
		RatingGrpcService service = new RatingGrpcService();
		List<RateUsageResponse> responses = new ArrayList<>();

		service.rateUsage(RateUsageRequest.newBuilder()
			.setUsageEventId("evt-1")
			.setTenantId("tenant-1")
			.setMeterId("api-calls")
			.setQuantity(50)
			.build(), new StreamObserver<>() {
				@Override
				public void onNext(RateUsageResponse value) {
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
		assertThat(responses.getFirst().getStatus()).isEqualTo("RATED");
		assertThat(responses.getFirst().getUnitPriceMinor()).isEqualTo(3);
		assertThat(responses.getFirst().getTotalAmountMinor()).isEqualTo(150);
	}
}
