CREATE SCHEMA IF NOT EXISTS iam; -- Identity and access management: handless all things related to user accounts.

-- Small table required by net.javacrumbs.shedlock.
-- Prevents issues when you call scheduler in Kubernets or similar environment.
CREATE TABLE iam.shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);

-- Main users table.
CREATE TABLE iam.users (
    -- Identificator.
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- When user account was created.
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- When user account was last modified.
    modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Username visible on frontend. Must be present.
    username VARCHAR(100) NOT NULL,
    -- User email. 'UNIQUE' ensures no two users can register with the same email. Business key.
    email VARCHAR(100) NOT NULL UNIQUE,
    -- Password as hash.
    password VARCHAR(100) NOT NULL,
    -- Language code like 'en'.
    lang VARCHAR(10) NOT NULL,
    -- Status of user.
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACTIVE')),
    -- Is user blocked?
    blocked BOOLEAN NOT NULL DEFAULT FALSE
);

-- Tokens for user.
CREATE TABLE iam.tokens (
    -- Identificator.
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- Foreign key to user table.
    id_user BIGINT NOT NULL,

    -- When token was created.
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- When token expires. Expired tokens cannot be used and will eventually be removed.
    expires_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Token type.
    type VARCHAR(50) NOT NULL CHECK (type IN ('ACTIVATE', 'PASSWORD', 'REMOVAL')),
    -- Token value itself. Business key.
    token VARCHAR(128) NOT NULL UNIQUE,

    -- Table-level constraint for Foreign Key
    CONSTRAINT fk_user FOREIGN KEY (id_user) REFERENCES iam.users(id) ON DELETE CASCADE
);

-- User history.
CREATE TABLE iam.history (
    -- Identificator.
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- UUID. Business key.
    uuid VARCHAR(128) NOT NULL UNIQUE,
    -- Foreign key to user table.
    id_user BIGINT NOT NULL,

    -- When user history entry was created.
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Who caused user history event?
    who VARCHAR(50) NOT NULL CHECK (who IN ('USER', 'OPERATOR', 'SYSTEM')),
    -- What caused user history event?
    what VARCHAR(50) NOT NULL CHECK (what IN ('CREATED', 'ACTIVATED', 'PASS_RESET', 'LOGIN')),

    -- Table-level constraint for Foreign Key
    CONSTRAINT fk_user FOREIGN KEY (id_user) REFERENCES iam.users(id) ON DELETE CASCADE
);
