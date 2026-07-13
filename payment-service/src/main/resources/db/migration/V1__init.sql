CREATE TABLE payments (
    payment_id         UUID PRIMARY KEY,
    sender_wallet_id   UUID,
    receiver_wallet_id UUID,
    amount             DECIMAL(19, 2) NOT NULL,
    currency           VARCHAR(3) NOT NULL,
    status             VARCHAR(50) NOT NULL,
    reference          VARCHAR(255) UNIQUE NOT NULL,
    narration          VARCHAR(255),
    failure_reason     VARCHAR(255),
    occurred_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at       TIMESTAMP
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

CREATE TABLE payment_saga (
    id                  UUID PRIMARY KEY,
    payment_id          UUID UNIQUE NOT NULL REFERENCES payments(payment_id),
    current_step        VARCHAR(50) NOT NULL,
    debit_completed_at  TIMESTAMP,
    ledger_posted_at    TIMESTAMP,
    completed_at        TIMESTAMP,
    failed_at           TIMESTAMP,
    failure_reason      VARCHAR(500),
    compensation_reason VARCHAR(500),
    reversed_at         TIMESTAMP,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE saga_events (
    id           UUID PRIMARY KEY,
    payment_id   UUID NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    payload      TEXT,
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE dead_letter_events (
    id            UUID PRIMARY KEY,
    topic         VARCHAR(100) NOT NULL,
    partition     INT,
    kafka_offset  BIGINT,
    event_type    VARCHAR(100),
    payload       TEXT,
    error_message TEXT,
    retry_count   INT DEFAULT 0,
    status        VARCHAR(20) DEFAULT 'UNRESOLVED',
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
