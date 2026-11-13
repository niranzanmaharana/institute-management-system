-- Phase 4.1: ledger adjustments (discount / scholarship / waiver / penalty).

CREATE TABLE fee_adjustments (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id    BIGINT NOT NULL,
  fee_account_id  BIGINT NOT NULL,
  type            VARCHAR(32) NOT NULL,
  amount          DECIMAL(12,2) NOT NULL,
  reason          VARCHAR(512) NOT NULL,
  approved_by     BIGINT NULL,
  created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  KEY idx_fee_adj_institute (institute_id),
  KEY idx_fee_adj_account (fee_account_id),
  CONSTRAINT fk_fee_adj_institute FOREIGN KEY (institute_id) REFERENCES institutes (id),
  CONSTRAINT fk_fee_adj_account FOREIGN KEY (fee_account_id) REFERENCES student_fee_accounts (id)
);

INSERT INTO permissions (code, description) VALUES
  ('fee:adjust', 'Create fee adjustments (discount, scholarship, waiver, penalty)');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'ACCOUNTANT') AND p.code = 'fee:adjust';
