CREATE TABLE usage_events (
    usage_event_id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(128) NOT NULL,
    meter_id VARCHAR(128) NOT NULL,
    idempotency_key VARCHAR(256) NOT NULL,
    quantity BIGINT NOT NULL,
    occurred_at_epoch_ms BIGINT NOT NULL,
    status VARCHAR(64) NOT NULL,
    message VARCHAR(512) NOT NULL,
    rated_amount_minor BIGINT NOT NULL,
    invoice_id VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_usage_events_tenant_idempotency UNIQUE (tenant_id, idempotency_key)
);

CREATE INDEX idx_usage_events_tenant_created ON usage_events (tenant_id, created_at DESC);
