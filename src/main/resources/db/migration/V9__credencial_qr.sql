-- Código personal para la credencial QR de cada usuario (check-in en recepción).
ALTER TABLE users ADD COLUMN qr_token VARCHAR(40);
UPDATE users SET qr_token = substr(md5(random()::text || id::text || clock_timestamp()::text), 1, 24);
ALTER TABLE users ALTER COLUMN qr_token SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT uq_users_qr_token UNIQUE (qr_token);
