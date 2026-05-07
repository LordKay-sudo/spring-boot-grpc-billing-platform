package com.lordkay.billing.rating.grpc;

import com.lordkay.billing.proto.v1.RateUsageRequest;
import com.lordkay.billing.proto.v1.RateUsageResponse;
import com.lordkay.billing.proto.v1.RatingServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

@Service
public class RatingGrpcService extends RatingServiceGrpc.RatingServiceImplBase {

	@Override
	public void rateUsage(RateUsageRequest request, StreamObserver<RateUsageResponse> responseObserver) {
		long unitPriceMinor = switch (request.getMeterId()) {
			case "api-calls" -> 3L;
			case "storage-gb" -> 12L;
			default -> 5L;
		};
		long totalAmountMinor = request.getQuantity() * unitPriceMinor;

		RateUsageResponse response = RateUsageResponse.newBuilder()
			.setUsageEventId(request.getUsageEventId())
			.setTenantId(request.getTenantId())
			.setQuantity(request.getQuantity())
			.setUnitPriceMinor(unitPriceMinor)
			.setTotalAmountMinor(totalAmountMinor)
			.setCurrencyCode("USD")
			.setStatus("RATED")
			.build();

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}
}
