-- Phase 3.5: course subjects + faculty assigned to a batch (optional subject).

CREATE TABLE subjects (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id  BIGINT NOT NULL,
  course_id     BIGINT NOT NULL,
  code          VARCHAR(32) NOT NULL,
  name          VARCHAR(128) NOT NULL,
  status        VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_subject_code (institute_id, course_id, code),
  KEY idx_subjects_institute (institute_id),
  KEY idx_subjects_course (course_id),
  CONSTRAINT fk_subjects_institute FOREIGN KEY (institute_id) REFERENCES institutes (id),
  CONSTRAINT fk_subjects_course FOREIGN KEY (course_id) REFERENCES courses (id)
);

CREATE TABLE batch_faculty_assignments (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id  BIGINT NOT NULL,
  batch_id      BIGINT NOT NULL,
  faculty_id    BIGINT NOT NULL,
  subject_id    BIGINT NULL,
  role          VARCHAR(32) NOT NULL DEFAULT 'TEACHER',
  created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  KEY idx_bfa_institute (institute_id),
  KEY idx_bfa_batch (batch_id),
  KEY idx_bfa_faculty (institute_id, faculty_id),
  CONSTRAINT fk_bfa_institute FOREIGN KEY (institute_id) REFERENCES institutes (id),
  CONSTRAINT fk_bfa_batch FOREIGN KEY (batch_id) REFERENCES batches (id),
  CONSTRAINT fk_bfa_subject FOREIGN KEY (subject_id) REFERENCES subjects (id)
);
