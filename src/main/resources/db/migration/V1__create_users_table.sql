CREATE TABLE users (
    -- Identificator.
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- When user account was created.
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- When user account was last modified.
    modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Username visible on frontend. Must be present.
    username VARCHAR(100) NOT NULL,
    -- User email. 'UNIQUE' ensures no two users can register with the same email.
    email VARCHAR(100) NOT NULL UNIQUE,
    -- Password as hash.
    password VARCHAR(100) NOT NULL,
    -- Status of user.
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACTIVE')),
    -- Is user blocked?
    blocked BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE tokens (
    -- Identificator.
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- Foreign key to user table.
    id_user BIGINT NOT NULL,
    -- When token was created.
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- When token expires. Expired tokens cannot be used and will eventually be removed.
    expires_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Token value itself.
    token VARCHAR(100) NOT NULL UNIQUE,

    -- Table-level constraint for Foreign Key
    CONSTRAINT fk_user FOREIGN KEY (id_user) REFERENCES usr_users(id)
);
