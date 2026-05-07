package com.lordkay.billing.usageingestion.grpc;

import com.lordkay.billing.proto.v1.CreateInvoiceResponse;
import com.lordkay.billing.proto.v1.RateUsageResponse;
import com.lordkay.billing.proto.v1.UsageEventRequest;
import com.lordkay.billing.proto.v1.UsageEventResponse;
import com.lordkay.billing.proto.v1.UsageIngestionServiceGrpc;
import io.grpc.stub.StreamObserver;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class UsageIngestionGrpcService extends UsageIngestionServiceGrpc.UsageIngestionServiceImplBase {

	private final RatingGateway ratingGateway;
	private final InvoicingGateway invoicingGateway;

	public UsageIngestionGrpcService(RatingGateway ratingGateway, InvoicingGateway invoicingGateway) {
		this.ratingGateway = ratingGateway;
		this.invoicingGateway = invoicingGateway;
	}

	@Override
	public void ingestUsage(UsageEventRequest request, StreamObserver<UsageEventResponse> responseObserver) {
		String usageEventId = UUID.randomUUID().toString();
		RateUsageResponse ratedUsage = ratingGateway.rateUsage(
			usageEventId,
			request.getTenantId(),
			request.getMeterId(),
			request.getQuantity()
		);
		CreateInvoiceResponse invoice = invoicingGateway.createInvoice(
			request.getTenantId(),
			usageEventId,
			ratedUsage.getTotalAmountMinor(),
			ratedUsage.getCurrencyCode()
		);

		UsageEventResponse response = UsageEventResponse.newBuilder()
			.setUsageEventId(usageEventId)
			.setStatus("ACCEPTED")
			.setMessage("Usage event accepted for tenant " + request.getTenantId())
			.setRatedAmountMinor(ratedUsage.getTotalAmountMinor())
			.setInvoiceId(invoice.getInvoiceId())
			.build();

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}
}
