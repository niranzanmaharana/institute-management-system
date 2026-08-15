# Document index

**Status:** Phase 0 complete (code). Phase 1 identity/auth shell delivered — start Docker to run full stack.

| Doc | Purpose |
| --- | --- |
| [01-modules-and-delivery-plan.md](./01-modules-and-delivery-plan.md) | Product scope, architecture, modules, MySQL ownership, testable delivery steps |
| [02-coding-standards-and-guidelines.md](./02-coding-standards-and-guidelines.md) | SOLID, patterns, Angular/Spring standards, responsive UI, DoD |
| [03-functional-requirements.md](./03-functional-requirements.md) | What the system must do (capabilities & workflows) |
| [04-business-rules.md](./04-business-rules.md) | Explicit business rules & state transitions |
| [05-domain-model.md](./05-domain-model.md) | Aggregates, relationships, ubiquitous language |
| [06-authorization-matrix.md](./06-authorization-matrix.md) | Roles × capabilities |
| [07-api-guidelines.md](./07-api-guidelines.md) | OpenAPI-first, versioning, idempotency |
| [08-security-and-privacy.md](./08-security-and-privacy.md) | Auth, PII, documents, audit |
| [09-deployment-and-operations.md](./09-deployment-and-operations.md) | Environments, CI/CD, backup/DR, health |
| [10-reporting-requirements.md](./10-reporting-requirements.md) | Reports that influence indexes & APIs |
| [11-mvp-v1-step-by-step-development.md](./11-mvp-v1-step-by-step-development.md) | **Authoritative MVP & v1 build order** (testable steps) |
| [12-course-fee-structure-and-relationships.md](./12-course-fee-structure-and-relationships.md) | How courses, fee plans, batches, enrollments & finance ledger relate |
| [design/](./design/) | UI look-and-feel reference (Skydash) |
| [review.md](./review.md) | External architecture/product review notes |
| [adr/](./adr/) | Architecture Decision Records |

**Code roots:** [ims-ui](../ims-ui) (Angular), [ims-api](../ims-api) (Spring), [infra](../infra), [postman](../postman)

## Locked decisions (summary)

| Topic | Decision | ADR |
| --- | --- | --- |
| Deployment shape (v1) | **Modular monolith** + API Gateway; extract services later | [0001](./adr/0001-modular-monolith.md) |
| Database (v1) | **One MySQL database**; module-owned tables; no cross-module FKs | [0002](./adr/0002-database-strategy.md) |
| Authentication | JWT (access) + refresh; Identity as a **module** | [0003](./adr/0003-authentication.md) |
| Multi-tenancy | **Option B** — multi-tenant from day one (`institute_id` on all business data) | [0004](./adr/0004-multi-tenancy.md) |
| Institute type (v1) | **Professional training / coaching institute** (multi-course, batches) | [0005](./adr/0005-institute-type.md) |
