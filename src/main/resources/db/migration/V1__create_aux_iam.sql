-- ============================================================================
-- Helper tables.
-- ============================================================================

-- Auxiliary schema: UserLand system and auxiliary tables. Includes tables used by third party libraries like Flyway or
-- ShedLock.
-- CREATE SCHEMA IF NOT EXISTS aux; -- commented as Flyway already creates it

-- Small table required by net.javacrumbs.shedlock.
-- Prevents issues when you call scheduler in Kubernets or similar multi-node environment.
-- It ensures only one given scheduler method is called at once.
-- See UserScheduler for example of @SchedulerLock use.
CREATE TABLE aux.shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);

-- General system configuration. For configuration that should go in effect immediately without restarting the system.
CREATE TABLE aux.config (
    -- Identificator.
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- Configuration variable name.
    name TEXT NOT NULL,
    -- Configuration variable value.
    value TEXT NOT NULL,
    -- Description of configuration variable for the developer.
    description TEXT NOT NULL
);
-- Known configuration.
INSERT INTO aux.config (name, value, description) VALUES ('user.lockdown', '0', 'If 1, no user can log in unless they have admin permissions.');

-- ============================================================================
-- Tables for entities specific to our UserLand system.
-- ============================================================================

-- Identity and access management schema: handles all things related to user accounts.
CREATE SCHEMA IF NOT EXISTS iam;

-- Permissions available for users.
-- Field name here combines with value in iam.user_permissions to create valid permission.
-- For example, name='role' and value='admin' results in "ROLE_ADMIN" for Spring authorization.
CREATE TABLE iam.permissions (
    -- Identificator.
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- Name of permission entry. Must be unique.
    name VARCHAR(255) NOT NULL UNIQUE,
    -- If true, this permission should be embedded in JWT key.
    in_jwt bool NOT NULL,
    -- If true, this permission should be embedded in Spring authorities.
    in_authorities bool NOT NULL
);
-- Known permissions.
INSERT INTO iam.permissions (name, in_jwt, in_authorities) VALUES ('role', true, true); -- general role
INSERT INTO iam.permissions (name, in_jwt, in_authorities) VALUES ('user', true, true); -- what you can do in user domain

-- Main users table. Contains data about user accounts.
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
    lang VARCHAR(2) NOT NULL,

    -- Status of user.
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACTIVE')),
    -- Is user locked?
    locked BOOLEAN NOT NULL DEFAULT FALSE
);

-- User profile table. Contains business data about users. Note user do not have any id_profile column (since profile id
-- is same as main user id) - you are one responsible for ensuring all users have profiles if required.
CREATE TABLE iam.profiles (
    -- Identificator AND Foreign Key.
    -- It is not GENERATED ALWAYS AS IDENTITY because it inherits the ID from iam.users.
    id BIGINT PRIMARY KEY,

    -- Name of user.
    name VARCHAR(100),
    -- Surname of user.
    surname VARCHAR(100),

    -- Table-level constraint making the Primary Key also act as the Foreign Key.
    CONSTRAINT fk_profile_user FOREIGN KEY (id) REFERENCES iam.users(id) ON DELETE CASCADE
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
    type VARCHAR(50) NOT NULL CHECK (type IN ('ACTIVATE', 'PASSWORD', 'DELETE')),
    -- Token value itself. Business key.
    token VARCHAR(128) NOT NULL UNIQUE,

    -- Table-level constraint for Foreign Key.
    CONSTRAINT fk_user FOREIGN KEY (id_user) REFERENCES iam.users(id) ON DELETE CASCADE,
    -- There can be only one token of given type for given user at once.
    CONSTRAINT uq_user_token_type UNIQUE (id_user, type)
);

-- JWT for user. Exists because we need ability to revoke them.
CREATE TABLE iam.jwt (
    -- Identificator.
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- Foreign key to user table.
    id_user BIGINT NOT NULL,

    -- When token was created.
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- When token expires. Expired tokens cannot be used and will eventually be removed.
    expires_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Token value itself. Business key.
    token TEXT NOT NULL UNIQUE,

    -- Table-level constraint for Foreign Key.
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
    what VARCHAR(50) NOT NULL CHECK (what IN ('CREATE', 'ACTIVATE', 'EDIT', 'PASS_RESET_REQ', 'PASS_RESET', 'DELETE_REQ', 'LOGIN', 'LOGOUT')),
    -- Parameters for user history event.
    params TEXT NOT NULL,

    -- Table-level constraint for Foreign Key.
    CONSTRAINT fk_user FOREIGN KEY (id_user) REFERENCES iam.users(id) ON DELETE CASCADE
);

-- User permission entry.
CREATE TABLE iam.user_permissions (
    -- Identificator.
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- UUID. Business key.
    uuid VARCHAR(128) NOT NULL UNIQUE,
    -- Foreign key to user table.
    id_user BIGINT NOT NULL,
    -- Foreign key to permission table.
    id_permission BIGINT NOT NULL,

    -- When user permission entry was created.
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Value of permission entry.
    value TEXT NOT NULL,

    -- Table-level constraint for Foreign Key.
    CONSTRAINT fk_user FOREIGN KEY (id_user) REFERENCES iam.users(id) ON DELETE CASCADE,
    -- Table-level constraint for Foreign Key.
    CONSTRAINT fk_permission FOREIGN KEY (id_permission) REFERENCES iam.permissions(id) ON DELETE CASCADE,
    -- There can be only one permission name+value combination for given user at once.
    CONSTRAINT uq_user_permission UNIQUE (id_user, id_permission, value)
);

-- ============================================================================
-- Other system tables.
-- ============================================================================

-- System history.
CREATE TABLE aux.history (
    -- Identificator.
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- UUID. Business key.
    uuid VARCHAR(128) NOT NULL UNIQUE,
    -- Foreign key to user table. Note it is optional!
    id_user BIGINT,

    -- When system history entry was created.
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Who caused system history event?
    who VARCHAR(50) NOT NULL CHECK (who IN ('OPERATOR', 'ADMIN', 'SYSTEM')),
    -- What caused system history event?
    what VARCHAR(50) NOT NULL CHECK (what IN ('LOCKDOWN')),
    -- Parameters for system history event.
    params TEXT NOT NULL,

    -- Table-level constraint for Foreign Key.
    CONSTRAINT fk_user FOREIGN KEY (id_user) REFERENCES iam.users(id)
);
