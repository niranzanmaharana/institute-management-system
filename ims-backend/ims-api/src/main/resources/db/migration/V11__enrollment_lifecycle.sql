-- Phase 3.2: enrollment lifecycle timestamps (status values remain VARCHAR).

ALTER TABLE enrollments
  ADD COLUMN suspended_at  DATETIME(6) NULL AFTER activated_at,
  ADD COLUMN withdrawn_at  DATETIME(6) NULL AFTER suspended_at,
  ADD COLUMN completed_at  DATETIME(6) NULL AFTER withdrawn_at,
  ADD COLUMN cancelled_at  DATETIME(6) NULL AFTER completed_at;
