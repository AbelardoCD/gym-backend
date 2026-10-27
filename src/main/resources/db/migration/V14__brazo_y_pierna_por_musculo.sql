-- Brazo se divide en bíceps, tríceps y antebrazo; pierna en cuádriceps, femoral, glúteo y pantorrilla.
UPDATE exercises SET muscle_group = 'TRICEP' WHERE muscle_group = 'BRAZO' AND name IN (
    'Extensión de tríceps en polea', 'Press francés', 'Fondos en banco', 'Patada de tríceps', 'Extensión de tríceps sobre la cabeza');
UPDATE exercises SET muscle_group = 'FEMORAL' WHERE muscle_group = 'PIERNA' AND name IN ('Peso muerto rumano', 'Curl femoral');
UPDATE exercises SET muscle_group = 'GLUTEO' WHERE muscle_group = 'PIERNA' AND name IN ('Hip thrust', 'Abductores en máquina', 'Aductores en máquina');
UPDATE exercises SET muscle_group = 'PANTORRILLA' WHERE muscle_group = 'PIERNA' AND name IN ('Elevación de pantorrillas', 'Elevación de pantorrillas sentado');
-- Los demás (y los que haya dado de alta el gimnasio) quedan en bíceps y cuádriceps; se pueden cambiar en Ejercicios.
UPDATE exercises SET muscle_group = 'BICEP' WHERE muscle_group = 'BRAZO';
UPDATE exercises SET muscle_group = 'CUADRICEPS' WHERE muscle_group = 'PIERNA';

INSERT INTO exercises (name, muscle_group) VALUES
    ('Curl concentrado', 'BICEP'),
    ('Fondos en máquina', 'TRICEP'),
    ('Curl de muñeca', 'ANTEBRAZO'),
    ('Curl de muñeca inverso', 'ANTEBRAZO'),
    ('Curl inverso con barra', 'ANTEBRAZO'),
    ('Paseo del granjero', 'ANTEBRAZO'),
    ('Curl femoral sentado', 'FEMORAL'),
    ('Peso muerto con piernas rígidas', 'FEMORAL'),
    ('Puente de glúteo', 'GLUTEO'),
    ('Patada de glúteo en polea', 'GLUTEO'),
    ('Elevación de pantorrillas en prensa', 'PANTORRILLA')
ON CONFLICT DO NOTHING;
