-- Phase 2.2 Academic catalog: years, fee categories, courses, fee plans, batches (tenant-scoped).

CREATE TABLE academic_years (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id  BIGINT NOT NULL,
  code          VARCHAR(32) NOT NULL,
  name          VARCHAR(128) NOT NULL,
  start_date    DATE NOT NULL,
  end_date      DATE NOT NULL,
  status        VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_academic_year_code (institute_id, code),
  KEY idx_academic_years_institute (institute_id),
  CONSTRAINT fk_academic_years_institute FOREIGN KEY (institute_id) REFERENCES institutes (id)
);

CREATE TABLE fee_categories (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id  BIGINT NOT NULL,
  code          VARCHAR(32) NOT NULL,
  name          VARCHAR(128) NOT NULL,
  created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_fee_category_code (institute_id, code),
  KEY idx_fee_categories_institute (institute_id),
  CONSTRAINT fk_fee_categories_institute FOREIGN KEY (institute_id) REFERENCES institutes (id)
);

CREATE TABLE courses (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id  BIGINT NOT NULL,
  code          VARCHAR(32) NOT NULL,
  name          VARCHAR(255) NOT NULL,
  description   VARCHAR(512) NULL,
  status        VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  deleted_at    DATETIME(6) NULL,
  UNIQUE KEY uk_course_code (institute_id, code),
  KEY idx_courses_institute (institute_id),
  CONSTRAINT fk_courses_institute FOREIGN KEY (institute_id) REFERENCES institutes (id)
);

CREATE TABLE course_fee_plans (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id  BIGINT NOT NULL,
  course_id     BIGINT NOT NULL,
  code          VARCHAR(32) NOT NULL,
  name          VARCHAR(128) NOT NULL,
  currency      CHAR(3) NOT NULL DEFAULT 'INR',
  total_amount  DECIMAL(12,2) NOT NULL,
  status        VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_fee_plan_code (institute_id, code),
  KEY idx_fee_plans_institute (institute_id),
  KEY idx_fee_plans_course (course_id),
  CONSTRAINT fk_fee_plans_institute FOREIGN KEY (institute_id) REFERENCES institutes (id),
  CONSTRAINT fk_fee_plans_course FOREIGN KEY (course_id) REFERENCES courses (id)
);

CREATE TABLE course_fee_installments (
  id                BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id      BIGINT NOT NULL,
  fee_plan_id       BIGINT NOT NULL,
  seq               INT NOT NULL,
  fee_category_id   BIGINT NOT NULL,
  label             VARCHAR(128) NOT NULL,
  amount            DECIMAL(12,2) NOT NULL,
  due_offset_days   INT NOT NULL DEFAULT 0,
  created_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_installment_seq (fee_plan_id, seq),
  KEY idx_installments_institute (institute_id),
  CONSTRAINT fk_installments_institute FOREIGN KEY (institute_id) REFERENCES institutes (id),
  CONSTRAINT fk_installments_plan FOREIGN KEY (fee_plan_id) REFERENCES course_fee_plans (id),
  CONSTRAINT fk_installments_category FOREIGN KEY (fee_category_id) REFERENCES fee_categories (id)
);

CREATE TABLE batches (
  id                BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id      BIGINT NOT NULL,
  course_id         BIGINT NOT NULL,
  academic_year_id  BIGINT NOT NULL,
  code              VARCHAR(32) NOT NULL,
  name              VARCHAR(128) NOT NULL,
  capacity          INT NOT NULL,
  status            VARCHAR(32) NOT NULL DEFAULT 'OPEN',
  start_date        DATE NULL,
  end_date          DATE NULL,
  created_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_batch_code (institute_id, code),
  KEY idx_batches_institute (institute_id),
  KEY idx_batches_course (course_id),
  CONSTRAINT fk_batches_institute FOREIGN KEY (institute_id) REFERENCES institutes (id),
  CONSTRAINT fk_batches_course FOREIGN KEY (course_id) REFERENCES courses (id),
  CONSTRAINT fk_batches_year FOREIGN KEY (academic_year_id) REFERENCES academic_years (id)
);
