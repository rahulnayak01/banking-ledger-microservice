-- PROCESSED EVENTS FOR CONSUMER IDEMPOTENCY
CREATE TABLE processed_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    event_id UUID NOT NULL,

    consumer_group VARCHAR(100) NOT NULL,

    event_type VARCHAR(100) NOT NULL,

    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_event_consumer UNIQUE (event_id, consumer_group)
);

CREATE INDEX idx_processed_events_consumer
    ON processed_events (consumer_group, processed_at);
