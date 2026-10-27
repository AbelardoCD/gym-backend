-- Perfil del socio: foto, contacto de emergencia y datos médicos.
ALTER TABLE users
    ADD COLUMN photo_key       VARCHAR(255),
    ADD COLUMN birth_date      DATE,
    ADD COLUMN emergency_name  VARCHAR(120),
    ADD COLUMN emergency_phone VARCHAR(40),
    ADD COLUMN medical_notes   VARCHAR(1000);

-- Medidas corporales: cada registro es una toma en una fecha; todos los campos son opcionales.
CREATE TABLE body_measurements (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    measured_on   DATE         NOT NULL,
    weight_kg     NUMERIC(5,2),
    height_cm     NUMERIC(5,1),
    body_fat_pct  NUMERIC(4,1),
    waist_cm      NUMERIC(5,1),
    chest_cm      NUMERIC(5,1),
    hip_cm        NUMERIC(5,1),
    arm_cm        NUMERIC(5,1),
    thigh_cm      NUMERIC(5,1),
    notes         VARCHAR(500),
    registered_by BIGINT       REFERENCES users (id) ON DELETE SET NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_body_measurements_user ON body_measurements (user_id, measured_on);

INSERT INTO permissions (code, name, description, sort_order) VALUES
    ('PROGRESO', 'Progreso de socios', 'Ver el perfil de los socios y registrar sus medidas', 25);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code = 'PROGRESO'
WHERE r.name IN ('ADMINISTRADOR', 'ENTRENADOR');
