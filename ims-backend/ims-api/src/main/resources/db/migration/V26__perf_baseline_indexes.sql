-- Step 7.3: indexes for outstanding list and student list/search filters.

CREATE INDEX idx_fee_accounts_outstanding
  ON student_fee_accounts (institute_id, status, outstanding_amount);

CREATE INDEX idx_students_status_deleted
  ON students (institute_id, status, deleted_at);

-- Login lookup already covered by uk_users_institute_username (institute_id, username).
