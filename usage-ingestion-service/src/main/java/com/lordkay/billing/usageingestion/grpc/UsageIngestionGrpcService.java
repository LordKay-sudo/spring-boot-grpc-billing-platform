package com.lordkay.billing.usageingestion.grpc;

import com.lordkay.billing.proto.v1.CreateInvoiceResponse;
import com.lordkay.billing.proto.v1.RateUsageResponse;
import com.lordkay.billing.proto.v1.UsageEventRequest;
import com.lordkay.billing.proto.v1.UsageEventResponse;
import com.lordkay.billing.proto.v1.UsageIngestionServiceGrpc;
import io.grpc.stub.StreamObserver;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class UsageIngestionGrpcService extends UsageIngestionServiceGrpc.UsageIngestionServiceImplBase {

	private final RatingGateway ratingGateway;
	private final InvoicingGateway invoicingGateway;
	private final boolean failOnRatingError;
	private final boolean failOnInvoicingError;

	public UsageIngestionGrpcService(
		RatingGateway ratingGateway,
		InvoicingGateway invoicingGateway,
		@Value("${billing.fail-on-rating-error:false}") boolean failOnRatingError,
		@Value("${billing.fail-on-invoicing-error:false}") boolean failOnInvoicingError
	) {
		this.ratingGateway = ratingGateway;
		this.invoicingGateway = invoicingGateway;
		this.failOnRatingError = failOnRatingError;
		this.failOnInvoicingError = failOnInvoicingError;
	}

	@Override
	public void ingestUsage(UsageEventRequest request, StreamObserver<UsageEventResponse> responseObserver) {
		String usageEventId = UUID.randomUUID().toString();
		UsageEventResponse response;
		try {
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

			response = UsageEventResponse.newBuilder()
				.setUsageEventId(usageEventId)
				.setStatus("ACCEPTED")
				.setMessage("Usage event accepted for tenant " + request.getTenantId())
				.setRatedAmountMinor(ratedUsage.getTotalAmountMinor())
				.setInvoiceId(invoice.getInvoiceId())
				.build();
		}
		catch (RuntimeException ex) {
			if (failOnRatingError || failOnInvoicingError) {
				throw ex;
			}
			// Graceful degradation keeps ingestion available when downstream billing dependencies fail.
			response = UsageEventResponse.newBuilder()
				.setUsageEventId(usageEventId)
				.setStatus("ACCEPTED_WITH_DEGRADATION")
				.setMessage("Usage accepted but downstream billing unavailable: " + ex.getClass().getSimpleName())
				.setRatedAmountMinor(0)
				.setInvoiceId("")
				.build();
		}

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}
}
