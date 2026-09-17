CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,

    company_id BIGINT NOT NULL,
    branch_id BIGINT,

    name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,

    role VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT fk_users_company
        FOREIGN KEY (company_id)
        REFERENCES companies(id),

    CONSTRAINT fk_users_branch
        FOREIGN KEY (branch_id)
        REFERENCES branches(id)
);