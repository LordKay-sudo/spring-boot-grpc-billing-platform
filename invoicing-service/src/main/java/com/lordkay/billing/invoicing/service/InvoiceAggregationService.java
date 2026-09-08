package com.lordkay.billing.invoicing.service;

import com.lordkay.billing.invoicing.domain.Invoice;
import com.lordkay.billing.invoicing.domain.InvoiceLineItem;
import com.lordkay.billing.invoicing.domain.InvoiceLineItemRepository;
import com.lordkay.billing.invoicing.domain.InvoiceRepository;
import com.lordkay.billing.proto.v1.CreateInvoiceRequest;
import com.lordkay.billing.proto.v1.CreateInvoiceResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceAggregationService {

	private final InvoiceRepository invoiceRepository;
	private final InvoiceLineItemRepository invoiceLineItemRepository;

	public InvoiceAggregationService(
		InvoiceRepository invoiceRepository,
		InvoiceLineItemRepository invoiceLineItemRepository
	) {
		this.invoiceRepository = invoiceRepository;
		this.invoiceLineItemRepository = invoiceLineItemRepository;
	}

	@Transactional
	public CreateInvoiceResponse applyRatedUsage(CreateInvoiceRequest request) {
		return invoiceLineItemRepository.findByUsageEventId(request.getUsageEventId())
			.flatMap(lineItem -> invoiceRepository.findById(lineItem.getInvoiceId()))
			.map(this::toResponse)
			.orElseGet(() -> aggregateNewUsage(request));
	}

	private CreateInvoiceResponse aggregateNewUsage(CreateInvoiceRequest request) {
		Invoice invoice = invoiceRepository
			.findByTenantIdAndBillingPeriodKeyAndCurrencyCode(
				request.getTenantId(),
				request.getBillingPeriodKey(),
				request.getCurrencyCode()
			)
			.orElseGet(() -> invoiceRepository.saveAndFlush(
				Invoice.draft(request.getTenantId(), request.getBillingPeriodKey(), request.getCurrencyCode())
			));

		InvoiceLineItem lineItem = InvoiceLineItem.create(
			invoice.getInvoiceId(),
			request.getUsageEventId(),
			request.getAmountMinor()
		);

		try {
			invoiceLineItemRepository.saveAndFlush(lineItem);
		}
		catch (DataIntegrityViolationException ex) {
			return invoiceLineItemRepository.findByUsageEventId(request.getUsageEventId())
				.flatMap(existing -> invoiceRepository.findById(existing.getInvoiceId()))
				.map(this::toResponse)
				.orElseThrow(() -> ex);
		}

		invoice.addLineItemAmount(request.getAmountMinor());
		invoiceRepository.save(invoice);
		return toResponse(invoice);
	}

	private CreateInvoiceResponse toResponse(Invoice invoice) {
		return CreateInvoiceResponse.newBuilder()
			.setInvoiceId(invoice.getInvoiceId())
			.setTenantId(invoice.getTenantId())
			.setTotalMinor(invoice.getTotalMinor())
			.setCurrencyCode(invoice.getCurrencyCode())
			.setStatus(invoice.getStatus())
			.setBillingPeriodKey(invoice.getBillingPeriodKey())
			.build();
	}
}
