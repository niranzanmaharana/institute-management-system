-- Phase 4.3: payment reversal — cancel receipt status; fee:reverse permission.

ALTER TABLE receipts
  ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'ISSUED' AFTER issued_at,
  ADD COLUMN cancelled_at DATETIME(6) NULL AFTER status;

ALTER TABLE payments
  ADD COLUMN reverse_reason VARCHAR(512) NULL AFTER status,
  ADD COLUMN reversed_at DATETIME(6) NULL AFTER reverse_reason,
  ADD COLUMN reversed_by BIGINT NULL AFTER reversed_at;

INSERT INTO permissions (code, description) VALUES
  ('fee:reverse', 'Reverse mistaken successful payments');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'ACCOUNTANT') AND p.code = 'fee:reverse';
