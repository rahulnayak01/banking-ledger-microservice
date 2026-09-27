ALTER TABLE transactions
    ADD COLUMN IF NOT EXISTS request_hash VARCHAR(64);
