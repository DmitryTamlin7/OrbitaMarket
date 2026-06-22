CREATE TABLE accounts (
                          id UUID PRIMARY KEY,
                          user_id VARCHAR(255) NOT NULL UNIQUE,
                          balance BIGINT NOT NULL DEFAULT 0 CHECK (balance >= 0),
                          version BIGINT NOT NULL DEFAULT 0
);


CREATE INDEX idx_accounts_user_id ON accounts(user_id);


CREATE TABLE orders (
                        id UUID PRIMARY KEY,
                        user_id VARCHAR(255) NOT NULL,
                        price BIGINT NOT NULL,
                        status VARCHAR(50) NOT NULL
);