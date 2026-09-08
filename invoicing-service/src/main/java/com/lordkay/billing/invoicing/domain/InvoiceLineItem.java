package com.lordkay.billing.invoicing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "invoice_line_items")
public class InvoiceLineItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "line_item_id")
	private Long lineItemId;

	@Column(name = "invoice_id", nullable = false, length = 64)
	private String invoiceId;

	@Column(name = "usage_event_id", nullable = false, unique = true, length = 36)
	private String usageEventId;

	@Column(name = "amount_minor", nullable = false)
	private long amountMinor;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected InvoiceLineItem() {
	}

	public static InvoiceLineItem create(String invoiceId, String usageEventId, long amountMinor) {
		InvoiceLineItem lineItem = new InvoiceLineItem();
		lineItem.invoiceId = invoiceId;
		lineItem.usageEventId = usageEventId;
		lineItem.amountMinor = amountMinor;
		lineItem.createdAt = Instant.now();
		return lineItem;
	}

	public String getInvoiceId() {
		return invoiceId;
	}

	public String getUsageEventId() {
		return usageEventId;
	}

	public long getAmountMinor() {
		return amountMinor;
	}
}
