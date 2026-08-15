# ADR 0004 — Multi-tenancy

**Status:** Accepted (revised)  
**Date:** 2026-08-08  
**Supersedes:** Option C draft  

## Context

Deferring `institute_id` until “later” forces painful migrations of unique constraints, queries, JWT claims, storage paths, and authorization. Product intent is multi-institute from the start.

## Decision

**Option B — Multi-tenant from day one (shared database, shared schema, row-level tenancy).**

1. Every business table includes **`institute_id BIGINT NOT NULL`** (except pure platform tables such as global migration history).
2. **Unique constraints are tenant-scoped** — e.g. `UNIQUE (institute_id, student_code)`, `UNIQUE (institute_id, course_code)`.
3. **JWT access token carries `institute_id`** (and optionally `institute_code`). All use cases resolve tenant from the token — **never** from a client-supplied body field alone.
4. Repositories / queries are **tenant-aware by default** (Hibernate filter, Spring Data aspect, or explicit mandatory `instituteId` on every repository method — pick one in Phase 0 and enforce with tests + ArchUnit).
5. Object storage keys are prefixed: `institutes/{institute_id}/...`.
6. Cross-tenant access is forbidden except for a **platform-level** role (`PLATFORM_ADMIN`) used only to create/suspend institutes and support operations.
7. v1 still uses the **modular monolith + one MySQL database** (ADR 0001/0002). Tenancy is **logical** (row-level), not database-per-tenant.
8. Full **SaaS commercial billing** for institutes (subscriptions, invoices to institutes) may remain a later product feature; **data isolation for multiple institutes is mandatory now**.

## Tenant resolution

```text
Login (user belongs to one institute)
  → JWT { sub, roles, institute_id }
  → Gateway / app TenantContext
  → All reads/writes filtered by institute_id
```

Users do not switch institutes in v1 without re-login (no multi-institute membership yet unless explicitly added later).

## Consequences

- Slightly more verbose schema and every query must be tenant-safe.
- Impossible to “forget” tenancy in new tables if checklist + tests require `institute_id`.
- Indexes typically lead with `institute_id`.
- Seed data creates at least one demo institute for local/dev.
- Adding a second institute in QA is a **release acceptance test**.
