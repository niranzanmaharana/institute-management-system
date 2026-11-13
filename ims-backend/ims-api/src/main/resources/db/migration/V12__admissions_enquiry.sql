-- Phase 3.3: optional enquiries, application.enquiry_id, FRONT_DESK may enroll (not manage catalog).

CREATE TABLE enquiries (
  id                      BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id            BIGINT NOT NULL,
  name                    VARCHAR(128) NOT NULL,
  phone                   VARCHAR(32) NULL,
  email                   VARCHAR(255) NULL,
  interested_course_id    BIGINT NULL,
  status                  VARCHAR(32) NOT NULL DEFAULT 'OPEN',
  notes                   VARCHAR(1024) NULL,
  converted_application_id BIGINT NULL,
  created_at              DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at              DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  KEY idx_enquiry_institute (institute_id),
  KEY idx_enquiry_status (institute_id, status),
  CONSTRAINT fk_enquiry_institute FOREIGN KEY (institute_id) REFERENCES institutes (id)
);

ALTER TABLE admission_applications
  ADD COLUMN enquiry_id BIGINT NULL AFTER application_no,
  ADD KEY idx_admission_enquiry (enquiry_id),
  ADD CONSTRAINT fk_admission_enquiry FOREIGN KEY (enquiry_id) REFERENCES enquiries (id);

INSERT INTO permissions (code, description) VALUES
  ('enrollment:write', 'Create enrollments (not course catalog)');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'ACADEMIC_COORDINATOR', 'FRONT_DESK')
  AND p.code = 'enrollment:write';
