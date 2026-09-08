package com.lordkay.billing.invoicing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
	name = "invoices",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_invoices_tenant_period_currency",
		columnNames = { "tenant_id", "billing_period_key", "currency_code" }
	)
)
public class Invoice {

	@Id
	@Column(name = "invoice_id", nullable = false, updatable = false, length = 64)
	private String invoiceId;

	@Column(name = "tenant_id", nullable = false, length = 128)
	private String tenantId;

	@Column(name = "billing_period_key", nullable = false, length = 16)
	private String billingPeriodKey;

	@Column(name = "currency_code", nullable = false, length = 3)
	private String currencyCode;

	@Column(name = "total_minor", nullable = false)
	private long totalMinor;

	@Column(name = "status", nullable = false, length = 32)
	private String status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Invoice() {
	}

	public static Invoice draft(String tenantId, String billingPeriodKey, String currencyCode) {
		Invoice invoice = new Invoice();
		invoice.invoiceId = "inv-" + UUID.randomUUID();
		invoice.tenantId = tenantId;
		invoice.billingPeriodKey = billingPeriodKey;
		invoice.currencyCode = currencyCode;
		invoice.totalMinor = 0L;
		invoice.status = "DRAFT";
		invoice.createdAt = Instant.now();
		return invoice;
	}

	public void addLineItemAmount(long amountMinor) {
		this.totalMinor += amountMinor;
	}

	public String getInvoiceId() {
		return invoiceId;
	}

	public String getTenantId() {
		return tenantId;
	}

	public String getBillingPeriodKey() {
		return billingPeriodKey;
	}

	public String getCurrencyCode() {
		return currencyCode;
	}

	public long getTotalMinor() {
		return totalMinor;
	}

	public String getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
