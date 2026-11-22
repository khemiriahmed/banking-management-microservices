CREATE TABLE roles (
    name VARCHAR(20) PRIMARY KEY
);

CREATE TABLE user_credentials (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES user_credentials(id) ON DELETE CASCADE,
    role_name VARCHAR(20) NOT NULL REFERENCES roles(name),
    PRIMARY KEY (user_id, role_name)
);

INSERT INTO roles (name) VALUES ('CLIENT'), ('ADMIN');
