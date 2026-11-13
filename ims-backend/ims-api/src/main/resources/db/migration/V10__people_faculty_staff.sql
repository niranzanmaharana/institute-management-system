-- Phase 3.1: faculties, staff (+ addresses), faculty/staff permissions.

CREATE TABLE faculties (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id  BIGINT NOT NULL,
  faculty_code  VARCHAR(32) NOT NULL,
  first_name    VARCHAR(64) NOT NULL,
  last_name     VARCHAR(64) NOT NULL,
  phone         VARCHAR(32) NULL,
  email         VARCHAR(255) NULL,
  department    VARCHAR(128) NULL,
  status        VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  deleted_at    DATETIME(6) NULL,
  UNIQUE KEY uk_faculty_code (institute_id, faculty_code),
  KEY idx_faculties_institute (institute_id),
  KEY idx_faculties_name (institute_id, last_name, first_name),
  CONSTRAINT fk_faculties_institute FOREIGN KEY (institute_id) REFERENCES institutes (id)
);

CREATE TABLE faculty_addresses (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id  BIGINT NOT NULL,
  faculty_id    BIGINT NOT NULL,
  line1         VARCHAR(255) NOT NULL,
  line2         VARCHAR(255) NULL,
  city          VARCHAR(64) NOT NULL,
  state         VARCHAR(64) NULL,
  postal_code   VARCHAR(16) NULL,
  country       VARCHAR(64) NOT NULL DEFAULT 'India',
  is_primary    TINYINT(1) NOT NULL DEFAULT 1,
  created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  KEY idx_fa_institute (institute_id),
  KEY idx_fa_faculty (faculty_id),
  CONSTRAINT fk_fa_faculty FOREIGN KEY (faculty_id) REFERENCES faculties (id),
  CONSTRAINT fk_fa_institute FOREIGN KEY (institute_id) REFERENCES institutes (id)
);

CREATE TABLE staff (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id  BIGINT NOT NULL,
  staff_code    VARCHAR(32) NOT NULL,
  first_name    VARCHAR(64) NOT NULL,
  last_name     VARCHAR(64) NOT NULL,
  phone         VARCHAR(32) NULL,
  email         VARCHAR(255) NULL,
  designation   VARCHAR(128) NULL,
  status        VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  deleted_at    DATETIME(6) NULL,
  UNIQUE KEY uk_staff_code (institute_id, staff_code),
  KEY idx_staff_institute (institute_id),
  KEY idx_staff_name (institute_id, last_name, first_name),
  CONSTRAINT fk_staff_institute FOREIGN KEY (institute_id) REFERENCES institutes (id)
);

INSERT INTO permissions (code, description) VALUES
  ('faculty:read', 'Read faculties'),
  ('faculty:write', 'Write faculties'),
  ('staff:read', 'Read staff'),
  ('staff:write', 'Write staff');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMIN' AND p.code IN ('faculty:read', 'faculty:write', 'staff:read', 'staff:write');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ACADEMIC_COORDINATOR' AND p.code IN (
  'student:read', 'student:write', 'faculty:read', 'faculty:write', 'staff:read',
  'course:read', 'course:write', 'admission:write', 'admission:approve'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'FRONT_DESK' AND p.code IN (
  'student:read', 'student:write', 'faculty:read', 'staff:read',
  'admission:write', 'course:read', 'fee:collect'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ACCOUNTANT' AND p.code IN (
  'student:read', 'faculty:read', 'staff:read', 'fee:read', 'fee:collect', 'course:read'
);
