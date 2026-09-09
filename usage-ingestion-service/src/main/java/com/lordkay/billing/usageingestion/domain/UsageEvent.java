package com.lordkay.billing.usageingestion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
	name = "usage_events",
	uniqueConstraints = @UniqueConstraint(name = "uk_usage_events_tenant_idempotency", columnNames = { "tenant_id", "idempotency_key" })
)
public class UsageEvent {

	@Id
	@Column(name = "usage_event_id", nullable = false, updatable = false, length = 36)
	private String usageEventId;

	@Column(name = "tenant_id", nullable = false, length = 128)
	private String tenantId;

	@Column(name = "meter_id", nullable = false, length = 128)
	private String meterId;

	@Column(name = "idempotency_key", nullable = false, length = 256)
	private String idempotencyKey;

	@Column(name = "quantity", nullable = false)
	private long quantity;

	@Column(name = "occurred_at_epoch_ms", nullable = false)
	private long occurredAtEpochMs;

	@Column(name = "status", nullable = false, length = 64)
	private String status;

	@Column(name = "message", nullable = false, length = 512)
	private String message;

	@Column(name = "rated_amount_minor", nullable = false)
	private long ratedAmountMinor;

	@Column(name = "invoice_id", length = 128)
	private String invoiceId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected UsageEvent() {
	}

	public static UsageEvent pending(
		String tenantId,
		String meterId,
		String idempotencyKey,
		long quantity,
		long occurredAtEpochMs
	) {
		UsageEvent event = new UsageEvent();
		event.usageEventId = UUID.randomUUID().toString();
		event.tenantId = tenantId;
		event.meterId = meterId;
		event.idempotencyKey = idempotencyKey;
		event.quantity = quantity;
		event.occurredAtEpochMs = occurredAtEpochMs;
		event.status = "PENDING";
		event.message = "Usage event accepted for tenant " + tenantId;
		event.ratedAmountMinor = 0L;
		event.invoiceId = "";
		event.createdAt = Instant.now();
		return event;
	}

	public void markAccepted(long ratedAmountMinor, String invoiceId) {
		this.status = "ACCEPTED";
		this.message = "Usage event accepted for tenant " + tenantId;
		this.ratedAmountMinor = ratedAmountMinor;
		this.invoiceId = invoiceId == null ? "" : invoiceId;
	}

	public void markDegraded(String reason) {
		this.status = "ACCEPTED_WITH_DEGRADATION";
		this.message = "Usage accepted but downstream billing unavailable: " + reason;
		this.ratedAmountMinor = 0L;
		this.invoiceId = "";
	}

	public void markInvoicingDegraded(long ratedAmountMinor, String reason) {
		this.status = "ACCEPTED_WITH_DEGRADATION";
		this.message = "Usage rated but invoicing unavailable: " + reason;
		this.ratedAmountMinor = ratedAmountMinor;
		this.invoiceId = "";
	}

	public String getUsageEventId() {
		return usageEventId;
	}

	public String getTenantId() {
		return tenantId;
	}

	public String getMeterId() {
		return meterId;
	}

	public String getIdempotencyKey() {
		return idempotencyKey;
	}

	public long getQuantity() {
		return quantity;
	}

	public long getOccurredAtEpochMs() {
		return occurredAtEpochMs;
	}

	public String getStatus() {
		return status;
	}

	public String getMessage() {
		return message;
	}

	public long getRatedAmountMinor() {
		return ratedAmountMinor;
	}

	public String getInvoiceId() {
		return invoiceId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
