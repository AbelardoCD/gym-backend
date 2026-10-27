-- Catálogo de ejercicios para armar rutinas, con los más comunes precargados.
CREATE TABLE exercises (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(120) NOT NULL,
    muscle_group VARCHAR(20)  NOT NULL,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_exercises_name ON exercises (lower(name));

-- Ejercicios de cada rutina, en orden, con series y repeticiones opcionales.
CREATE TABLE routine_exercises (
    id          BIGSERIAL PRIMARY KEY,
    routine_id  BIGINT       NOT NULL REFERENCES routines (id) ON DELETE CASCADE,
    exercise_id BIGINT       NOT NULL REFERENCES exercises (id),
    position    INT          NOT NULL,
    sets        INT,
    reps        VARCHAR(30),
    notes       VARCHAR(255)
);
CREATE INDEX ix_routine_exercises_routine ON routine_exercises (routine_id, position);

INSERT INTO exercises (name, muscle_group) VALUES
    -- Pecho
    ('Press de banca plano', 'PECHO'),
    ('Press de banca inclinado', 'PECHO'),
    ('Press de banca declinado', 'PECHO'),
    ('Press con mancuernas', 'PECHO'),
    ('Press inclinado con mancuernas', 'PECHO'),
    ('Aperturas con mancuernas', 'PECHO'),
    ('Cruce de poleas', 'PECHO'),
    ('Fondos en paralelas', 'PECHO'),
    ('Lagartijas', 'PECHO'),
    -- Espalda
    ('Dominadas', 'ESPALDA'),
    ('Jalón al pecho', 'ESPALDA'),
    ('Remo con barra', 'ESPALDA'),
    ('Remo con mancuerna', 'ESPALDA'),
    ('Remo en polea baja', 'ESPALDA'),
    ('Peso muerto', 'ESPALDA'),
    ('Pullover con mancuerna', 'ESPALDA'),
    ('Hiperextensiones', 'ESPALDA'),
    -- Hombro
    ('Press militar', 'HOMBRO'),
    ('Press de hombro con mancuernas', 'HOMBRO'),
    ('Press Arnold', 'HOMBRO'),
    ('Elevaciones laterales', 'HOMBRO'),
    ('Elevaciones frontales', 'HOMBRO'),
    ('Pájaros (deltoide posterior)', 'HOMBRO'),
    ('Face pull', 'HOMBRO'),
    ('Encogimientos de hombros', 'HOMBRO'),
    -- Brazo
    ('Curl con barra', 'BRAZO'),
    ('Curl con mancuernas', 'BRAZO'),
    ('Curl martillo', 'BRAZO'),
    ('Curl predicador', 'BRAZO'),
    ('Extensión de tríceps en polea', 'BRAZO'),
    ('Press francés', 'BRAZO'),
    ('Fondos en banco', 'BRAZO'),
    ('Patada de tríceps', 'BRAZO'),
    -- Pierna
    ('Sentadilla', 'PIERNA'),
    ('Sentadilla frontal', 'PIERNA'),
    ('Sentadilla búlgara', 'PIERNA'),
    ('Prensa de pierna', 'PIERNA'),
    ('Zancadas', 'PIERNA'),
    ('Peso muerto rumano', 'PIERNA'),
    ('Extensión de cuádriceps', 'PIERNA'),
    ('Curl femoral', 'PIERNA'),
    ('Hip thrust', 'PIERNA'),
    ('Elevación de pantorrillas', 'PIERNA'),
    ('Abductores en máquina', 'PIERNA'),
    -- Core
    ('Plancha', 'CORE'),
    ('Crunch abdominal', 'CORE'),
    ('Elevación de piernas colgado', 'CORE'),
    ('Russian twist', 'CORE'),
    ('Rueda abdominal', 'CORE'),
    ('Mountain climbers', 'CORE'),
    -- CrossFit y funcional
    ('Burpees', 'FUNCIONAL'),
    ('Thrusters', 'FUNCIONAL'),
    ('Wall balls', 'FUNCIONAL'),
    ('Box jumps', 'FUNCIONAL'),
    ('Kettlebell swing', 'FUNCIONAL'),
    ('Clean and jerk', 'FUNCIONAL'),
    ('Snatch', 'FUNCIONAL'),
    ('Power clean', 'FUNCIONAL'),
    ('Toes to bar', 'FUNCIONAL'),
    ('Muscle ups', 'FUNCIONAL'),
    ('Double unders', 'FUNCIONAL'),
    ('Handstand push ups', 'FUNCIONAL'),
    ('Pistol squats', 'FUNCIONAL'),
    -- Cardio
    ('Remo (ergómetro)', 'CARDIO'),
    ('Bicicleta', 'CARDIO'),
    ('Caminadora', 'CARDIO'),
    ('Escaladora', 'CARDIO'),
    ('Saltar la cuerda', 'CARDIO'),
    ('Assault bike', 'CARDIO');
