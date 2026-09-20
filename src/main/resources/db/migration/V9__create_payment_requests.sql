CREATE TABLE payment_requests (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    branch_id BIGINT REFERENCES branches(id),
    cashier_id BIGINT NOT NULL REFERENCES users(id),
    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    status VARCHAR(30) NOT NULL DEFAULT 'WAITING'
        CHECK (status IN ('WAITING', 'PAID', 'AMBIGUOUS', 'EXPIRED', 'CANCELLED')),
    client_request_id VARCHAR(150) NOT NULL CHECK (btrim(client_request_id) <> ''),
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    paid_at TIMESTAMPTZ,
    CONSTRAINT uq_payment_requests_cashier_client UNIQUE (cashier_id, client_request_id),
    CONSTRAINT ck_payment_requests_expiration CHECK (expires_at > created_at)
);

CREATE INDEX idx_payment_requests_cashier_history
    ON payment_requests (cashier_id, company_id, created_at DESC, id DESC);
