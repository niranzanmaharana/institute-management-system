# ADR 0001 — Modular monolith for v1

**Status:** Accepted  
**Date:** 2026-08-08  
**Context:** Original plan proposed five+ Spring Boot microservices with separate MySQL databases. Review noted high operational cost for a single/few-institute product.

## Decision

Ship **v1 as a modular monolith**:

```text
Angular 22
    ↓
API Gateway (routing, rate limit, CORS, JWT check, tracing)
    ↓
ims-application (Spring Boot, Java 21)
    ├── identity
    ├── people
    ├── admissions
    ├── academic
    ├── finance
    ├── operations   (attendance, timetable, exams)
    ├── documents
    ├── notification (phase 2 depth)
    └── reporting    (phase 2 depth)
```

- Preserve **strict module boundaries** (packages, allowed dependencies, ports).
- Modules communicate via **application APIs / in-process domain events**, not by reaching into another module’s persistence.
- **Extract** a module to a separate deployable only when load, team, or release cadence justifies it (likely candidates: finance, notification, reporting).

## Consequences

- Simpler local setup, transactions for in-process workflows, debugging, and deployment.
- Cross-module consistency (e.g. enrollment → fee account) can be transactional in v1 via application orchestration + idempotency.
- Must enforce boundaries in code review / ArchUnit (or similar) so extraction remains possible.
- Gateway remains the SPA’s single entry point even with one backend app.
