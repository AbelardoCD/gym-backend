-- Cada rutina se arma por día de la semana (1 = lunes ... 7 = domingo).
-- Los ejercicios que ya existían quedan en el lunes.
ALTER TABLE routine_exercises ADD COLUMN day_of_week INT NOT NULL DEFAULT 1 CHECK (day_of_week BETWEEN 1 AND 7);
ALTER TABLE routine_exercises ALTER COLUMN day_of_week DROP DEFAULT;
DROP INDEX ix_routine_exercises_routine;
CREATE INDEX ix_routine_exercises_routine ON routine_exercises (routine_id, day_of_week, position);

-- Descripción opcional de cada día.
CREATE TABLE routine_days (
    routine_id  BIGINT NOT NULL REFERENCES routines (id) ON DELETE CASCADE,
    day_of_week INT    NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    description TEXT   NOT NULL,
    PRIMARY KEY (routine_id, day_of_week)
);
