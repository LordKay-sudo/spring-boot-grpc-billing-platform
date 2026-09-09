package com.lordkay.billing.usageingestion.service;

import com.lordkay.billing.proto.v1.UsageEventRequest;
import com.lordkay.billing.proto.v1.UsageEventResponse;
import com.lordkay.billing.usageingestion.domain.UsageEvent;
import com.lordkay.billing.usageingestion.domain.UsageEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsageIngestionOrchestrator {

	private final UsageEventRepository usageEventRepository;
	private final UsageBillingProcessor usageBillingProcessor;
	private final boolean asyncRatingEnabled;

	public UsageIngestionOrchestrator(
		UsageEventRepository usageEventRepository,
		UsageBillingProcessor usageBillingProcessor,
		@Value("${billing.async-rating.enabled:true}") boolean asyncRatingEnabled
	) {
		this.usageEventRepository = usageEventRepository;
		this.usageBillingProcessor = usageBillingProcessor;
		this.asyncRatingEnabled = asyncRatingEnabled;
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

		if (asyncRatingEnabled) {
			return toQueuedResponse(event);
		}

		usageBillingProcessor.processPendingEvent(event.getUsageEventId());
		return usageEventRepository.findById(event.getUsageEventId())
			.map(this::toResponse)
			.orElseThrow();
	}

	private UsageEventResponse toDuplicateResponse(UsageEvent event) {
		return UsageEventResponse.newBuilder()
			.setUsageEventId(event.getUsageEventId())
			.setStatus(toApiStatus(event.getStatus()))
			.setMessage("Duplicate idempotency key; returning stored result")
			.setRatedAmountMinor(event.getRatedAmountMinor())
			.setInvoiceId(event.getInvoiceId() == null ? "" : event.getInvoiceId())
			.build();
	}

	private UsageEventResponse toQueuedResponse(UsageEvent event) {
		return UsageEventResponse.newBuilder()
			.setUsageEventId(event.getUsageEventId())
			.setStatus("QUEUED")
			.setMessage("Usage event queued for async rating")
			.setRatedAmountMinor(0)
			.setInvoiceId("")
			.build();
	}

	private UsageEventResponse toResponse(UsageEvent event) {
		return UsageEventResponse.newBuilder()
			.setUsageEventId(event.getUsageEventId())
			.setStatus(toApiStatus(event.getStatus()))
			.setMessage(event.getMessage())
			.setRatedAmountMinor(event.getRatedAmountMinor())
			.setInvoiceId(event.getInvoiceId() == null ? "" : event.getInvoiceId())
			.build();
	}

	static String toApiStatus(String internalStatus) {
		return "PENDING".equals(internalStatus) ? "QUEUED" : internalStatus;
	}
}
