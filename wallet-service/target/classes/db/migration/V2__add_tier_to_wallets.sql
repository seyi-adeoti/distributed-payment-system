-- Add kyc_tier to wallets
ALTER TABLE wallets ADD COLUMN kyc_tier VARCHAR(50) DEFAULT 'TIER_0';
