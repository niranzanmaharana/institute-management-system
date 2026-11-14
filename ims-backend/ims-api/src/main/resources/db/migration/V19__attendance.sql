-- Phase 5.1: attendance sessions + records; course min attendance threshold.

ALTER TABLE courses
  ADD COLUMN min_attendance_percent DECIMAL(5,2) NULL AFTER status;

CREATE TABLE attendance_sessions (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id    BIGINT NOT NULL,
  batch_id        BIGINT NOT NULL,
  session_date    DATE NOT NULL,
  subject_id      BIGINT NULL,
  subject_key     BIGINT GENERATED ALWAYS AS (IFNULL(subject_id, 0)) STORED,
  status          VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  marked_by       BIGINT NULL,
  locked_at       DATETIME(6) NULL,
  unlocked_at     DATETIME(6) NULL,
  created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_batch_session (institute_id, batch_id, session_date, subject_key),
  KEY idx_att_sess_institute (institute_id),
  KEY idx_att_sess_batch_date (institute_id, batch_id, session_date),
  CONSTRAINT fk_att_sess_institute FOREIGN KEY (institute_id) REFERENCES institutes (id),
  CONSTRAINT fk_att_sess_batch FOREIGN KEY (batch_id) REFERENCES batches (id),
  CONSTRAINT fk_att_sess_subject FOREIGN KEY (subject_id) REFERENCES subjects (id)
);

CREATE TABLE attendance_records (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id    BIGINT NOT NULL,
  session_id      BIGINT NOT NULL,
  student_id      BIGINT NOT NULL,
  status          VARCHAR(32) NOT NULL,
  note            VARCHAR(255) NULL,
  created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_session_student (session_id, student_id),
  KEY idx_att_rec_institute (institute_id),
  KEY idx_att_rec_student (institute_id, student_id),
  CONSTRAINT fk_att_rec_institute FOREIGN KEY (institute_id) REFERENCES institutes (id),
  CONSTRAINT fk_att_rec_session FOREIGN KEY (session_id) REFERENCES attendance_sessions (id)
);

INSERT INTO permissions (code, description) VALUES
  ('attendance:mark', 'Create and mark attendance sessions'),
  ('attendance:unlock', 'Unlock locked attendance sessions');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'FACULTY') AND p.code = 'attendance:mark';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'ACADEMIC_COORDINATOR') AND p.code = 'attendance:unlock';
