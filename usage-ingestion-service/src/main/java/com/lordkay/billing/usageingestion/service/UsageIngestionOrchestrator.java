package com.lordkay.billing.usageingestion.service;

import com.lordkay.billing.proto.v1.CreateInvoiceResponse;
import com.lordkay.billing.proto.v1.RateUsageResponse;
import com.lordkay.billing.proto.v1.UsageEventRequest;
import com.lordkay.billing.proto.v1.UsageEventResponse;
import com.lordkay.billing.usageingestion.domain.UsageEvent;
import com.lordkay.billing.usageingestion.domain.UsageEventRepository;
import com.lordkay.billing.usageingestion.grpc.InvoicingGateway;
import com.lordkay.billing.usageingestion.grpc.RatingGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsageIngestionOrchestrator {

	private final UsageEventRepository usageEventRepository;
	private final RatingGateway ratingGateway;
	private final InvoicingGateway invoicingGateway;
	private final boolean failOnRatingError;
	private final boolean failOnInvoicingError;

	public UsageIngestionOrchestrator(
		UsageEventRepository usageEventRepository,
		RatingGateway ratingGateway,
		InvoicingGateway invoicingGateway,
		@Value("${billing.fail-on-rating-error:false}") boolean failOnRatingError,
		@Value("${billing.fail-on-invoicing-error:false}") boolean failOnInvoicingError
	) {
		this.usageEventRepository = usageEventRepository;
		this.ratingGateway = ratingGateway;
		this.invoicingGateway = invoicingGateway;
		this.failOnRatingError = failOnRatingError;
		this.failOnInvoicingError = failOnInvoicingError;
	}

	@Transactional
	public UsageEventResponse ingest(UsageEventRequest request) {
		return usageEventRepository.findByTenantIdAndIdempotencyKey(request.getTenantId(), request.getIdempotencyKey())
			.map(this::toDuplicateResponse)
			.orElseGet(() -> processNewEvent(request));
	}

	private UsageEventResponse processNewEvent(UsageEventRequest request) {
		UsageEvent event = UsageEvent.pending(
			request.getTenantId(),
			request.getMeterId(),
			request.getIdempotencyKey(),
			request.getQuantity(),
			request.getOccurredAtEpochMs()
		);

		try {
			usageEventRepository.saveAndFlush(event);
		}
		catch (DataIntegrityViolationException ex) {
			return usageEventRepository
				.findByTenantIdAndIdempotencyKey(request.getTenantId(), request.getIdempotencyKey())
				.map(this::toDuplicateResponse)
				.orElseThrow(() -> ex);
		}

		RateUsageResponse ratedUsage = null;
		try {
			ratedUsage = ratingGateway.rateUsage(
				event.getUsageEventId(),
				request.getTenantId(),
				request.getMeterId(),
				request.getQuantity()
			);
		}
		catch (RuntimeException ex) {
			if (failOnRatingError) {
				throw ex;
			}
			event.markDegraded(ex.getClass().getSimpleName());
			usageEventRepository.save(event);
			return toResponse(event);
		}

		try {
			String billingPeriodKey = BillingPeriodKey.fromEpochMs(request.getOccurredAtEpochMs());
			CreateInvoiceResponse invoice = invoicingGateway.createInvoice(
				request.getTenantId(),
				event.getUsageEventId(),
				ratedUsage.getTotalAmountMinor(),
				ratedUsage.getCurrencyCode(),
				billingPeriodKey
			);
			event.markAccepted(ratedUsage.getTotalAmountMinor(), invoice.getInvoiceId());
		}
		catch (RuntimeException ex) {
			if (failOnInvoicingError) {
				throw ex;
			}
			event.markInvoicingDegraded(ratedUsage.getTotalAmountMinor(), ex.getClass().getSimpleName());
		}

		usageEventRepository.save(event);
		return toResponse(event);
	}

	private UsageEventResponse toDuplicateResponse(UsageEvent event) {
		return UsageEventResponse.newBuilder()
			.setUsageEventId(event.getUsageEventId())
			.setStatus(event.getStatus())
			.setMessage("Duplicate idempotency key; returning stored result")
			.setRatedAmountMinor(event.getRatedAmountMinor())
			.setInvoiceId(event.getInvoiceId() == null ? "" : event.getInvoiceId())
			.build();
	}

	private UsageEventResponse toResponse(UsageEvent event) {
		return UsageEventResponse.newBuilder()
			.setUsageEventId(event.getUsageEventId())
			.setStatus(event.getStatus())
			.setMessage(event.getMessage())
			.setRatedAmountMinor(event.getRatedAmountMinor())
			.setInvoiceId(event.getInvoiceId() == null ? "" : event.getInvoiceId())
			.build();
	}
}
