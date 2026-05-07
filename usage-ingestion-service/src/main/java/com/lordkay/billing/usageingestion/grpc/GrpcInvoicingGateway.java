package com.lordkay.billing.usageingestion.grpc;

import com.lordkay.billing.proto.v1.CreateInvoiceRequest;
import com.lordkay.billing.proto.v1.CreateInvoiceResponse;
import com.lordkay.billing.proto.v1.InvoicingServiceGrpc;
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
public class GrpcInvoicingGateway implements InvoicingGateway {

	private final ManagedChannel channel;
	private final InvoicingServiceGrpc.InvoicingServiceBlockingStub invoicingStub;

	public GrpcInvoicingGateway(
		@Value("${billing.invoicing.host:localhost}") String host,
		@Value("${billing.invoicing.port:9092}") int port,
		@Value("${billing.invoicing.tls.enabled:false}") boolean tlsEnabled,
		@Value("${billing.invoicing.tls.trust-cert:}") String trustCertPath
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
					throw new IllegalStateException("Unable to configure TLS trust cert for invoicing client", ex);
				}
			}
		}
		else {
			channelBuilder.usePlaintext();
		}
		this.channel = channelBuilder.build();
		this.invoicingStub = InvoicingServiceGrpc.newBlockingStub(this.channel);
	}

	@Override
	public CreateInvoiceResponse createInvoice(String tenantId, String usageEventId, long amountMinor, String currencyCode) {
		return invoicingStub.createInvoice(CreateInvoiceRequest.newBuilder()
			.setTenantId(tenantId)
			.setUsageEventId(usageEventId)
			.setAmountMinor(amountMinor)
			.setCurrencyCode(currencyCode)
			.build());
	}

	@PreDestroy
	void shutdown() throws InterruptedException {
		channel.shutdownNow().awaitTermination(3, TimeUnit.SECONDS);
	}
}
