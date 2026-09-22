CREATE TABLE payment_orders (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    subscription_id BIGINT NOT NULL REFERENCES subscriptions(id),
    amount NUMERIC(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    provider VARCHAR(20) NOT NULL,
    external_reference VARCHAR(100) NOT NULL,
    provider_transaction_id VARCHAR(150),
    idempotency_key VARCHAR(100) NOT NULL,
    paid_at TIMESTAMP WITH TIME ZONE,
    failed_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_payment_orders_amount CHECK (amount > 0),
    CONSTRAINT ck_payment_orders_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_payment_orders_status CHECK (status IN ('PENDING','PAID','FAILED','CANCELLED','EXPIRED')),
    CONSTRAINT ck_payment_orders_provider CHECK (provider IN ('MANUAL')),
    CONSTRAINT uq_payment_orders_external_reference UNIQUE (external_reference),
    CONSTRAINT uq_payment_orders_company_idempotency UNIQUE (company_id, idempotency_key)
);

CREATE UNIQUE INDEX uq_payment_orders_pending_subscription
    ON payment_orders(subscription_id) WHERE status = 'PENDING';
