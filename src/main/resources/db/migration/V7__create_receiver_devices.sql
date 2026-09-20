CREATE TABLE receiver_devices (
    id BIGSERIAL PRIMARY KEY,

    company_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,

    name VARCHAR(150) NOT NULL,

    token_hash VARCHAR(64) NOT NULL UNIQUE,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    last_seen_at TIMESTAMP,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT fk_receiver_devices_company
        FOREIGN KEY (company_id)
        REFERENCES companies(id),

    CONSTRAINT fk_receiver_devices_branch
        FOREIGN KEY (branch_id)
        REFERENCES branches(id)
);

CREATE INDEX idx_receiver_devices_company_branch
    ON receiver_devices(company_id, branch_id);