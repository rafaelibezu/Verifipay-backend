CREATE TABLE verifications (
    id BIGSERIAL PRIMARY KEY,

    company_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,
    cashier_id BIGINT NOT NULL,

    payment_event_id BIGINT,

    method VARCHAR(30) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,

    reference VARCHAR(100),
    security_code VARCHAR(20),

    status VARCHAR(30) NOT NULL,

    verified_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_verifications_company
        FOREIGN KEY (company_id)
        REFERENCES companies(id),

    CONSTRAINT fk_verifications_branch
        FOREIGN KEY (branch_id)
        REFERENCES branches(id),

    CONSTRAINT fk_verifications_cashier
        FOREIGN KEY (cashier_id)
        REFERENCES users(id),

    CONSTRAINT fk_verifications_payment_event
        FOREIGN KEY (payment_event_id)
        REFERENCES payment_events(id)
);

CREATE INDEX idx_verifications_company_branch
ON verifications (
    company_id,
    branch_id,
    created_at
);