-- Cada rutina es para Gym o para CrossFit. Las existentes quedan como Gym.
ALTER TABLE routines
    ADD COLUMN discipline VARCHAR(20) NOT NULL DEFAULT 'GYM'
        CHECK (discipline IN ('GYM', 'CROSSFIT'));
