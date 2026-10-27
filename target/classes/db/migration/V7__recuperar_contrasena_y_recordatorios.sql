-- Enlaces para restablecer la contraseña. Solo se guarda el hash del token.
CREATE TABLE password_reset_tokens (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX password_reset_tokens_user_idx ON password_reset_tokens (user_id, created_at DESC);

-- Recordatorios de vencimiento ya enviados, para no mandar el mismo dos veces.
CREATE TABLE membership_reminders (
    user_id     BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    paid_until  DATE        NOT NULL,
    kind        VARCHAR(20) NOT NULL CHECK (kind IN ('POR_VENCER', 'VENCIDA')),
    sent_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, paid_until, kind)
);
