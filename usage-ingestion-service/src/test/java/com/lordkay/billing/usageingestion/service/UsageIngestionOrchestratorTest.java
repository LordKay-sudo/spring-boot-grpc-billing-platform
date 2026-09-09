package com.lordkay.billing.usageingestion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lordkay.billing.proto.v1.UsageEventRequest;
import com.lordkay.billing.proto.v1.UsageEventResponse;
import com.lordkay.billing.usageingestion.domain.UsageEvent;
import com.lordkay.billing.usageingestion.domain.UsageEventRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UsageIngestionOrchestratorTest {

	@Mock
	private UsageEventRepository usageEventRepository;

	@Mock
	private UsageBillingProcessor usageBillingProcessor;

	@Test
	void ingestReturnsStoredResultForDuplicateIdempotencyKey() {
		UsageIngestionOrchestrator orchestrator = new UsageIngestionOrchestrator(usageEventRepository, usageBillingProcessor, true);
		UsageEvent existing = UsageEvent.pending("tenant-1", "api-calls", "key-123", 42, 1715068800000L);
		existing.markAccepted(126, "inv-123");
		when(usageEventRepository.findByTenantIdAndIdempotencyKey("tenant-1", "key-123")).thenReturn(Optional.of(existing));

		UsageEventResponse response = orchestrator.ingest(validRequest());

		assertThat(response.getStatus()).isEqualTo("ACCEPTED");
		assertThat(response.getMessage()).contains("Duplicate idempotency key");
		verify(usageBillingProcessor, never()).processPendingEvent(any());
	}

	@Test
	void ingestQueuesUsageWhenAsyncRatingEnabled() {
		UsageIngestionOrchestrator orchestrator = new UsageIngestionOrchestrator(usageEventRepository, usageBillingProcessor, true);
		when(usageEventRepository.findByTenantIdAndIdempotencyKey("tenant-1", "key-123")).thenReturn(Optional.empty());
		when(usageEventRepository.saveAndFlush(any(UsageEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

		UsageEventResponse response = orchestrator.ingest(validRequest());

		assertThat(response.getStatus()).isEqualTo("QUEUED");
		assertThat(response.getRatedAmountMinor()).isZero();
		assertThat(response.getInvoiceId()).isEmpty();
		verify(usageBillingProcessor, never()).processPendingEvent(any());
	}

	@Test
	void ingestProcessesSynchronouslyWhenAsyncRatingDisabled() {
		UsageIngestionOrchestrator orchestrator = new UsageIngestionOrchestrator(usageEventRepository, usageBillingProcessor, false);
		UsageEvent event = UsageEvent.pending("tenant-1", "api-calls", "key-123", 42, 1715068800000L);
		event.markAccepted(126, "inv-123");
		when(usageEventRepository.findByTenantIdAndIdempotencyKey("tenant-1", "key-123")).thenReturn(Optional.empty());
		when(usageEventRepository.saveAndFlush(any(UsageEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(usageBillingProcessor.processPendingEvent(any())).thenReturn(true);
		when(usageEventRepository.findById(any())).thenReturn(Optional.of(event));

		UsageEventResponse response = orchestrator.ingest(validRequest());

		assertThat(response.getStatus()).isEqualTo("ACCEPTED");
		assertThat(response.getRatedAmountMinor()).isEqualTo(126);
		verify(usageBillingProcessor).processPendingEvent(any());
	}

	private UsageEventRequest validRequest() {
		return UsageEventRequest.newBuilder()
			.setTenantId("tenant-1")
			.setMeterId("api-calls")
			.setIdempotencyKey("key-123")
			.setQuantity(42)
			.setOccurredAtEpochMs(1715068800000L)
			.build();
	}
}
