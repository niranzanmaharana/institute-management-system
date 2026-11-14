-- Phase 5.3: exams, components, results, component marks.

CREATE TABLE exams (
  id                BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id      BIGINT NOT NULL,
  batch_id          BIGINT NOT NULL,
  name              VARCHAR(128) NOT NULL,
  exam_date         DATE NOT NULL,
  status            VARCHAR(32) NOT NULL DEFAULT 'SCHEDULED',
  original_exam_id  BIGINT NULL,
  created_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  published_at      DATETIME(6) NULL,
  locked_at         DATETIME(6) NULL,
  KEY idx_exams_institute (institute_id),
  KEY idx_exams_batch (institute_id, batch_id),
  CONSTRAINT fk_exams_institute FOREIGN KEY (institute_id) REFERENCES institutes (id),
  CONSTRAINT fk_exams_batch FOREIGN KEY (batch_id) REFERENCES batches (id),
  CONSTRAINT fk_exams_original FOREIGN KEY (original_exam_id) REFERENCES exams (id)
);

CREATE TABLE exam_components (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id    BIGINT NOT NULL,
  exam_id         BIGINT NOT NULL,
  subject_id      BIGINT NULL,
  code            VARCHAR(32) NOT NULL,
  name            VARCHAR(128) NOT NULL,
  max_marks       DECIMAL(8,2) NOT NULL,
  pass_marks      DECIMAL(8,2) NOT NULL,
  weightage       DECIMAL(5,2) NULL,
  seq             INT NOT NULL DEFAULT 1,
  created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_exam_component_code (exam_id, code),
  KEY idx_exam_comp_institute (institute_id),
  CONSTRAINT fk_exam_comp_institute FOREIGN KEY (institute_id) REFERENCES institutes (id),
  CONSTRAINT fk_exam_comp_exam FOREIGN KEY (exam_id) REFERENCES exams (id),
  CONSTRAINT fk_exam_comp_subject FOREIGN KEY (subject_id) REFERENCES subjects (id)
);

CREATE TABLE exam_results (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id    BIGINT NOT NULL,
  exam_id         BIGINT NOT NULL,
  student_id      BIGINT NOT NULL,
  result_status   VARCHAR(32) NOT NULL DEFAULT 'MARKS',
  total_marks     DECIMAL(8,2) NULL,
  grace_marks     DECIMAL(8,2) NULL,
  pass_fail       VARCHAR(16) NULL,
  grade           VARCHAR(8) NULL,
  remarks         VARCHAR(255) NULL,
  created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_exam_student (institute_id, exam_id, student_id),
  KEY idx_exam_res_student (institute_id, student_id),
  CONSTRAINT fk_exam_res_institute FOREIGN KEY (institute_id) REFERENCES institutes (id),
  CONSTRAINT fk_exam_res_exam FOREIGN KEY (exam_id) REFERENCES exams (id)
);

CREATE TABLE exam_component_marks (
  id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id        BIGINT NOT NULL,
  exam_result_id      BIGINT NOT NULL,
  exam_component_id   BIGINT NOT NULL,
  marks               DECIMAL(8,2) NULL,
  created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_result_component (exam_result_id, exam_component_id),
  KEY idx_exam_cm_institute (institute_id),
  CONSTRAINT fk_exam_cm_institute FOREIGN KEY (institute_id) REFERENCES institutes (id),
  CONSTRAINT fk_exam_cm_result FOREIGN KEY (exam_result_id) REFERENCES exam_results (id),
  CONSTRAINT fk_exam_cm_component FOREIGN KEY (exam_component_id) REFERENCES exam_components (id)
);

INSERT INTO permissions (code, description) VALUES
  ('exam:mark', 'Enter and edit exam marks (draft)'),
  ('exam:publish', 'Publish, lock, and unlock exam results');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'ACADEMIC_COORDINATOR', 'FACULTY') AND p.code = 'exam:mark';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'ACADEMIC_COORDINATOR') AND p.code = 'exam:publish';
