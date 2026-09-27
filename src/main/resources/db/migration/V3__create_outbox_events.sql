-- OUTBOX EVENTS
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    aggregate_type VARCHAR(50) NOT NULL,

    aggregate_id UUID NOT NULL,

    event_type VARCHAR(100) NOT NULL,

    payload JSONB NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    retry_count INT NOT NULL DEFAULT 0,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    published_at TIMESTAMP WITH TIME ZONE,

    error_message TEXT,

    CONSTRAINT chk_outbox_status
        CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED'))
);

CREATE INDEX idx_outbox_status_created_at
    ON outbox_events (status, created_at);

CREATE INDEX idx_outbox_aggregate
    ON outbox_events (aggregate_type, aggregate_id);
