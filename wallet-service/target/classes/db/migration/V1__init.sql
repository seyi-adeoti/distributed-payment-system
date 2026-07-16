CREATE TABLE wallets (
    wallet_id  UUID PRIMARY KEY,
    user_id    UUID NOT NULL,
    balance    DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    currency   VARCHAR(3) NOT NULL,
    status     VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE wallet_transactions (
    transaction_id UUID PRIMARY KEY,
    wallet_id      UUID NOT NULL REFERENCES wallets(wallet_id),
    type           VARCHAR(50) NOT NULL,
    amount         DECIMAL(19, 2) NOT NULL,
    reference      VARCHAR(255) UNIQUE NOT NULL,
    narration      VARCHAR(255),
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

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

-- Insert Dummy Data for Testing
INSERT INTO wallets (wallet_id, user_id, balance, currency, status) VALUES 
('11111111-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 50000.00, 'NGN', 'ACTIVE'),
('22222222-2222-2222-2222-222222222222', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 10000.00, 'NGN', 'ACTIVE');
