CREATE TABLE draft
(
    id          UUID PRIMARY KEY,
    raw         BYTEA NOT NULL,
    created     TIMESTAMP NOT NULL,
    updated     TIMESTAMP NOT NULL
)
