CREATE TABLE plans (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(150) NOT NULL,
    price NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    billing_interval VARCHAR(20) NOT NULL,
    max_cashiers INTEGER NOT NULL,
    max_receiver_devices INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_plans_code UNIQUE (code),
    CONSTRAINT ck_plans_price CHECK (price > 0),
    CONSTRAINT ck_plans_max_cashiers CHECK (max_cashiers >= 0),
    CONSTRAINT ck_plans_max_receiver_devices CHECK (max_receiver_devices >= 0),
    CONSTRAINT ck_plans_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_plans_billing_interval CHECK (billing_interval IN ('MONTHLY'))
);

CREATE TABLE subscriptions (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    plan_id BIGINT NOT NULL REFERENCES plans(id),
    status VARCHAR(30) NOT NULL,
    contracted_price NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    billing_interval VARCHAR(20) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    ended_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_subscriptions_price CHECK (contracted_price > 0),
    CONSTRAINT ck_subscriptions_status CHECK (
        status IN ('PENDING_PAYMENT', 'ACTIVE', 'PAST_DUE', 'CANCELLED', 'EXPIRED')
    ),
    CONSTRAINT ck_subscriptions_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_subscriptions_billing_interval CHECK (billing_interval IN ('MONTHLY')),
    CONSTRAINT ck_subscriptions_ended_at CHECK (
        (started_at IS NULL AND ended_at IS NULL)
        OR (started_at IS NOT NULL AND (ended_at IS NULL OR ended_at >= started_at))
    ),
    CONSTRAINT ck_subscriptions_cancelled_at CHECK (
        cancelled_at IS NULL
        OR (started_at IS NOT NULL AND cancelled_at >= started_at)
    ),
    CONSTRAINT ck_subscriptions_status_ended_at CHECK (
        (status IN ('PENDING_PAYMENT', 'ACTIVE', 'PAST_DUE') AND ended_at IS NULL)
        OR (status IN ('CANCELLED', 'EXPIRED') AND ended_at IS NOT NULL)
    )
);

CREATE INDEX ix_subscriptions_company_history
    ON subscriptions(company_id, created_at DESC, id DESC);
CREATE UNIQUE INDEX uq_subscriptions_current_company
    ON subscriptions(company_id) WHERE ended_at IS NULL;

INSERT INTO plans(code, name, price, currency, billing_interval, max_cashiers, max_receiver_devices, active)
VALUES ('BASIC', 'BASIC', 40.00, 'PEN', 'MONTHLY', 1, 1, TRUE),
       ('PRO', 'PRO', 80.00, 'PEN', 'MONTHLY', 5, 1, TRUE);
