-- Phase 3.4: private person document metadata + document permissions.

CREATE TABLE person_documents (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id  BIGINT NOT NULL,
  owner_type    VARCHAR(16) NOT NULL,
  owner_id      BIGINT NOT NULL,
  doc_type      VARCHAR(64) NOT NULL,
  file_name     VARCHAR(255) NOT NULL,
  storage_key   VARCHAR(512) NOT NULL,
  content_type  VARCHAR(128) NOT NULL,
  file_size     BIGINT NOT NULL,
  checksum      VARCHAR(128) NULL,
  status        VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  uploaded_by   BIGINT NULL,
  uploaded_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_person_docs_storage_key (storage_key),
  KEY idx_person_docs_institute (institute_id),
  KEY idx_person_docs_owner (institute_id, owner_type, owner_id, status),
  CONSTRAINT fk_person_docs_institute FOREIGN KEY (institute_id) REFERENCES institutes (id)
);

INSERT INTO permissions (code, description) VALUES
  ('document:read', 'Read person documents'),
  ('document:write', 'Upload and delete person documents');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMIN' AND p.code IN ('document:read', 'document:write');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'FRONT_DESK' AND p.code IN ('document:read', 'document:write');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('ACADEMIC_COORDINATOR', 'ACCOUNTANT') AND p.code = 'document:read';
