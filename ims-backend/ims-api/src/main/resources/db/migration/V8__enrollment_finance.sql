-- Phase 2.4–2.5 Enrollment + finance ledger (tenant-scoped; no cross-module FKs).

CREATE TABLE enrollments (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id    BIGINT NOT NULL,
  student_id      BIGINT NOT NULL,
  batch_id        BIGINT NOT NULL,
  course_id       BIGINT NOT NULL,
  fee_plan_id     BIGINT NOT NULL,
  status          VARCHAR(32) NOT NULL DEFAULT 'APPLIED',
  activated_at    DATETIME(6) NULL,
  created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  KEY idx_enrollments_institute (institute_id),
  KEY idx_enrollments_student (institute_id, student_id),
  KEY idx_enrollments_batch (institute_id, batch_id),
  CONSTRAINT fk_enrollments_institute FOREIGN KEY (institute_id) REFERENCES institutes (id)
);

CREATE TABLE financial_years (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id  BIGINT NOT NULL,
  name          VARCHAR(64) NOT NULL,
  start_date    DATE NOT NULL,
  end_date      DATE NOT NULL,
  is_active     TINYINT(1) NOT NULL DEFAULT 0,
  created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_fy_name (institute_id, name),
  KEY idx_fy_institute (institute_id),
  CONSTRAINT fk_fy_institute FOREIGN KEY (institute_id) REFERENCES institutes (id)
);

CREATE TABLE student_fee_accounts (
  id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id        BIGINT NOT NULL,
  student_id          BIGINT NOT NULL,
  enrollment_id       BIGINT NOT NULL,
  currency            CHAR(3) NOT NULL DEFAULT 'INR',
  status              VARCHAR(32) NOT NULL DEFAULT 'OPEN',
  outstanding_amount  DECIMAL(12,2) NOT NULL DEFAULT 0,
  version             BIGINT NOT NULL DEFAULT 0,
  created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_fee_account_enrollment (institute_id, enrollment_id),
  KEY idx_fee_accounts_institute (institute_id),
  CONSTRAINT fk_fee_accounts_institute FOREIGN KEY (institute_id) REFERENCES institutes (id)
);

CREATE TABLE invoices (
  id                BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id      BIGINT NOT NULL,
  fee_account_id    BIGINT NOT NULL,
  invoice_no        VARCHAR(32) NOT NULL,
  fee_category_id   BIGINT NOT NULL,
  financial_year_id BIGINT NOT NULL,
  description       VARCHAR(255) NOT NULL,
  amount            DECIMAL(12,2) NOT NULL,
  amount_paid       DECIMAL(12,2) NOT NULL DEFAULT 0,
  due_date          DATE NOT NULL,
  status            VARCHAR(32) NOT NULL DEFAULT 'DUE',
  installment_no    INT NULL,
  created_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_invoice_no (institute_id, invoice_no),
  KEY idx_invoices_account (fee_account_id),
  KEY idx_invoices_institute (institute_id),
  CONSTRAINT fk_invoices_institute FOREIGN KEY (institute_id) REFERENCES institutes (id),
  CONSTRAINT fk_invoices_account FOREIGN KEY (fee_account_id) REFERENCES student_fee_accounts (id)
);

CREATE TABLE payments (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id    BIGINT NOT NULL,
  fee_account_id  BIGINT NOT NULL,
  payment_no      VARCHAR(32) NOT NULL,
  amount          DECIMAL(12,2) NOT NULL,
  method          VARCHAR(32) NOT NULL DEFAULT 'CASH',
  paid_at         DATETIME(6) NOT NULL,
  idempotency_key VARCHAR(64) NULL,
  status          VARCHAR(32) NOT NULL DEFAULT 'SUCCESS',
  created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_payment_no (institute_id, payment_no),
  UNIQUE KEY uk_payment_idempotency (institute_id, idempotency_key),
  KEY idx_payments_account (fee_account_id),
  CONSTRAINT fk_payments_institute FOREIGN KEY (institute_id) REFERENCES institutes (id),
  CONSTRAINT fk_payments_account FOREIGN KEY (fee_account_id) REFERENCES student_fee_accounts (id)
);

CREATE TABLE payment_allocations (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id  BIGINT NOT NULL,
  payment_id    BIGINT NOT NULL,
  invoice_id    BIGINT NOT NULL,
  amount        DECIMAL(12,2) NOT NULL,
  created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  KEY idx_alloc_payment (payment_id),
  KEY idx_alloc_invoice (invoice_id),
  CONSTRAINT fk_alloc_institute FOREIGN KEY (institute_id) REFERENCES institutes (id),
  CONSTRAINT fk_alloc_payment FOREIGN KEY (payment_id) REFERENCES payments (id),
  CONSTRAINT fk_alloc_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id)
);

CREATE TABLE receipts (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id  BIGINT NOT NULL,
  payment_id    BIGINT NOT NULL,
  receipt_no    VARCHAR(32) NOT NULL,
  issued_at     DATETIME(6) NOT NULL,
  created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_receipt_no (institute_id, receipt_no),
  UNIQUE KEY uk_receipt_payment (institute_id, payment_id),
  CONSTRAINT fk_receipts_institute FOREIGN KEY (institute_id) REFERENCES institutes (id),
  CONSTRAINT fk_receipts_payment FOREIGN KEY (payment_id) REFERENCES payments (id)
);
