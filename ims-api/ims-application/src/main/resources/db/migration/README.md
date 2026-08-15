# Flyway conventions (IMS)

1. Every **business** table includes `institute_id BIGINT NOT NULL` (except `institutes` and rare platform-only rows).
2. Natural keys are tenant-scoped: `UNIQUE (institute_id, ...)`.
3. No foreign keys across module boundaries; within-module FKs are OK.
4. Migrations are immutable once merged — fix forward with a new version.
5. Seed at least two institutes (`DEMO_A`, `DEMO_B`) for isolation tests.
6. People module tables (`students`, `guardians`, `student_guardians`, `student_addresses`, …) always include `institute_id`.
