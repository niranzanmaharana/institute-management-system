-- Step 7.1: configurable account lockout after failed logins (doc 08).
ALTER TABLE users
  ADD COLUMN failed_login_count INT NOT NULL DEFAULT 0 AFTER last_login_at,
  ADD COLUMN locked_until DATETIME(6) NULL AFTER failed_login_count;
