CREATE TABLE transaction_records (
    id UUID PRIMARY KEY,
    external_reference VARCHAR(128) NOT NULL UNIQUE,
    transfer_id UUID NOT NULL,
    owner_user_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_transaction_records_owner_occurred ON transaction_records(owner_user_id, occurred_at DESC);
CREATE INDEX idx_transaction_records_transfer ON transaction_records(transfer_id);

CREATE TABLE transaction_entries (
    id UUID PRIMARY KEY,
    transaction_record_id UUID NOT NULL,
    account_id UUID,
    destination_reference VARCHAR(64),
    side VARCHAR(10) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    CONSTRAINT fk_transaction_entries_record FOREIGN KEY (transaction_record_id)
        REFERENCES transaction_records(id) ON DELETE CASCADE
);

CREATE INDEX idx_transaction_entries_record ON transaction_entries(transaction_record_id);
CREATE INDEX idx_transaction_entries_account ON transaction_entries(account_id);
CREATE INDEX idx_transaction_entries_destination ON transaction_entries(destination_reference);
