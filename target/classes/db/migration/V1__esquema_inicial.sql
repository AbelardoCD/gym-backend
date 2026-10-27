CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(150) NOT NULL,
    phone         VARCHAR(30),
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('ADMINISTRADOR', 'ENTRENADOR', 'SOCIO')),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE routines (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    description TEXT,
    trainer_id  BIGINT       NOT NULL REFERENCES users (id),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE routine_members (
    routine_id BIGINT NOT NULL REFERENCES routines (id) ON DELETE CASCADE,
    member_id  BIGINT NOT NULL REFERENCES users (id),
    PRIMARY KEY (routine_id, member_id)
);

CREATE TABLE routine_files (
    id            BIGSERIAL PRIMARY KEY,
    routine_id    BIGINT       NOT NULL REFERENCES routines (id) ON DELETE CASCADE,
    file_type     VARCHAR(10)  NOT NULL CHECK (file_type IN ('VIDEO', 'PDF', 'EXCEL', 'IMAGEN')),
    original_name VARCHAR(255) NOT NULL,
    content_type  VARCHAR(100) NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    storage_key   VARCHAR(255) NOT NULL,
    uploaded_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE attendances (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT      NOT NULL REFERENCES users (id),
    check_in_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    check_out_at TIMESTAMPTZ,
    registered_by BIGINT     REFERENCES users (id)
);
CREATE INDEX idx_attendances_user ON attendances (user_id, check_in_at DESC);

CREATE TABLE chat_requests (
    id           BIGSERIAL PRIMARY KEY,
    sender_id    BIGINT      NOT NULL REFERENCES users (id),
    receiver_id  BIGINT      NOT NULL REFERENCES users (id),
    status       VARCHAR(10) NOT NULL CHECK (status IN ('PENDIENTE', 'ACEPTADA', 'RECHAZADA')),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    responded_at TIMESTAMPTZ,
    CHECK (sender_id <> receiver_id)
);

-- Una conversación por pareja de usuarios; user_a_id siempre es el id menor.
CREATE TABLE conversations (
    id         BIGSERIAL PRIMARY KEY,
    user_a_id  BIGINT      NOT NULL REFERENCES users (id),
    user_b_id  BIGINT      NOT NULL REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_a_id, user_b_id),
    CHECK (user_a_id < user_b_id)
);

CREATE TABLE messages (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT      NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
    sender_id       BIGINT      NOT NULL REFERENCES users (id),
    content         TEXT        NOT NULL,
    sent_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    read_at         TIMESTAMPTZ
);
CREATE INDEX idx_messages_conversation ON messages (conversation_id, sent_at DESC);
