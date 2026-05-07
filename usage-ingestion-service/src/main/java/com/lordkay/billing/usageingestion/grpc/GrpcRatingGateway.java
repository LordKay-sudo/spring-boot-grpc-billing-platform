package com.lordkay.billing.usageingestion.grpc;

import com.lordkay.billing.proto.v1.RateUsageRequest;
import com.lordkay.billing.proto.v1.RateUsageResponse;
import com.lordkay.billing.proto.v1.RatingServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class GrpcRatingGateway implements RatingGateway {

	private final ManagedChannel channel;
	private final RatingServiceGrpc.RatingServiceBlockingStub ratingStub;

	public GrpcRatingGateway(
		@Value("${billing.rating.host:localhost}") String host,
		@Value("${billing.rating.port:9091}") int port
	) {
		this.channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
		this.ratingStub = RatingServiceGrpc.newBlockingStub(this.channel);
	}

	@Override
	public RateUsageResponse rateUsage(String usageEventId, String tenantId, String meterId, long quantity) {
		return ratingStub.rateUsage(RateUsageRequest.newBuilder()
			.setUsageEventId(usageEventId)
			.setTenantId(tenantId)
			.setMeterId(meterId)
			.setQuantity(quantity)
			.build());
	}

	@PreDestroy
	void shutdown() throws InterruptedException {
		channel.shutdownNow().awaitTermination(3, TimeUnit.SECONDS);
	}
}
