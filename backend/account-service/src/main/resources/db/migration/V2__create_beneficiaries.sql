CREATE TABLE beneficiaries (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    destination_account_reference VARCHAR(32) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_beneficiaries_owner_reference UNIQUE (owner_user_id, destination_account_reference)
);

CREATE INDEX idx_beneficiaries_owner ON beneficiaries(owner_user_id);
