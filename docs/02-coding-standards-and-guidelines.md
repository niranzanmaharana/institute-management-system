# Institute Management System — Coding Standards & Guidelines

**Document purpose:** Rules for writing maintainable, loosely coupled code across Angular 22, Spring Boot (Java 21) **modular monolith**, and shared infrastructure.  
**Companions:** [01-modules-and-delivery-plan.md](./01-modules-and-delivery-plan.md), [07-api-guidelines.md](./07-api-guidelines.md), ADRs  
**Status:** Updated after `review.md` — apply from the first commit of production code.

---

## 1. Goals

- Code that is easy to change without cascading breakage
- Clear ownership of business rules inside domain layers / modules
- Consistent style so any engineer can navigate any module
- Security, observability, and testability by default — not as afterthoughts
- **SOLID** and appropriate design patterns; **no tight coupling** between modules
- **Responsive UI** for laptop, tablet, and mobile
- **Predictable UX states** and **WCAG 2.2 AA** targets on primary flows

---

## 2. Guiding principles (all stacks)

1. **Dependencies point inward** — UI → application → domain; infrastructure implements domain ports.
2. **Prefer composition over inheritance.**
3. **Explicit over clever** — readable beats terse.
4. **Fail fast with clear errors** — never swallow exceptions silently.
5. **One reason to change per class** (SRP).
6. **Talk through interfaces / ports** at boundaries (DIP).
7. **Modules do not reach into each other’s persistence** (same rule as “no shared DB tables across services”).
8. **Don’t invent frameworks** — use Spring and Angular idioms unless they fight the domain.
9. **Business rules live in docs `03`/`04` and in domain code** — not only in tests or UI.
10. **Every business query is tenant-scoped** (`institute_id` from TenantContext / JWT — ADR 0004). Never trust client-sent tenant IDs.

---

## 3. SOLID — how we apply it

| Principle | Backend (Spring modules) | Frontend (Angular) |
| --- | --- | --- |
| **S**ingle Responsibility | One application service = one use-case cluster; controllers only map HTTP | One component = one UI concern; smart vs presentational split |
| **O**pen/Closed | Strategy for payment methods / fee adjustment types | Injectable strategies over giant `switch` in components |
| **L**iskov | Port implementations honor contracts | Prefer composition over brittle component inheritance |
| **I**nterface Segregation | Small ports (`FeeLedger`, `StudentDirectory`) | Narrow facades per feature |
| **D**ependency Inversion | Domain depends on interfaces; JPA is an adapter | Components depend on abstract APIs, not raw HTTP details |

### 3.1 Tight coupling — forbidden patterns

| Forbidden | Do instead |
| --- | --- |
| Module A imports Module B’s JPA entities / repositories | Application API, port, or domain event; IDs only |
| Angular feature imports another feature’s internals | Shared UI kit + public facades; lazy routes |
| Static mutable globals | Injectable scoped services |
| `new` of infrastructure inside domain | Constructor injection |
| Controllers → repositories for non-trivial flows | Application / use-case services |
| Domain types in fat `ims-common` | Thin common only (errors, tracing, page, security utils, **TenantContext**) |
| Hard-coded hosts/ports | Configuration |
| Shared “Util” god classes | Focused helpers |
| Unscoped repository `findById` without tenant check | Tenant-filtered access; missing row → 404 (no cross-tenant leak) |

Enforce module boundaries with **ArchUnit** (or equivalent) in CI.

---

## 4. Recommended design patterns

| Pattern | Where | Example |
| --- | --- | --- |
| **Hexagonal / Ports & Adapters** | Each module | `PaymentRecorder` port; JPA adapter |
| **Repository** | Persistence | Domain-friendly interfaces |
| **DTO / Mapper** | API edges | Never expose JPA entities in REST |
| **Strategy** | Variable algorithms | `PaymentAllocationStrategy`, `OutstandingCalculator` |
| **Factory** | Aggregates | `InvoiceFactory.fromFeePlan(...)` |
| **Domain Event** | Cross-module side effects | `EnrollmentActivated` → finance |
| **Outbox** | Reliable publish (ready in v1 schema) | Persist event + business row |
| **Facade** | Multi-step use cases | `CollectPaymentFacade` |
| **Circuit Breaker / Retry** | External HTTP (future extracted services) | Resilience4j |
| **CQRS-lite** | Heavy reports | Separate query services |

**Do not** force a pattern when a clear method suffices.

---

## 5. Backend standards (Java 21 + Spring Boot)

### 5.1 Language & runtime

- Java **21**: records for DTOs, sealed types where hierarchies are closed, pattern matching when clearer.
- Prefer immutability for DTOs/values.
- No wildcard imports.
- `Optional` for maybe-return only; Bean Validation on API inputs.
- Time: `Instant`/`OffsetDateTime` UTC; `LocalDate` for calendar dates.
- Money: `BigDecimal`, `HALF_UP`, scale 2 at boundaries.

### 5.2 Package structure (modular monolith)

```
com.ims
  ├── common             # thin shared library
  ├── identity
  │     ├── api | application | domain | infrastructure
  ├── people
  ├── admissions
  ├── academic
  ├── finance
  ├── operations
  ├── documents
  ├── notification
  └── reporting
```

Per-module layering:

| Layer | May call | Must not |
| --- | --- | --- |
| api (controllers) | application | other module repos |
| application | domain + own ports + other modules’ **public application APIs** | raw JDBC/Web in domain |
| domain | domain only | Spring Web, other modules |
| infrastructure | implements ports | own business policy beyond mapping |

### 5.3 REST & OpenAPI

- Follow [07-api-guidelines.md](./07-api-guidelines.md): **OpenAPI-first**, `/api/v1`, unified errors, pagination, idempotency.
- Prefer generating Angular API clients from OpenAPI.

### 5.4 Persistence

- Flyway only; no `ddl-auto=update` in shared envs.
- Transactions on application services; readOnly for queries.
- `@Version` on `student_fee_accounts` (and similar aggregates).
- No cross-module FKs (ADR 0002).
- **`institute_id` on every business table**; unique keys tenant-scoped (ADR 0004).
- Tenant filter strategy chosen in Phase 0 (Hibernate `@Filter` / aspect / mandatory param) and covered by isolation tests.

### 5.5 Security

- JWT validated at gateway; permissions on use cases (`@PreAuthorize` / equivalent).
- Object-level checks for self/assigned resources.
- Never log secrets or raw ID document contents.
- Audit business-critical transitions (doc 08).

### 5.6 Inter-module communication

- Prefer in-process **ports / application services / events**.
- Anti-corruption: map foreign module DTOs at the boundary.
- When a module is extracted later, replace the port adapter with HTTP — domain stays stable.

### 5.7 Logging & tracing

- **Priority:** every HTTP request and meaningful business activity must emit logs.
- SLF4J; console pattern includes MDC `traceId`, `spanId`, `requestId`, `userId`.
- Both `api-gateway` and `ims-application` use the **same** pattern and `X-Request-Id` header so one call can be grepped by `traceId` or `requestId` across processes.
- Propagation: W3C `traceparent` (Micrometer Tracing) + `X-Request-Id`.
- OTLP traces → Jaeger (`http://localhost:4318/v1/traces`, UI `http://localhost:16686`). Service names: `api-gateway`, `ims-application`.
- Mask PII at INFO (no raw passwords).
- Span / use-case names preferred for domain work (`finance.collectPayment`); HTTP access logs cover the transport layer.
- Helper: `com.ims.common.tracing.CorrelationIds`.

### 5.8 Testing

| Type | Tooling | Focus |
| --- | --- | --- |
| Unit | JUnit 5, AssertJ, Mockito | Domain, calculators |
| Integration | Spring Boot + Testcontainers MySQL | Flows, migrations, concurrency |
| ArchUnit | Module dependency rules | No illegal package coupling |
| API | OpenAPI / MockMvc | Contracts |
| Concurrency | Parallel payments on one account | Ledger integrity |

Name tests after business rules where possible: `shouldBlockSecondActiveEnrollment_sameCourse`.

### 5.9 Code style

- Spotless (or agreed formatter) in CI.
- Lombok sparingly; prefer records for DTOs; avoid `@Data` on entities.
- No commented-out code on main.

---

## 6. Frontend standards (Angular 22)

### 6.1 Project structure

```
src/app
  ├── core/
  ├── shared/          # design system + UX state components (mandatory)
  ├── features/
  ├── layout/
  └── environments/
```

- Lazy-loaded features; `core` must not import `features`.
- Cross-feature imports forbidden.

### 6.2 Modern Angular

- Standalone components; Signals; Signal Forms / typed reactive forms as adopted.
- `OnPush` / zoneless-ready patterns as team standard.
- `inject()` preferred in new code; `strict` TypeScript + templates.

### 6.3 Component rules

- Presentational vs container split.
- No complex template logic.
- `DestroyRef` / `takeUntilDestroyed` / async pipe for subscriptions.

### 6.4 State & data access

- API services per area; prefer **OpenAPI-generated** clients.
- Auth interceptor; centralized 401 refresh/logout.
- Show `traceId` on unexpected errors for support.

### 6.5 Design system (mandatory before feature UI)

Ship and reuse at least:

`Button`, `Input`, `Select`, `DatePicker`, `Modal`, `Drawer`, `Table`, `Card`, `EmptyState`, `LoadingState`, `ErrorState`, `ConfirmationDialog`, `Toast`, `Pagination`, responsive data view (table↔cards).

Feature teams do not invent one-off buttons/modals.

### 6.6 UX states (Definition of Done for screens)

Every list/detail/form handles:

- Loading
- Empty
- Success with data
- Validation error
- Authorization / forbidden
- Not found
- Server error / network error with Retry
- Partial data (when applicable)

### 6.7 Accessibility

- Target **WCAG 2.2 AA** for primary flows.
- Keyboard-only navigation, focus management, focus trap in modal/drawer, labels, contrast, accessible errors, semantic tables.

### 6.8 Responsive design — mandatory

Breakpoints (single source of truth):

| Token | Width |
| --- | --- |
| Mobile | `< 768px` |
| Tablet | `768–1023px` |
| Laptop | `≥ 1024px` |

Rules: mobile-first; drawer shell on small screens; table↔cards; contained table scroll only if needed; ≥44px touch targets; no hover-only critical actions; Playwright at **375 / 768 / 1440**.

### 6.9 Testing

| Type | Focus |
| --- | --- |
| Unit / component | Services, forms, guards |
| e2e | Login, vertical slice (enroll→pay), attendance |
| Responsive e2e | Critical paths at three viewports |

### 6.10 Code style

- ESLint + Prettier; no default exports; no magic breakpoint pixels in features.

---

## 7. Gateway & Identity

- Gateway: routing, authn validation, rate limits, header propagation — **no business DB access**.
- Strip inbound spoofed identity headers; set from JWT.
- Identity module: credentials + permission source of truth; audit auth failures without user enumeration.

---

## 8. Database & SQL

- Immutable migrations once applied; fix-forward.
- Index production query paths (especially outstanding, due dates, attendance).
- No `SELECT *`.
- Cross-module consistency via application orchestration + idempotency — not 2PC.

---

## 9. Configuration & secrets

- Env-based config; profiles `local`, `test`, `staging`, `prod`.
- `.env.example` only in git; real secrets elsewhere.
- See [09-deployment-and-operations.md](./09-deployment-and-operations.md).

---

## 10. Git & PR guidelines

- `main` deployable; `feature/...`, `fix/...`
- Focused commits; PR must list how tested + module impact.
- Checklist:
  - [ ] Standards + business rules followed
  - [ ] Tests added (incl. concurrency/idempotency if finance)
  - [ ] Migrations + OpenAPI updated
  - [ ] Audit events for critical transitions
  - [ ] No illegal module coupling
  - [ ] UI: UX states + responsive breakpoints + a11y basics

---

## 11. Documentation

- Module README snippets as needed.
- OpenAPI = HTTP contract.
- ADRs for significant choices under `docs/adr/`.

---

## 12. Performance & reliability

- Paginate lists; avoid N+1; cache only with invalidation story.
- Liveness/readiness; graceful shutdown.

---

## 13. Security checklist (every feature)

- [ ] AuthN / AuthZ / object-level where needed
- [ ] Input validated; PII minimized in logs
- [ ] Audit for critical transitions
- [ ] Idempotency for financial POSTs
- [ ] Dependency scan awareness
- [ ] (UI) Responsive + no hover-only critical actions

---

## 14. Example: collect payment

```
Angular Finance
  → Gateway (JWT, trace)
    → finance CollectPaymentFacade
        → OutstandingCalculator
        → ports (Payment, Invoice, Receipt, Audit, Idempotency)
```

Student display names via `StudentDirectory` port — finance never imports people JPA.

---

## 15. Anti-patterns (reject in review)

1. God classes
2. Anemic controllers holding all rules
3. JPA entities in REST
4. Cross-module FKs or repo calls
5. Swallowing exceptions
6. Stale TODOs without tickets
7. Permanent feature-flag bifurcations
8. Duplicating outstanding math in Angular
9. Unbounded fan-out I/O
10. Fat `ims-common` with domain types
11. Desktop-only CSS without mobile layout
12. Hover-only critical actions
13. Tables as only mobile UI for payment/attendance/enrollment
14. Screens without empty/error/forbidden states
15. Queries or APIs that omit tenant scope (cross-tenant IDOR)

---

## 16. Tooling baseline

| Area | Tool |
| --- | --- |
| Java | Maven, Spotless, JUnit 5, Testcontainers, ArchUnit, springdoc |
| Angular | CLI 22, ESLint, Prettier, Playwright |
| Containers | Docker Compose |
| Tracing | OpenTelemetry + Jaeger |
| CI | Build, test, lint, migrate-check, OpenAPI drift, ArchUnit |

---

## 17. Definition of Done

1. Meets this standards document and relevant BR/FR IDs
2. Automated tests green (incl. rule-focused tests)
3. Works through **Gateway** with JWT
4. Logs/traces correlate; audits written when required
5. Migrations + OpenAPI updated
6. No illegal module dependencies
7. README/ops notes if runbooks change
8. Review for coupling/SOLID
9. **UI:** UX states + mobile/tablet/laptop verified + a11y basics on the touched flow
10. **Tenancy:** no cross-tenant leakage for any new resource (isolation test or explicit N/A for platform-only)

---

## 18. Decisions

| Topic | Recommendation |
| --- | --- |
| JPA on entities | Pragmatic in v1; never expose via API |
| MapStruct | OK for repetitive mapping |
| `ims-common` | Thin only |
| Angular test runner | Pick one in Phase 1 and keep |
| OpenAPI client gen | Prefer generated Angular clients |
| Design system | Mandatory before feature UI |

---

## 19. Document history

| Version | Date | Notes |
| --- | --- | --- |
| 0.1 | 2026-08-08 | Initial standards |
| 0.2 | 2026-08-08 | Responsive rules |
| 0.3 | 2026-08-08 | Modular monolith language; design system; UX states; WCAG; OpenAPI-first; ArchUnit; audit DoD |
| 0.4 | 2026-08-08 | Multi-tenant from day one — TenantContext rules & anti-IDOR |
