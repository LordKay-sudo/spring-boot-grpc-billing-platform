package com.lordkay.billing.usageingestion.service;

import com.lordkay.billing.proto.v1.CreateInvoiceResponse;
import com.lordkay.billing.proto.v1.RateUsageResponse;
import com.lordkay.billing.usageingestion.domain.UsageEvent;
import com.lordkay.billing.usageingestion.domain.UsageEventRepository;
import com.lordkay.billing.usageingestion.grpc.InvoicingGateway;
import com.lordkay.billing.usageingestion.grpc.RatingGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsageBillingProcessor {

	private final UsageEventRepository usageEventRepository;
	private final RatingGateway ratingGateway;
	private final InvoicingGateway invoicingGateway;
	private final boolean failOnRatingError;
	private final boolean failOnInvoicingError;

	public UsageBillingProcessor(
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
	public boolean processPendingEvent(String usageEventId) {
		UsageEvent event = usageEventRepository.findById(usageEventId).orElse(null);
		if (event == null || !"PENDING".equals(event.getStatus())) {
			return false;
		}

		RateUsageResponse ratedUsage;
		try {
			ratedUsage = ratingGateway.rateUsage(
				event.getUsageEventId(),
				event.getTenantId(),
				event.getMeterId(),
				event.getQuantity()
			);
		}
		catch (RuntimeException ex) {
			if (failOnRatingError) {
				throw ex;
			}
			event.markDegraded(ex.getClass().getSimpleName());
			usageEventRepository.save(event);
			return true;
		}

		try {
			String billingPeriodKey = BillingPeriodKey.fromEpochMs(event.getOccurredAtEpochMs());
			CreateInvoiceResponse invoice = invoicingGateway.createInvoice(
				event.getTenantId(),
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
		return true;
	}
}
