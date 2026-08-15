-- Phase 2.3 Admissions (minimal approve path).

CREATE TABLE admission_applications (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id    BIGINT NOT NULL,
  application_no  VARCHAR(32) NOT NULL,
  course_id       BIGINT NOT NULL,
  applicant_name  VARCHAR(128) NOT NULL,
  phone           VARCHAR(32) NULL,
  email           VARCHAR(255) NULL,
  status          VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED',
  student_id      BIGINT NULL,
  decided_by      BIGINT NULL,
  decision_reason VARCHAR(512) NULL,
  decided_at      DATETIME(6) NULL,
  created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_application_no (institute_id, application_no),
  KEY idx_admission_institute (institute_id),
  KEY idx_admission_status (institute_id, status),
  CONSTRAINT fk_admission_institute FOREIGN KEY (institute_id) REFERENCES institutes (id)
);
