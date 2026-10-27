INSERT INTO permissions (code, name, description, sort_order) VALUES
    ('REPORTES', 'Reportes', 'Ver ingresos, socios y asistencia, y exportarlos a Excel', 100);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code = 'REPORTES'
WHERE r.name = 'ADMINISTRADOR';
