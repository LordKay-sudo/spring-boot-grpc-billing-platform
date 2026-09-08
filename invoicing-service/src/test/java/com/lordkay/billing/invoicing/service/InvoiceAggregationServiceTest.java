package com.lordkay.billing.invoicing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.lordkay.billing.invoicing.domain.InvoiceLineItemRepository;
import com.lordkay.billing.invoicing.domain.InvoiceRepository;
import com.lordkay.billing.proto.v1.CreateInvoiceRequest;
import com.lordkay.billing.proto.v1.CreateInvoiceResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class InvoiceAggregationServiceTest {

	@Autowired
	private InvoiceAggregationService invoiceAggregationService;

	@Autowired
	private InvoiceRepository invoiceRepository;

	@Autowired
	private InvoiceLineItemRepository invoiceLineItemRepository;

	@Test
	void aggregatesMultipleUsageEventsIntoSamePeriodInvoice() {
		CreateInvoiceRequest first = CreateInvoiceRequest.newBuilder()
			.setTenantId("tenant-1")
			.setUsageEventId("evt-1")
			.setAmountMinor(120)
			.setCurrencyCode("USD")
			.setBillingPeriodKey("2024-06")
			.build();
		CreateInvoiceRequest second = CreateInvoiceRequest.newBuilder()
			.setTenantId("tenant-1")
			.setUsageEventId("evt-2")
			.setAmountMinor(80)
			.setCurrencyCode("USD")
			.setBillingPeriodKey("2024-06")
			.build();

		CreateInvoiceResponse firstResponse = invoiceAggregationService.applyRatedUsage(first);
		CreateInvoiceResponse secondResponse = invoiceAggregationService.applyRatedUsage(second);

		assertThat(secondResponse.getInvoiceId()).isEqualTo(firstResponse.getInvoiceId());
		assertThat(secondResponse.getTotalMinor()).isEqualTo(200);
		assertThat(invoiceRepository.count()).isEqualTo(1);
		assertThat(invoiceLineItemRepository.count()).isEqualTo(2);
	}

	@Test
	void duplicateUsageEventDoesNotDoubleCount() {
		CreateInvoiceRequest request = CreateInvoiceRequest.newBuilder()
			.setTenantId("tenant-1")
			.setUsageEventId("evt-dup")
			.setAmountMinor(120)
			.setCurrencyCode("USD")
			.setBillingPeriodKey("2024-07")
			.build();

		CreateInvoiceResponse first = invoiceAggregationService.applyRatedUsage(request);
		CreateInvoiceResponse second = invoiceAggregationService.applyRatedUsage(request);

		assertThat(second.getInvoiceId()).isEqualTo(first.getInvoiceId());
		assertThat(second.getTotalMinor()).isEqualTo(120);
		assertThat(invoiceLineItemRepository.count()).isEqualTo(1);
	}
}
