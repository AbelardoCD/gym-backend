-- Fecha de inscripción del usuario; para los existentes se toma la fecha de alta.
ALTER TABLE users ADD COLUMN enrolled_on DATE;
UPDATE users SET enrolled_on = created_at::date;
ALTER TABLE users ALTER COLUMN enrolled_on SET NOT NULL;
ALTER TABLE users ALTER COLUMN enrolled_on SET DEFAULT CURRENT_DATE;

-- Catálogo de membresías: cada una tiene un periodo y un precio que el administrador configura.
CREATE TABLE membership_plans (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(80)    NOT NULL UNIQUE,
    access     VARCHAR(20)    NOT NULL CHECK (access IN ('GYM', 'CROSSFIT', 'AMBOS')),
    period     VARCHAR(10)    NOT NULL CHECK (period IN ('DIA', 'SEMANA', 'MES', 'ANIO')),
    price      NUMERIC(10, 2) NOT NULL CHECK (price >= 0),
    active     BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ    NOT NULL DEFAULT now()
);

-- Precios iniciales en 0: se ajustan desde la pantalla de membresías.
INSERT INTO membership_plans (name, access, period, price) VALUES
    ('Gym - Día',          'GYM',      'DIA',    0),
    ('Gym - Semana',       'GYM',      'SEMANA', 0),
    ('Gym - Mes',          'GYM',      'MES',    0),
    ('Gym - Año',          'GYM',      'ANIO',   0),
    ('CrossFit - Día',     'CROSSFIT', 'DIA',    0),
    ('CrossFit - Semana',  'CROSSFIT', 'SEMANA', 0),
    ('CrossFit - Mes',     'CROSSFIT', 'MES',    0),
    ('CrossFit - Año',     'CROSSFIT', 'ANIO',   0);

-- Pagos. Se guarda una copia del nombre y periodo de la membresía por si después cambia.
CREATE TABLE payments (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT         NOT NULL REFERENCES users (id),
    plan_id        BIGINT         REFERENCES membership_plans (id) ON DELETE SET NULL,
    plan_name      VARCHAR(80)    NOT NULL,
    period         VARCHAR(10)    NOT NULL CHECK (period IN ('DIA', 'SEMANA', 'MES', 'ANIO')),
    quantity       INT            NOT NULL CHECK (quantity > 0),
    amount         NUMERIC(10, 2) NOT NULL CHECK (amount >= 0),
    method         VARCHAR(20)    NOT NULL CHECK (method IN ('EFECTIVO', 'TARJETA', 'TRANSFERENCIA')),
    covers_from    DATE           NOT NULL,
    covers_to      DATE           NOT NULL,
    notes          VARCHAR(255),
    registered_by  BIGINT         NOT NULL REFERENCES users (id),
    paid_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CHECK (covers_to >= covers_from)
);
CREATE INDEX payments_user_idx ON payments (user_id, covers_to DESC);
CREATE INDEX payments_paid_at_idx ON payments (paid_at DESC);

-- Nuevas pantallas.
INSERT INTO permissions (code, name, description, sort_order) VALUES
    ('PAGOS',       'Pagos',       'Registrar pagos y ver la lista de pagos', 80),
    ('MEMBRESIAS',  'Membresías',  'Configurar los tipos de membresía y sus precios', 90);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN ('PAGOS', 'MEMBRESIAS')
WHERE r.name = 'ADMINISTRADOR';

-- Recepción cobra, pero no cambia precios.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code = 'PAGOS'
WHERE r.name = 'RECEPCION';
