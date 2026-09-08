package com.lordkay.billing.usageingestion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.lordkay.billing.proto.v1.CreateInvoiceResponse;
import com.lordkay.billing.proto.v1.RateUsageResponse;
import com.lordkay.billing.proto.v1.UsageEventRequest;
import com.lordkay.billing.proto.v1.UsageEventResponse;
import com.lordkay.billing.usageingestion.domain.UsageEventRepository;
import com.lordkay.billing.usageingestion.grpc.InvoicingGateway;
import com.lordkay.billing.usageingestion.grpc.RatingGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(UsageIngestionOrchestrator.class)
class UsageIngestionPersistenceTest {

	@Autowired
	private UsageEventRepository usageEventRepository;

	@Autowired
	private UsageIngestionOrchestrator orchestrator;

	@MockitoBean
	private RatingGateway ratingGateway;

	@MockitoBean
	private InvoicingGateway invoicingGateway;

	@Test
	void duplicateIdempotencyKeyReturnsSameUsageEventId() {
		when(ratingGateway.rateUsage(anyString(), anyString(), anyString(), anyLong())).thenAnswer(invocation -> RateUsageResponse.newBuilder()
			.setUsageEventId(invocation.getArgument(0))
			.setTenantId(invocation.getArgument(1))
			.setQuantity(invocation.getArgument(3))
			.setUnitPriceMinor(3)
			.setTotalAmountMinor(126)
			.setCurrencyCode("USD")
			.setStatus("RATED")
			.build());
		when(invoicingGateway.createInvoice(anyString(), anyString(), anyLong(), anyString())).thenReturn(CreateInvoiceResponse.newBuilder()
			.setInvoiceId("inv-123")
			.setTenantId("tenant-1")
			.setTotalMinor(126)
			.setCurrencyCode("USD")
			.setStatus("DRAFT")
			.build());

		UsageEventRequest request = UsageEventRequest.newBuilder()
			.setTenantId("tenant-1")
			.setMeterId("api-calls")
			.setIdempotencyKey("persist-key-001")
			.setQuantity(42)
			.setOccurredAtEpochMs(1715068800000L)
			.build();

		UsageEventResponse first = orchestrator.ingest(request);
		UsageEventResponse second = orchestrator.ingest(request);

		assertThat(second.getUsageEventId()).isEqualTo(first.getUsageEventId());
		assertThat(second.getMessage()).contains("Duplicate idempotency key");
		assertThat(usageEventRepository.count()).isEqualTo(1);
	}
}
