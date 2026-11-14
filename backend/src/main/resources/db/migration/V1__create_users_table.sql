CREATE TABLE users (
    id            BIGSERIAL    PRIMARY KEY,
    email         VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(50)  NOT NULL,
    bio           VARCHAR(300),
    is_public     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_email UNIQUE (email)
);
