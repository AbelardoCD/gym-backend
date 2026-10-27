-- Los roles pasan a su propia tabla y un usuario puede tener varios.
CREATE TABLE roles (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(30)  NOT NULL UNIQUE,
    description VARCHAR(255)
);

INSERT INTO roles (name, description) VALUES
    ('ADMINISTRADOR', 'Administra usuarios y todo el sistema'),
    ('ENTRENADOR', 'Crea rutinas y las asigna a socios'),
    ('SOCIO', 'Miembro del gimnasio');

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles (id),
    PRIMARY KEY (user_id, role_id)
);

-- Conserva el rol que ya tenía cada usuario.
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.name = u.role;

ALTER TABLE users DROP COLUMN role;
