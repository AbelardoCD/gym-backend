-- Cada permiso es una pantalla del sistema; cada rol decide qué pantallas puede ver.
CREATE TABLE permissions (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(40)  NOT NULL UNIQUE,
    name        VARCHAR(80)  NOT NULL,
    description VARCHAR(255),
    sort_order  INT          NOT NULL DEFAULT 0
);

INSERT INTO permissions (code, name, description, sort_order) VALUES
    ('INICIO',             'Inicio',                 'Ver las rutinas asignadas al usuario', 10),
    ('RUTINAS',            'Rutinas',                'Crear, editar y asignar rutinas', 20),
    ('ASISTENCIA',         'Mi asistencia',          'Registrar y consultar la asistencia propia', 30),
    ('ASISTENCIA_GESTION', 'Control de asistencia',  'Registrar y consultar la asistencia de cualquier usuario', 40),
    ('CHAT',               'Chat',                   'Chatear con otros usuarios', 50),
    ('USUARIOS',           'Usuarios',               'Crear y editar usuarios', 60),
    ('ROLES',              'Roles y permisos',       'Crear roles y decidir qué pantallas ve cada uno', 70);

CREATE TABLE role_permissions (
    role_id       BIGINT NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions (id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- Administrador: todo.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.name = 'ADMINISTRADOR';

-- Entrenador: lo que ya podía hacer.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
    ON p.code IN ('INICIO', 'RUTINAS', 'ASISTENCIA', 'ASISTENCIA_GESTION', 'CHAT')
WHERE r.name = 'ENTRENADOR';

-- Socio y cualquier otro rol que ya exista: lo básico.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
    ON p.code IN ('INICIO', 'ASISTENCIA', 'CHAT')
WHERE r.name NOT IN ('ADMINISTRADOR', 'ENTRENADOR');
