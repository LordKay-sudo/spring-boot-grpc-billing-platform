package com.lordkay.billing.usageingestion.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lordkay.billing.usageingestion.domain.UsageEvent;
import com.lordkay.billing.usageingestion.domain.UsageEventRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UsageRatingWorkerTest {

	@Mock
	private UsageEventRepository usageEventRepository;

	@Mock
	private UsageBillingProcessor usageBillingProcessor;

	@InjectMocks
	private UsageRatingWorker worker;

	@Test
	void pollPendingEventsProcessesEachPendingUsageEvent() {
		UsageEvent first = UsageEvent.pending("tenant-1", "api-calls", "key-1", 10, 1715068800000L);
		UsageEvent second = UsageEvent.pending("tenant-1", "api-calls", "key-2", 20, 1715068800000L);
		when(usageEventRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(first, second));

		worker.pollPendingEvents();

		verify(usageBillingProcessor).processPendingEvent(first.getUsageEventId());
		verify(usageBillingProcessor).processPendingEvent(second.getUsageEventId());
	}
}
