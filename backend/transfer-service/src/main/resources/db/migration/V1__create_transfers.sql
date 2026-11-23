CREATE TABLE transfers (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL,
    source_account_id UUID NOT NULL,
    beneficiary_id UUID NOT NULL,
    destination_reference VARCHAR(64) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    reason VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    failure_reason VARCHAR(500),
    compensation_status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_transfers_owner_idempotency UNIQUE (owner_user_id, idempotency_key)
);

CREATE INDEX idx_transfers_owner_created ON transfers(owner_user_id, created_at DESC);
CREATE INDEX idx_transfers_status ON transfers(status);

CREATE TABLE transfer_status_history (
    id UUID PRIMARY KEY,
    transfer_id UUID NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    reason VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_transfer_status_history_transfer ON transfer_status_history(transfer_id, created_at ASC);
