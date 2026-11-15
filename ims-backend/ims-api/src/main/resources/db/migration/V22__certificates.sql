-- Phase 6.1: certificate templates + generated documents (bonafide / completion).

CREATE TABLE document_templates (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  code            VARCHAR(32) NOT NULL,
  name            VARCHAR(128) NOT NULL,
  active          TINYINT(1) NOT NULL DEFAULT 1,
  created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_document_templates_code (code)
);

INSERT INTO document_templates (code, name, active) VALUES
  ('BONAFIDE', 'Bonafide certificate', 1),
  ('COMPLETION', 'Course completion certificate', 1);

CREATE TABLE generated_documents (
  id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id        BIGINT NOT NULL,
  template_code       VARCHAR(32) NOT NULL,
  student_id          BIGINT NOT NULL,
  enrollment_id       BIGINT NULL,
  storage_key         VARCHAR(512) NOT NULL,
  file_name           VARCHAR(255) NOT NULL,
  content_type        VARCHAR(128) NOT NULL,
  file_size           BIGINT NOT NULL,
  status              VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  issued_at           DATETIME(6) NOT NULL,
  issued_by_user_id   BIGINT NULL,
  created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  KEY idx_gendoc_institute (institute_id),
  KEY idx_gendoc_student (institute_id, student_id),
  KEY idx_gendoc_template (institute_id, template_code),
  CONSTRAINT fk_gendoc_institute FOREIGN KEY (institute_id) REFERENCES institutes (id),
  CONSTRAINT fk_gendoc_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments (id)
);

INSERT INTO permissions (code, description) VALUES
  ('certificate:generate', 'Generate student certificates (bonafide, completion)'),
  ('certificate:read', 'List and download generated certificates');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'ACADEMIC_COORDINATOR') AND p.code = 'certificate:generate';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'ACADEMIC_COORDINATOR', 'FRONT_DESK', 'ACCOUNTANT')
  AND p.code = 'certificate:read';
