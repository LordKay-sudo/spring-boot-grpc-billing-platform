package com.lordkay.billing.usageingestion.grpc;

import com.lordkay.billing.proto.v1.RateUsageRequest;
import com.lordkay.billing.proto.v1.RateUsageResponse;
import com.lordkay.billing.proto.v1.RatingServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import jakarta.annotation.PreDestroy;
import java.nio.file.Files;
import java.nio.file.Path;
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
		@Value("${billing.rating.port:9091}") int port,
		@Value("${billing.rating.tls.enabled:false}") boolean tlsEnabled,
		@Value("${billing.rating.tls.trust-cert:}") String trustCertPath
	) {
		NettyChannelBuilder channelBuilder = NettyChannelBuilder.forAddress(host, port);
		if (tlsEnabled) {
			channelBuilder.useTransportSecurity();
			if (!trustCertPath.isBlank() && Files.exists(Path.of(trustCertPath))) {
				try {
					channelBuilder.sslContext(
						GrpcSslContexts.forClient().trustManager(Path.of(trustCertPath).toFile()).build()
					);
				}
				catch (Exception ex) {
					throw new IllegalStateException("Unable to configure TLS trust cert for rating client", ex);
				}
			}
		}
		else {
			channelBuilder.usePlaintext();
		}
		this.channel = channelBuilder.build();
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
