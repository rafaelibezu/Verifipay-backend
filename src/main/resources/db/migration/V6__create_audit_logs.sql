CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,

    company_id BIGINT,
    user_id BIGINT,

    action VARCHAR(80) NOT NULL,
    entity VARCHAR(80),
    entity_id BIGINT,

    details VARCHAR(500),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_audit_logs_company
        FOREIGN KEY (company_id)
        REFERENCES companies(id),

    CONSTRAINT fk_audit_logs_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
);

CREATE INDEX idx_audit_logs_company_created
ON audit_logs (
    company_id,
    created_at
);