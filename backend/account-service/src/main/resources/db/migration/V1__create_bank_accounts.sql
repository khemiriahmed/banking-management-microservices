CREATE TABLE bank_accounts (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL,
    account_number_masked VARCHAR(32) NOT NULL UNIQUE,
    account_type VARCHAR(20) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    balance NUMERIC(19, 2) NOT NULL,
    reserved_balance NUMERIC(19, 2) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_bank_accounts_owner ON bank_accounts(owner_user_id);
CREATE INDEX idx_bank_accounts_status ON bank_accounts(status);
