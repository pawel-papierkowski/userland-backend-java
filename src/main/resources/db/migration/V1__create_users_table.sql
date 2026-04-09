CREATE TABLE users (
    -- 'BIGINT' matches Java's 'Long'.
    -- 'GENERATED ALWAYS AS IDENTITY' creates the auto-incrementing counter that Hibernate expects.
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    -- When user account was created.
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- When user account was last modified.
    modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Username visible on frontend. Must be present.
    username VARCHAR(255) NOT NULL,

    -- 'UNIQUE' ensures no two users can register with the same email.
    email VARCHAR(255) NOT NULL UNIQUE,

    -- BCrypt hashes are usually 60 characters, but 255 is safe.
    password VARCHAR(255) NOT NULL,

    -- Status of user.
    status VARCHAR(50) NOT NULL CHECK (status IN ('PENDING', 'ACTIVE')),

    -- Is user blocked?
    blocked BOOLEAN NOT NULL DEFAULT FALSE,
);
