-- Identity schema (Phase 1). Passwords seeded at runtime via IdentityDataLoader.

CREATE TABLE roles (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  code        VARCHAR(64) NOT NULL,
  name        VARCHAR(128) NOT NULL,
  description VARCHAR(512) NULL,
  created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_roles_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE permissions (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  code        VARCHAR(128) NOT NULL,
  description VARCHAR(512) NULL,
  UNIQUE KEY uk_permissions_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE role_permissions (
  role_id       BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  PRIMARY KEY (role_id, permission_id),
  CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES roles (id),
  CONSTRAINT fk_rp_perm FOREIGN KEY (permission_id) REFERENCES permissions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE users (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id    BIGINT NULL,
  username        VARCHAR(64) NOT NULL,
  email           VARCHAR(255) NOT NULL,
  password_hash   VARCHAR(255) NOT NULL,
  status          VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  person_type     VARCHAR(32) NOT NULL DEFAULT 'NONE',
  person_id       BIGINT NULL,
  last_login_at   DATETIME(6) NULL,
  created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  deleted_at      DATETIME(6) NULL,
  UNIQUE KEY uk_users_institute_username (institute_id, username),
  UNIQUE KEY uk_users_institute_email (institute_id, email),
  KEY idx_users_institute (institute_id),
  CONSTRAINT fk_users_institute FOREIGN KEY (institute_id) REFERENCES institutes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_roles (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE refresh_tokens (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id     BIGINT NOT NULL,
  token_hash  VARCHAR(255) NOT NULL,
  expires_at  DATETIME(6) NOT NULL,
  revoked_at  DATETIME(6) NULL,
  created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_refresh_token_hash (token_hash),
  KEY idx_refresh_user (user_id),
  CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE login_audit (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id    BIGINT NULL,
  username   VARCHAR(64) NULL,
  success    TINYINT(1) NOT NULL,
  ip_address VARCHAR(64) NULL,
  user_agent VARCHAR(512) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  KEY idx_login_audit_user (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO roles (code, name, description) VALUES
  ('PLATFORM_ADMIN', 'Platform Admin', 'Create and suspend institutes'),
  ('ADMIN', 'Institute Admin', 'Full institute administration'),
  ('ACCOUNTANT', 'Accountant', 'Fees and payments'),
  ('ACADEMIC_COORDINATOR', 'Academic Coordinator', 'Courses and exams'),
  ('FACULTY', 'Faculty', 'Teaching and attendance'),
  ('FRONT_DESK', 'Front Desk', 'Admissions and registration'),
  ('STUDENT', 'Student', 'Self-service access');

INSERT INTO permissions (code, description) VALUES
  ('institute:manage', 'Create/suspend institutes'),
  ('user:manage', 'Manage institute users'),
  ('student:read', 'Read students'),
  ('student:write', 'Write students'),
  ('fee:read', 'Read fees'),
  ('fee:collect', 'Collect payments'),
  ('course:read', 'Read courses'),
  ('course:write', 'Write courses'),
  ('admission:write', 'Create admissions'),
  ('admission:approve', 'Approve admissions');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.code = 'PLATFORM_ADMIN' AND p.code = 'institute:manage';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMIN' AND p.code IN (
  'user:manage','student:read','student:write','fee:read','fee:collect','course:read','course:write','admission:write','admission:approve'
);
