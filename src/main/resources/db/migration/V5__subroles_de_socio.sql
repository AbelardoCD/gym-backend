-- Subroles del socio: Gym, CrossFit o ambos.
CREATE TABLE user_disciplines (
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    discipline VARCHAR(20) NOT NULL CHECK (discipline IN ('GYM', 'CROSSFIT')),
    PRIMARY KEY (user_id, discipline)
);

-- Los socios que ya existían quedan como Gym.
INSERT INTO user_disciplines (user_id, discipline)
SELECT ur.user_id, 'GYM'
FROM user_roles ur
JOIN roles r ON r.id = ur.role_id
WHERE r.name = 'SOCIO';
