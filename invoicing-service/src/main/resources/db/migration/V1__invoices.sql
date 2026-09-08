CREATE TABLE invoices (
    invoice_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(128) NOT NULL,
    billing_period_key VARCHAR(16) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    total_minor BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_invoices_tenant_period_currency UNIQUE (tenant_id, billing_period_key, currency_code)
);

CREATE TABLE invoice_line_items (
    line_item_id BIGSERIAL PRIMARY KEY,
    invoice_id VARCHAR(64) NOT NULL REFERENCES invoices(invoice_id),
    usage_event_id VARCHAR(36) NOT NULL,
    amount_minor BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_invoice_line_items_usage_event UNIQUE (usage_event_id)
);

CREATE INDEX idx_invoice_line_items_invoice_id ON invoice_line_items (invoice_id);
