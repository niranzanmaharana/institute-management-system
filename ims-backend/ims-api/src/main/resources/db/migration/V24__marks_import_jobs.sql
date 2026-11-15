-- Phase 6.3: async-friendly bulk marks import jobs (idempotent per exam+student via upsert).

CREATE TABLE import_jobs (
  id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id        BIGINT NOT NULL,
  job_type            VARCHAR(32) NOT NULL,
  exam_id             BIGINT NULL,
  status              VARCHAR(32) NOT NULL DEFAULT 'QUEUED',
  file_name           VARCHAR(255) NULL,
  total_rows          INT NOT NULL DEFAULT 0,
  success_count       INT NOT NULL DEFAULT 0,
  failed_count        INT NOT NULL DEFAULT 0,
  error_report_csv    MEDIUMTEXT NULL,
  message             VARCHAR(512) NULL,
  payload_csv         MEDIUMTEXT NULL,
  created_by_user_id  BIGINT NULL,
  created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  started_at          DATETIME(6) NULL,
  completed_at        DATETIME(6) NULL,
  KEY idx_import_jobs_institute (institute_id),
  KEY idx_import_jobs_exam (institute_id, exam_id),
  KEY idx_import_jobs_status (institute_id, status),
  CONSTRAINT fk_import_jobs_institute FOREIGN KEY (institute_id) REFERENCES institutes (id),
  CONSTRAINT fk_import_jobs_exam FOREIGN KEY (exam_id) REFERENCES exams (id)
);
