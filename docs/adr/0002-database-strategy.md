# ADR 0002 — Database strategy

**Status:** Accepted  
**Date:** 2026-08-08  
**Context:** Microservices plan used one DB per service. Modular monolith changes that.

## Decision

- **One MySQL 8 database** for v1 (`ims`).
- Tables owned by a single module; naming: prefer clear names (`students`, `invoices`) with module ownership documented in `05-domain-model.md`.
- **Every business table includes `institute_id`** (ADR 0004). Unique constraints are tenant-scoped.
- **No foreign keys across module boundaries.** Cross-module references are opaque IDs (`student_id`, `enrollment_id`) validated in application code **within the same institute**.
- Foreign keys **within** a module are encouraged.
- All schema changes via **Flyway**.
- Logical grouping may use table prefixes only if helpful (`fin_`, `ops_`) — not required if ownership is clear.

## Consequences

- Easier joins for reporting inside carefully designed read models; still avoid leaking write-side coupling.
- Extraction to a microservice later requires breaking out tables + replacing ID checks with APIs/events.
- Single backup/restore unit for operational simplicity (see deployment doc).
