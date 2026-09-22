ALTER TABLE subscriptions
    DROP CONSTRAINT ck_subscriptions_ended_at,
    DROP CONSTRAINT ck_subscriptions_cancelled_at,
    DROP CONSTRAINT ck_subscriptions_status_ended_at;

ALTER TABLE subscriptions
    ADD CONSTRAINT ck_subscriptions_lifecycle_dates CHECK (
        (status = 'PENDING_PAYMENT' AND started_at IS NULL AND ended_at IS NULL AND cancelled_at IS NULL)
        OR (status IN ('ACTIVE', 'PAST_DUE') AND started_at IS NOT NULL AND ended_at IS NULL AND cancelled_at IS NULL)
        OR (status = 'CANCELLED' AND cancelled_at IS NOT NULL AND ended_at IS NOT NULL
            AND (started_at IS NULL OR (cancelled_at >= started_at AND ended_at >= started_at)))
        OR (status = 'EXPIRED' AND started_at IS NOT NULL AND ended_at IS NOT NULL
            AND cancelled_at IS NULL AND ended_at >= started_at)
    );
