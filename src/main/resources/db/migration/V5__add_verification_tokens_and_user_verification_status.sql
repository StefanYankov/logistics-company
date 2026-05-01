CREATE TABLE verification_tokens
(
    id UUID NOT NULL,
    version     BIGINT,
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    token_hash  VARCHAR(64) NOT NULL,
    token_type  VARCHAR(32) NOT NULL,
    expiry_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    user_id UUID NOT NULL,
    CONSTRAINT pk_verification_tokens PRIMARY KEY (id)
);

ALTER TABLE users
    ADD email_verified BOOLEAN;

ALTER TABLE users
    ALTER COLUMN email_verified SET NOT NULL;

ALTER TABLE verification_tokens
    ADD CONSTRAINT uc_verification_tokens_token_hash UNIQUE (token_hash);

ALTER TABLE verification_tokens
    ADD CONSTRAINT FK_VERIFICATION_TOKENS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

CREATE INDEX idx_verification_tokens_user_id ON verification_tokens (user_id);