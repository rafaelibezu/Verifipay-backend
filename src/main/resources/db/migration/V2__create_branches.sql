CREATE TABLE branches (
    id BIGSERIAL PRIMARY KEY,

    company_id BIGINT NOT NULL,

    name VARCHAR(150) NOT NULL,
    address VARCHAR(250),

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT fk_branches_company
        FOREIGN KEY (company_id)
        REFERENCES companies(id)
);