CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- CUSTOMERS

CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    external_customer_id VARCHAR(100) NOT NULL UNIQUE,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- ACCOUNTS

CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    customer_id UUID NOT NULL,

    account_number VARCHAR(30) NOT NULL UNIQUE,

    account_type VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',

    currency VARCHAR(3) NOT NULL,

    status VARCHAR(20) NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_accounts_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(id),

    CONSTRAINT chk_account_status
        CHECK (status IN ('ACTIVE', 'BLOCKED', 'CLOSED')),

    CONSTRAINT chk_account_type
    CHECK (account_type IN ('CUSTOMER', 'SYSTEM')),

    CONSTRAINT chk_account_currency
        CHECK (currency ~ '^[A-Z]{3}$')
);

-- TRANSACTIONS

CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    transaction_type VARCHAR(30) NOT NULL,

    status VARCHAR(30) NOT NULL,

    idempotency_key VARCHAR(100) UNIQUE,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_transaction_status
        CHECK (
            status IN (
                'PENDING',
                'COMPLETED',
                'FAILED'
            )
        )
);

-- LEDGER ENTRIES

CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    transaction_id UUID NOT NULL,

    account_id UUID NOT NULL,

    entry_type VARCHAR(10) NOT NULL,

    amount NUMERIC(19, 4) NOT NULL,

    currency VARCHAR(3) NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ledger_transaction
        FOREIGN KEY (transaction_id)
        REFERENCES transactions(id),

    CONSTRAINT fk_ledger_account
        FOREIGN KEY (account_id)
        REFERENCES accounts(id),

    CONSTRAINT chk_entry_type
        CHECK (entry_type IN ('DEBIT', 'CREDIT')),

    CONSTRAINT chk_entry_amount
        CHECK (amount > 0),

    CONSTRAINT chk_entry_currency
        CHECK (currency ~ '^[A-Z]{3}$')
);

-- EVENT STORE

CREATE TABLE events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    aggregate_id UUID NOT NULL,

    aggregate_type VARCHAR(50) NOT NULL,

    event_type VARCHAR(100) NOT NULL,

    sequence_number BIGINT NOT NULL,

    payload JSONB NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_event_sequence
        UNIQUE (aggregate_id, sequence_number)
);

-- BALANCE PROJECTION

CREATE TABLE account_balances (
    account_id UUID PRIMARY KEY,

    ledger_balance NUMERIC(19, 4) NOT NULL DEFAULT 0,

    available_balance NUMERIC(19, 4) NOT NULL DEFAULT 0,

    version BIGINT NOT NULL DEFAULT 0,

    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_balance_account
        FOREIGN KEY (account_id)
        REFERENCES accounts(id)
);

-- INDEXES

CREATE INDEX idx_accounts_customer
    ON accounts(customer_id);


CREATE INDEX idx_ledger_entries_account
    ON ledger_entries(account_id);


CREATE INDEX idx_ledger_entries_transaction
    ON ledger_entries(transaction_id);


CREATE INDEX idx_ledger_entries_created_at
    ON ledger_entries(created_at);


CREATE INDEX idx_events_aggregate
    ON events(aggregate_id);


CREATE INDEX idx_events_created_at
    ON events(created_at);