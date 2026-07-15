ALTER TABLE users
    ADD COLUMN isUsing2FA bit,
    ADD COLUMN secret VARCHAR(255),
    ADD COLUMN reset_token VARCHAR(255),
ADD COLUMN reset_token_expiry TIMESTAMP;