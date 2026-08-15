# ADR 0003 — Authentication & authorization

**Status:** Accepted  
**Date:** 2026-08-08  

## Decision

- **Identity** is a module inside the modular monolith (not a separate deployable in v1).
- AuthN: username/email + password; **JWT access tokens** (short-lived) + **refresh tokens**.
- Access JWT includes **`institute_id`** for institute users (ADR 0004).
- Gateway validates JWT on inbound requests; the application also enforces authorization on use cases.
- AuthZ: **roles + fine-grained permissions** (`fee:write`, `student:read`, …). Prefer permission checks in code.
- Map `users` → person via `person_type` + `person_id` (student/faculty/staff) **within the same institute**. A person may hold multiple app roles (e.g. faculty who is also admin) via `user_roles`.
- `PLATFORM_ADMIN` users have no institute membership (or null `institute_id`) and only institute lifecycle APIs.

## Consequences

- Extracting Identity later is still possible if SSO/multi-app needs arise.
- Permission catalog must stay in sync with `06-authorization-matrix.md`.
