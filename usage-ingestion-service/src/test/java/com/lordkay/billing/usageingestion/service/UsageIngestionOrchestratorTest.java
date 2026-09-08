package com.lordkay.billing.usageingestion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lordkay.billing.proto.v1.CreateInvoiceResponse;
import com.lordkay.billing.proto.v1.RateUsageResponse;
import com.lordkay.billing.proto.v1.UsageEventRequest;
import com.lordkay.billing.proto.v1.UsageEventResponse;
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
class UsageIngestionOrchestratorTest {

	@Mock
	private UsageEventRepository usageEventRepository;

	@Mock
	private RatingGateway ratingGateway;

	@Mock
	private InvoicingGateway invoicingGateway;

	private UsageIngestionOrchestrator orchestrator;

	@BeforeEach
	void setUp() {
		orchestrator = new UsageIngestionOrchestrator(usageEventRepository, ratingGateway, invoicingGateway, false, false);
	}

	@Test
	void ingestReturnsStoredResultForDuplicateIdempotencyKey() {
		UsageEvent existing = UsageEvent.pending("tenant-1", "api-calls", "key-123", 42, 1715068800000L);
		existing.markAccepted(126, "inv-123");
		when(usageEventRepository.findByTenantIdAndIdempotencyKey("tenant-1", "key-123")).thenReturn(Optional.of(existing));

		UsageEventResponse response = orchestrator.ingest(validRequest());

		assertThat(response.getUsageEventId()).isEqualTo(existing.getUsageEventId());
		assertThat(response.getStatus()).isEqualTo("ACCEPTED");
		assertThat(response.getMessage()).contains("Duplicate idempotency key");
		verify(ratingGateway, never()).rateUsage(anyString(), anyString(), anyString(), anyLong());
		verify(invoicingGateway, never()).createInvoice(anyString(), anyString(), anyLong(), anyString(), anyString());
	}

	@Test
	void ingestPersistsAndRatesUsageOnHappyPath() {
		when(usageEventRepository.findByTenantIdAndIdempotencyKey("tenant-1", "key-123")).thenReturn(Optional.empty());
		when(usageEventRepository.saveAndFlush(any(UsageEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(usageEventRepository.save(any(UsageEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(ratingGateway.rateUsage(anyString(), anyString(), anyString(), anyLong())).thenAnswer(invocation -> RateUsageResponse.newBuilder()
			.setUsageEventId(invocation.getArgument(0))
			.setTenantId(invocation.getArgument(1))
			.setQuantity(invocation.getArgument(3))
			.setUnitPriceMinor(3)
			.setTotalAmountMinor(126)
			.setCurrencyCode("USD")
			.setStatus("RATED")
			.build());
		when(invoicingGateway.createInvoice(eq("tenant-1"), anyString(), eq(126L), eq("USD"), eq("2024-05"))).thenReturn(CreateInvoiceResponse.newBuilder()
			.setInvoiceId("inv-123")
			.setTenantId("tenant-1")
			.setTotalMinor(126)
			.setCurrencyCode("USD")
			.setStatus("DRAFT")
			.setBillingPeriodKey("2024-05")
			.build());

		UsageEventResponse response = orchestrator.ingest(validRequest());

		assertThat(response.getStatus()).isEqualTo("ACCEPTED");
		assertThat(response.getRatedAmountMinor()).isEqualTo(126);
		assertThat(response.getInvoiceId()).isEqualTo("inv-123");

		verify(usageEventRepository).saveAndFlush(any(UsageEvent.class));
		ArgumentCaptor<UsageEvent> savedEvent = ArgumentCaptor.forClass(UsageEvent.class);
		verify(usageEventRepository).save(savedEvent.capture());
		assertThat(savedEvent.getValue().getStatus()).isEqualTo("ACCEPTED");
	}

	@Test
	void ingestDegradesWhenRatingFails() {
		when(usageEventRepository.findByTenantIdAndIdempotencyKey("tenant-1", "key-123")).thenReturn(Optional.empty());
		when(usageEventRepository.saveAndFlush(any(UsageEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(usageEventRepository.save(any(UsageEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(ratingGateway.rateUsage(anyString(), anyString(), anyString(), anyLong()))
			.thenThrow(new RuntimeException("rating-down"));

		UsageEventResponse response = orchestrator.ingest(validRequest());

		assertThat(response.getStatus()).isEqualTo("ACCEPTED_WITH_DEGRADATION");
		assertThat(response.getRatedAmountMinor()).isZero();
		assertThat(response.getInvoiceId()).isEmpty();
		verify(invoicingGateway, never()).createInvoice(anyString(), anyString(), anyLong(), anyString(), anyString());
	}

	@Test
	void ingestDegradesWhenInvoicingFails() {
		when(usageEventRepository.findByTenantIdAndIdempotencyKey("tenant-1", "key-123")).thenReturn(Optional.empty());
		when(usageEventRepository.saveAndFlush(any(UsageEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(usageEventRepository.save(any(UsageEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(ratingGateway.rateUsage(anyString(), anyString(), anyString(), anyLong())).thenReturn(RateUsageResponse.newBuilder()
			.setUsageEventId("evt-1")
			.setTenantId("tenant-1")
			.setQuantity(42)
			.setUnitPriceMinor(3)
			.setTotalAmountMinor(126)
			.setCurrencyCode("USD")
			.setStatus("RATED")
			.build());
		when(invoicingGateway.createInvoice(anyString(), anyString(), anyLong(), anyString(), anyString()))
			.thenThrow(new RuntimeException("invoicing-down"));

		UsageEventResponse response = orchestrator.ingest(validRequest());

		assertThat(response.getStatus()).isEqualTo("ACCEPTED_WITH_DEGRADATION");
		assertThat(response.getRatedAmountMinor()).isEqualTo(126);
		assertThat(response.getInvoiceId()).isEmpty();
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
