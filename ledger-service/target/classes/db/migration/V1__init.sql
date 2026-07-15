CREATE TABLE processed_events (
    id           UUID PRIMARY KEY,
    reference    VARCHAR(255) UNIQUE NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE outbox_events (
    id             UUID PRIMARY KEY,
    aggregate_id   UUID,
    aggregate_type VARCHAR(100),
    event_type     VARCHAR(100),
    payload        TEXT,
    status         VARCHAR(50),
    retry_count    INT DEFAULT 0,
    last_error     TEXT,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    published_at   TIMESTAMP
);
