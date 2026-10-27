-- Más ejercicios comunes en máquina y polea. Si el gimnasio ya dio de alta alguno con el mismo nombre, se respeta el suyo.
INSERT INTO exercises (name, muscle_group) VALUES
    ('Aperturas en máquina (pec deck)', 'PECHO'),
    ('Press de pecho en máquina', 'PECHO'),
    ('Remo en máquina', 'ESPALDA'),
    ('Jalón con agarre cerrado', 'ESPALDA'),
    ('Press de hombro en máquina', 'HOMBRO'),
    ('Elevaciones laterales en polea', 'HOMBRO'),
    ('Extensión de tríceps sobre la cabeza', 'BRAZO'),
    ('Curl en polea', 'BRAZO'),
    ('Sentadilla hack', 'PIERNA'),
    ('Aductores en máquina', 'PIERNA'),
    ('Elevación de pantorrillas sentado', 'PIERNA')
ON CONFLICT DO NOTHING;
