package com.lordkay.billing.usageingestion.grpc;

import com.lordkay.billing.proto.v1.CreateInvoiceRequest;
import com.lordkay.billing.proto.v1.CreateInvoiceResponse;
import com.lordkay.billing.proto.v1.InvoicingServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;
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
		@Value("${billing.invoicing.port:9092}") int port
	) {
		this.channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
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
