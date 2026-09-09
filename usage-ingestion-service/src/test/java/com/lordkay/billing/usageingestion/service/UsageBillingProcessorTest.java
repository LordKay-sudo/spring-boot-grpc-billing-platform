package com.lordkay.billing.usageingestion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lordkay.billing.proto.v1.CreateInvoiceResponse;
import com.lordkay.billing.proto.v1.RateUsageResponse;
import com.lordkay.billing.usageingestion.domain.UsageEvent;
import com.lordkay.billing.usageingestion.domain.UsageEventRepository;
import com.lordkay.billing.usageingestion.grpc.InvoicingGateway;
import com.lordkay.billing.usageingestion.grpc.RatingGateway;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UsageBillingProcessorTest {

	@Mock
	private UsageEventRepository usageEventRepository;

	@Mock
	private RatingGateway ratingGateway;

	@Mock
	private InvoicingGateway invoicingGateway;

	private UsageBillingProcessor processor;

	@BeforeEach
	void setUp() {
		processor = new UsageBillingProcessor(usageEventRepository, ratingGateway, invoicingGateway, false, false);
	}

	@Test
	void processPendingEventRatesAndInvoicesUsage() {
		UsageEvent event = UsageEvent.pending("tenant-1", "api-calls", "key-123", 42, 1715068800000L);
		when(usageEventRepository.findById(event.getUsageEventId())).thenReturn(Optional.of(event));
		when(usageEventRepository.save(any(UsageEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(ratingGateway.rateUsage(anyString(), anyString(), anyString(), anyLong())).thenReturn(RateUsageResponse.newBuilder()
			.setUsageEventId(event.getUsageEventId())
			.setTenantId("tenant-1")
			.setQuantity(42)
			.setUnitPriceMinor(3)
			.setTotalAmountMinor(126)
			.setCurrencyCode("USD")
			.setStatus("RATED")
			.build());
		when(invoicingGateway.createInvoice(eq("tenant-1"), anyString(), eq(126L), eq("USD"), eq("2024-05")))
			.thenReturn(CreateInvoiceResponse.newBuilder()
				.setInvoiceId("inv-123")
				.setTenantId("tenant-1")
				.setTotalMinor(126)
				.setCurrencyCode("USD")
				.setStatus("DRAFT")
				.setBillingPeriodKey("2024-05")
				.build());

		assertThat(processor.processPendingEvent(event.getUsageEventId())).isTrue();

		ArgumentCaptor<UsageEvent> savedEvent = ArgumentCaptor.forClass(UsageEvent.class);
		verify(usageEventRepository).save(savedEvent.capture());
		assertThat(savedEvent.getValue().getStatus()).isEqualTo("ACCEPTED");
		assertThat(savedEvent.getValue().getRatedAmountMinor()).isEqualTo(126);
	}

	@Test
	void processPendingEventDegradesWhenRatingFails() {
		UsageEvent event = UsageEvent.pending("tenant-1", "api-calls", "key-123", 42, 1715068800000L);
		when(usageEventRepository.findById(event.getUsageEventId())).thenReturn(Optional.of(event));
		when(usageEventRepository.save(any(UsageEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(ratingGateway.rateUsage(anyString(), anyString(), anyString(), anyLong()))
			.thenThrow(new RuntimeException("rating-down"));

		assertThat(processor.processPendingEvent(event.getUsageEventId())).isTrue();

		ArgumentCaptor<UsageEvent> savedEvent = ArgumentCaptor.forClass(UsageEvent.class);
		verify(usageEventRepository).save(savedEvent.capture());
		assertThat(savedEvent.getValue().getStatus()).isEqualTo("ACCEPTED_WITH_DEGRADATION");
		verify(invoicingGateway, never()).createInvoice(anyString(), anyString(), anyLong(), anyString(), anyString());
	}

	@Test
	void processPendingEventSkipsNonPendingEvents() {
		UsageEvent event = UsageEvent.pending("tenant-1", "api-calls", "key-123", 42, 1715068800000L);
		event.markAccepted(126, "inv-123");
		when(usageEventRepository.findById(event.getUsageEventId())).thenReturn(Optional.of(event));

		assertThat(processor.processPendingEvent(event.getUsageEventId())).isFalse();

		verify(ratingGateway, never()).rateUsage(anyString(), anyString(), anyString(), anyLong());
	}
}
