package com.lordkay.billing.usageingestion.service;

import com.lordkay.billing.usageingestion.domain.UsageEvent;
import com.lordkay.billing.usageingestion.domain.UsageEventRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "billing.async-rating.enabled", havingValue = "true")
public class UsageRatingWorker {

	private final UsageEventRepository usageEventRepository;
	private final UsageBillingProcessor usageBillingProcessor;

	public UsageRatingWorker(UsageEventRepository usageEventRepository, UsageBillingProcessor usageBillingProcessor) {
		this.usageEventRepository = usageEventRepository;
		this.usageBillingProcessor = usageBillingProcessor;
	}

	@Scheduled(fixedDelayString = "${billing.async-rating.poll-delay-ms:2000}")
	public void pollPendingEvents() {
		for (UsageEvent event : usageEventRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING")) {
			usageBillingProcessor.processPendingEvent(event.getUsageEventId());
		}
	}
}
