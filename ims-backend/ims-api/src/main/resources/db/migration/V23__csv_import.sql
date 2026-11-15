-- Phase 6.2: CSV import permission for bulk student/faculty onboarding.

INSERT INTO permissions (code, description) VALUES
  ('import:write', 'Import students/faculty via CSV (validate then insert)');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'ACADEMIC_COORDINATOR') AND p.code = 'import:write';
