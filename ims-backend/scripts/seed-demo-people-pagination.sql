-- Manual seed (NOT a Flyway migration).
-- Purpose: 50 students + 50 faculties for DEMO_A so list pagination is easy to demo.
-- Run against your MySQL database, e.g.:
--   mysql -u <user> -p <database> < ims-api/scripts/seed-demo-people-pagination.sql
--
-- Safe to re-run: deletes prior sample rows (emails *@demo.ims.local) first.
-- Uses SAMPLE-* codes so they do not collide with auto-generated business codes.

SET @inst := (SELECT id FROM institutes WHERE code = 'DEMO_A' LIMIT 1);
SELECT IF(@inst IS NULL, CAST('DEMO_A institute not found' AS UNSIGNED), @inst) AS institute_id;

DELETE FROM students
WHERE institute_id = @inst AND email LIKE 'sample.student%@demo.ims.local';

DELETE FROM faculties
WHERE institute_id = @inst AND email LIKE 'sample.faculty%@demo.ims.local';

-- Students 1..50; last 5 inactive for status-filter demo.
INSERT INTO students (institute_id, student_code, first_name, last_name, phone, email, status, deleted_at)
VALUES
  (@inst, 'SAMPLE-S-001', 'Sample01', 'Student', '9000000001', 'sample.student01@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-002', 'Sample02', 'Student', '9000000002', 'sample.student02@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-003', 'Sample03', 'Student', '9000000003', 'sample.student03@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-004', 'Sample04', 'Student', '9000000004', 'sample.student04@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-005', 'Sample05', 'Student', '9000000005', 'sample.student05@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-006', 'Sample06', 'Student', '9000000006', 'sample.student06@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-007', 'Sample07', 'Student', '9000000007', 'sample.student07@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-008', 'Sample08', 'Student', '9000000008', 'sample.student08@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-009', 'Sample09', 'Student', '9000000009', 'sample.student09@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-010', 'Sample10', 'Student', '9000000010', 'sample.student10@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-011', 'Sample11', 'Student', '9000000011', 'sample.student11@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-012', 'Sample12', 'Student', '9000000012', 'sample.student12@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-013', 'Sample13', 'Student', '9000000013', 'sample.student13@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-014', 'Sample14', 'Student', '9000000014', 'sample.student14@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-015', 'Sample15', 'Student', '9000000015', 'sample.student15@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-016', 'Sample16', 'Student', '9000000016', 'sample.student16@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-017', 'Sample17', 'Student', '9000000017', 'sample.student17@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-018', 'Sample18', 'Student', '9000000018', 'sample.student18@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-019', 'Sample19', 'Student', '9000000019', 'sample.student19@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-020', 'Sample20', 'Student', '9000000020', 'sample.student20@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-021', 'Sample21', 'Student', '9000000021', 'sample.student21@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-022', 'Sample22', 'Student', '9000000022', 'sample.student22@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-023', 'Sample23', 'Student', '9000000023', 'sample.student23@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-024', 'Sample24', 'Student', '9000000024', 'sample.student24@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-025', 'Sample25', 'Student', '9000000025', 'sample.student25@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-026', 'Sample26', 'Student', '9000000026', 'sample.student26@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-027', 'Sample27', 'Student', '9000000027', 'sample.student27@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-028', 'Sample28', 'Student', '9000000028', 'sample.student28@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-029', 'Sample29', 'Student', '9000000029', 'sample.student29@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-030', 'Sample30', 'Student', '9000000030', 'sample.student30@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-031', 'Sample31', 'Student', '9000000031', 'sample.student31@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-032', 'Sample32', 'Student', '9000000032', 'sample.student32@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-033', 'Sample33', 'Student', '9000000033', 'sample.student33@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-034', 'Sample34', 'Student', '9000000034', 'sample.student34@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-035', 'Sample35', 'Student', '9000000035', 'sample.student35@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-036', 'Sample36', 'Student', '9000000036', 'sample.student36@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-037', 'Sample37', 'Student', '9000000037', 'sample.student37@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-038', 'Sample38', 'Student', '9000000038', 'sample.student38@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-039', 'Sample39', 'Student', '9000000039', 'sample.student39@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-040', 'Sample40', 'Student', '9000000040', 'sample.student40@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-041', 'Sample41', 'Student', '9000000041', 'sample.student41@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-042', 'Sample42', 'Student', '9000000042', 'sample.student42@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-043', 'Sample43', 'Student', '9000000043', 'sample.student43@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-044', 'Sample44', 'Student', '9000000044', 'sample.student44@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-045', 'Sample45', 'Student', '9000000045', 'sample.student45@demo.ims.local', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-S-046', 'Sample46', 'Student', '9000000046', 'sample.student46@demo.ims.local', 'INACTIVE', UTC_TIMESTAMP(6)),
  (@inst, 'SAMPLE-S-047', 'Sample47', 'Student', '9000000047', 'sample.student47@demo.ims.local', 'INACTIVE', UTC_TIMESTAMP(6)),
  (@inst, 'SAMPLE-S-048', 'Sample48', 'Student', '9000000048', 'sample.student48@demo.ims.local', 'INACTIVE', UTC_TIMESTAMP(6)),
  (@inst, 'SAMPLE-S-049', 'Sample49', 'Student', '9000000049', 'sample.student49@demo.ims.local', 'INACTIVE', UTC_TIMESTAMP(6)),
  (@inst, 'SAMPLE-S-050', 'Sample50', 'Student', '9000000050', 'sample.student50@demo.ims.local', 'INACTIVE', UTC_TIMESTAMP(6));

-- Faculties 1..50; last 5 inactive.
INSERT INTO faculties (institute_id, faculty_code, first_name, last_name, phone, email, department, status, deleted_at)
VALUES
  (@inst, 'SAMPLE-F-001', 'Sample01', 'Faculty', '8000000001', 'sample.faculty01@demo.ims.local', 'Mathematics', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-002', 'Sample02', 'Faculty', '8000000002', 'sample.faculty02@demo.ims.local', 'Physics', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-003', 'Sample03', 'Faculty', '8000000003', 'sample.faculty03@demo.ims.local', 'Chemistry', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-004', 'Sample04', 'Faculty', '8000000004', 'sample.faculty04@demo.ims.local', 'English', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-005', 'Sample05', 'Faculty', '8000000005', 'sample.faculty05@demo.ims.local', 'Computer Science', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-006', 'Sample06', 'Faculty', '8000000006', 'sample.faculty06@demo.ims.local', 'Commerce', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-007', 'Sample07', 'Faculty', '8000000007', 'sample.faculty07@demo.ims.local', 'Biology', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-008', 'Sample08', 'Faculty', '8000000008', 'sample.faculty08@demo.ims.local', 'History', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-009', 'Sample09', 'Faculty', '8000000009', 'sample.faculty09@demo.ims.local', 'Mathematics', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-010', 'Sample10', 'Faculty', '8000000010', 'sample.faculty10@demo.ims.local', 'Physics', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-011', 'Sample11', 'Faculty', '8000000011', 'sample.faculty11@demo.ims.local', 'Chemistry', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-012', 'Sample12', 'Faculty', '8000000012', 'sample.faculty12@demo.ims.local', 'English', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-013', 'Sample13', 'Faculty', '8000000013', 'sample.faculty13@demo.ims.local', 'Computer Science', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-014', 'Sample14', 'Faculty', '8000000014', 'sample.faculty14@demo.ims.local', 'Commerce', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-015', 'Sample15', 'Faculty', '8000000015', 'sample.faculty15@demo.ims.local', 'Biology', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-016', 'Sample16', 'Faculty', '8000000016', 'sample.faculty16@demo.ims.local', 'History', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-017', 'Sample17', 'Faculty', '8000000017', 'sample.faculty17@demo.ims.local', 'Mathematics', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-018', 'Sample18', 'Faculty', '8000000018', 'sample.faculty18@demo.ims.local', 'Physics', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-019', 'Sample19', 'Faculty', '8000000019', 'sample.faculty19@demo.ims.local', 'Chemistry', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-020', 'Sample20', 'Faculty', '8000000020', 'sample.faculty20@demo.ims.local', 'English', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-021', 'Sample21', 'Faculty', '8000000021', 'sample.faculty21@demo.ims.local', 'Computer Science', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-022', 'Sample22', 'Faculty', '8000000022', 'sample.faculty22@demo.ims.local', 'Commerce', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-023', 'Sample23', 'Faculty', '8000000023', 'sample.faculty23@demo.ims.local', 'Biology', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-024', 'Sample24', 'Faculty', '8000000024', 'sample.faculty24@demo.ims.local', 'History', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-025', 'Sample25', 'Faculty', '8000000025', 'sample.faculty25@demo.ims.local', 'Mathematics', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-026', 'Sample26', 'Faculty', '8000000026', 'sample.faculty26@demo.ims.local', 'Physics', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-027', 'Sample27', 'Faculty', '8000000027', 'sample.faculty27@demo.ims.local', 'Chemistry', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-028', 'Sample28', 'Faculty', '8000000028', 'sample.faculty28@demo.ims.local', 'English', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-029', 'Sample29', 'Faculty', '8000000029', 'sample.faculty29@demo.ims.local', 'Computer Science', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-030', 'Sample30', 'Faculty', '8000000030', 'sample.faculty30@demo.ims.local', 'Commerce', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-031', 'Sample31', 'Faculty', '8000000031', 'sample.faculty31@demo.ims.local', 'Biology', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-032', 'Sample32', 'Faculty', '8000000032', 'sample.faculty32@demo.ims.local', 'History', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-033', 'Sample33', 'Faculty', '8000000033', 'sample.faculty33@demo.ims.local', 'Mathematics', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-034', 'Sample34', 'Faculty', '8000000034', 'sample.faculty34@demo.ims.local', 'Physics', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-035', 'Sample35', 'Faculty', '8000000035', 'sample.faculty35@demo.ims.local', 'Chemistry', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-036', 'Sample36', 'Faculty', '8000000036', 'sample.faculty36@demo.ims.local', 'English', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-037', 'Sample37', 'Faculty', '8000000037', 'sample.faculty37@demo.ims.local', 'Computer Science', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-038', 'Sample38', 'Faculty', '8000000038', 'sample.faculty38@demo.ims.local', 'Commerce', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-039', 'Sample39', 'Faculty', '8000000039', 'sample.faculty39@demo.ims.local', 'Biology', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-040', 'Sample40', 'Faculty', '8000000040', 'sample.faculty40@demo.ims.local', 'History', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-041', 'Sample41', 'Faculty', '8000000041', 'sample.faculty41@demo.ims.local', 'Mathematics', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-042', 'Sample42', 'Faculty', '8000000042', 'sample.faculty42@demo.ims.local', 'Physics', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-043', 'Sample43', 'Faculty', '8000000043', 'sample.faculty43@demo.ims.local', 'Chemistry', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-044', 'Sample44', 'Faculty', '8000000044', 'sample.faculty44@demo.ims.local', 'English', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-045', 'Sample45', 'Faculty', '8000000045', 'sample.faculty45@demo.ims.local', 'Computer Science', 'ACTIVE', NULL),
  (@inst, 'SAMPLE-F-046', 'Sample46', 'Faculty', '8000000046', 'sample.faculty46@demo.ims.local', 'Commerce', 'INACTIVE', UTC_TIMESTAMP(6)),
  (@inst, 'SAMPLE-F-047', 'Sample47', 'Faculty', '8000000047', 'sample.faculty47@demo.ims.local', 'Biology', 'INACTIVE', UTC_TIMESTAMP(6)),
  (@inst, 'SAMPLE-F-048', 'Sample48', 'Faculty', '8000000048', 'sample.faculty48@demo.ims.local', 'History', 'INACTIVE', UTC_TIMESTAMP(6)),
  (@inst, 'SAMPLE-F-049', 'Sample49', 'Faculty', '8000000049', 'sample.faculty49@demo.ims.local', 'Mathematics', 'INACTIVE', UTC_TIMESTAMP(6)),
  (@inst, 'SAMPLE-F-050', 'Sample50', 'Faculty', '8000000050', 'sample.faculty50@demo.ims.local', 'Physics', 'INACTIVE', UTC_TIMESTAMP(6));

SELECT
  (SELECT COUNT(*) FROM students WHERE institute_id = @inst AND email LIKE 'sample.student%@demo.ims.local') AS sample_students,
  (SELECT COUNT(*) FROM faculties WHERE institute_id = @inst AND email LIKE 'sample.faculty%@demo.ims.local') AS sample_faculties;
