-- Phase 2.1 People: students, guardians, addresses (tenant-scoped).

CREATE TABLE students (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id  BIGINT NOT NULL,
  student_code  VARCHAR(32) NOT NULL,
  first_name    VARCHAR(64) NOT NULL,
  last_name     VARCHAR(64) NOT NULL,
  phone         VARCHAR(32) NULL,
  email         VARCHAR(255) NULL,
  status        VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  deleted_at    DATETIME(6) NULL,
  UNIQUE KEY uk_student_code (institute_id, student_code),
  KEY idx_students_institute (institute_id),
  KEY idx_students_institute_name (institute_id, last_name, first_name),
  CONSTRAINT fk_students_institute FOREIGN KEY (institute_id) REFERENCES institutes (id)
);

CREATE TABLE guardians (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id  BIGINT NOT NULL,
  name          VARCHAR(128) NOT NULL,
  phone         VARCHAR(32) NULL,
  email         VARCHAR(255) NULL,
  created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  deleted_at    DATETIME(6) NULL,
  UNIQUE KEY uk_guardian_phone (institute_id, phone),
  UNIQUE KEY uk_guardian_email (institute_id, email),
  KEY idx_guardians_institute (institute_id),
  CONSTRAINT fk_guardians_institute FOREIGN KEY (institute_id) REFERENCES institutes (id)
);

CREATE TABLE student_guardians (
  institute_id  BIGINT NOT NULL,
  student_id    BIGINT NOT NULL,
  guardian_id   BIGINT NOT NULL,
  relation      VARCHAR(64) NOT NULL,
  is_primary    TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (student_id, guardian_id),
  KEY idx_sg_institute (institute_id),
  KEY idx_sg_guardian (guardian_id),
  CONSTRAINT fk_sg_student FOREIGN KEY (student_id) REFERENCES students (id),
  CONSTRAINT fk_sg_guardian FOREIGN KEY (guardian_id) REFERENCES guardians (id),
  CONSTRAINT fk_sg_institute FOREIGN KEY (institute_id) REFERENCES institutes (id)
);

CREATE TABLE student_addresses (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id  BIGINT NOT NULL,
  student_id    BIGINT NOT NULL,
  line1         VARCHAR(255) NOT NULL,
  line2         VARCHAR(255) NULL,
  city          VARCHAR(64) NOT NULL,
  state         VARCHAR(64) NULL,
  postal_code   VARCHAR(16) NULL,
  country       VARCHAR(64) NOT NULL DEFAULT 'India',
  is_primary    TINYINT(1) NOT NULL DEFAULT 1,
  created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  KEY idx_sa_institute (institute_id),
  KEY idx_sa_student (student_id),
  CONSTRAINT fk_sa_student FOREIGN KEY (student_id) REFERENCES students (id),
  CONSTRAINT fk_sa_institute FOREIGN KEY (institute_id) REFERENCES institutes (id)
);
