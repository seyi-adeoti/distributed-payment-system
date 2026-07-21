-- Add kyc_tier to users
ALTER TABLE users ADD COLUMN kyc_tier VARCHAR(50) DEFAULT 'TIER_0';

-- Add outbox_events table for transactional messaging
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
