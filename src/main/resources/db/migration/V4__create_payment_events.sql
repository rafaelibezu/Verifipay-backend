CREATE TABLE payment_events (
    id BIGSERIAL PRIMARY KEY,

    company_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,

    method VARCHAR(30) NOT NULL,

    amount NUMERIC(12, 2) NOT NULL,

    reference VARCHAR(100),
    security_code VARCHAR(20),

    occurred_at TIMESTAMP NOT NULL,

    consumed BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_payment_events_company
        FOREIGN KEY (company_id)
        REFERENCES companies(id),

    CONSTRAINT fk_payment_events_branch
        FOREIGN KEY (branch_id)
        REFERENCES branches(id)
);

CREATE INDEX idx_payment_events_lookup
ON payment_events (
    company_id,
    branch_id,
    method,
    amount,
    consumed,
    occurred_at
);