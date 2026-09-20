ALTER TABLE payment_events
ADD COLUMN source_event_id VARCHAR(150);

CREATE UNIQUE INDEX uq_payment_events_source_event_id
ON payment_events(source_event_id)
WHERE source_event_id IS NOT NULL;