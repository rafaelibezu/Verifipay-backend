ALTER TABLE payment_requests ADD COLUMN cancelled_at TIMESTAMPTZ;

ALTER TABLE payment_events
    ADD COLUMN matching_status VARCHAR(30),
    ADD COLUMN occurred_at_instant TIMESTAMPTZ,
    ADD COLUMN payment_request_id BIGINT,
    ADD CONSTRAINT fk_payment_events_payment_request
        FOREIGN KEY (payment_request_id) REFERENCES payment_requests(id),
    ADD CONSTRAINT uq_payment_events_payment_request UNIQUE (payment_request_id),
    ADD CONSTRAINT ck_payment_events_matching_status
        CHECK (matching_status IS NULL OR matching_status IN ('UNMATCHED', 'MATCHED', 'AMBIGUOUS')),
    ADD CONSTRAINT ck_payment_events_matching_assignment CHECK (
        (matching_status IS NOT NULL AND matching_status = 'MATCHED' AND payment_request_id IS NOT NULL)
        OR ((matching_status IS NULL OR matching_status <> 'MATCHED') AND payment_request_id IS NULL)
    ),
    ADD CONSTRAINT ck_payment_events_matching_timestamp
        CHECK (matching_status IS NULL OR occurred_at_instant IS NOT NULL),
    ADD CONSTRAINT ck_payment_events_matching_source
        CHECK (matching_status IS NULL OR source_event_id IS NOT NULL);

CREATE TABLE payment_match_candidates (
    id BIGSERIAL PRIMARY KEY,
    payment_event_id BIGINT NOT NULL,
    payment_request_id BIGINT NOT NULL,
    CONSTRAINT fk_payment_match_candidates_event
        FOREIGN KEY (payment_event_id) REFERENCES payment_events(id),
    CONSTRAINT fk_payment_match_candidates_request
        FOREIGN KEY (payment_request_id) REFERENCES payment_requests(id),
    CONSTRAINT uq_payment_match_candidates_pair UNIQUE (payment_event_id, payment_request_id)
);

CREATE INDEX idx_payment_requests_matching
    ON payment_requests (company_id, amount, created_at, expires_at)
    WHERE status IN ('WAITING', 'CANCELLED');
