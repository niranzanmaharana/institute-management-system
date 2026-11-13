-- Phase 4.2: refund lifecycle (request → approve/reject → process complete/fail).

CREATE TABLE refund_requests (
  id                      BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id            BIGINT NOT NULL,
  fee_account_id          BIGINT NOT NULL,
  amount                  DECIMAL(12,2) NOT NULL,
  status                  VARCHAR(32) NOT NULL,
  reason                  VARCHAR(512) NOT NULL,
  requested_by            BIGINT NULL,
  decided_by              BIGINT NULL,
  processed_at            DATETIME(6) NULL,
  process_note            VARCHAR(512) NULL,
  process_idempotency_key VARCHAR(64) NULL,
  created_at              DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at              DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  KEY idx_refund_institute (institute_id),
  KEY idx_refund_account (fee_account_id),
  KEY idx_refund_status (institute_id, status),
  UNIQUE KEY uk_refund_process_key (institute_id, process_idempotency_key),
  CONSTRAINT fk_refund_institute FOREIGN KEY (institute_id) REFERENCES institutes (id),
  CONSTRAINT fk_refund_account FOREIGN KEY (fee_account_id) REFERENCES student_fee_accounts (id)
);

INSERT INTO permissions (code, description) VALUES
  ('fee:refund', 'Request, approve, reject, and process fee refunds');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'ACCOUNTANT') AND p.code = 'fee:refund';
