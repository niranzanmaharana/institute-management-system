# Institute Management System — Modules, Architecture & Delivery Plan

**Document purpose:** Product scope, modular architecture, module ownership, MySQL structures, and a testable delivery path.  
**Audience:** Product / architecture review before implementation.  
**Status:** Updated after `review.md` — **do not start domain coding** until companion docs `03`–`06` and ADRs are accepted.  
**Index:** [README.md](./README.md)

---

## 1. Product vision

Build a **multi-tenant** institute management platform for **professional training / coaching institutes** (ADR 0005): courses, admissions, students, faculties, enrollments, fees (including outstanding balances), attendance, examinations, and supporting administration. Each institute’s data is isolated by `institute_id` from day one (ADR 0004).

**UI:** Responsive Angular **22** (laptop, tablet, mobile).  
**Backend (v1):** Java **21** / Spring Boot **modular monolith** behind an API Gateway, with Identity as a module, structured logging, and distributed tracing (ADR 0001).

### 1.1 Technology baseline


| Layer         | Choice                                                                                |
| ------------- | ------------------------------------------------------------------------------------- |
| UI            | Angular **22**, fully responsive (see §3.13)                                          |
| Backend       | Java **21**, Spring Boot **3.x** — **one deployable application** with strict modules |
| API entry     | Spring Cloud Gateway (or equivalent) — single entry for the SPA                       |
| AuthN / AuthZ | Identity **module** — OAuth2/OIDC-style JWT access + refresh (ADR 0003)               |
| Data          | MySQL **8.x** — **one database**; module-owned tables; no cross-module FKs (ADR 0002) |
| Files         | Private object storage (MinIO local / S3) — pre-signed URLs (see doc 08)              |
| Messaging     | In-process domain events in v1; transactional outbox ready; broker later              |
| Observability | JSON logs + OpenTelemetry + correlation IDs                                           |
| Build         | Maven multi-module, Angular CLI, Docker Compose                                       |




### 1.2 Institute type & non-goals (v1)

**In scope:** Multi-course coaching/training with batches, admission workflow, fee ledger, attendance, exams (pass/fail), certificates/receipts, CSV import/export for core data.

**Out of scope:** Native mobile apps; LMS/video; payroll/HRIS; marketing CMS; full commercial **SaaS billing** to institutes (subscriptions/invoicing the tenant) — tenant **data isolation** is in scope; full hostel/transport ops; semester promotion/GPA (revise ADR 0005 if customer is a college/school).

### 1.3 Multi-tenancy

**Option B (ADR 0004) — multi-tenant from day one.**


| Rule       | Detail                                                                                                                                   |
| ---------- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| Model      | Shared DB/schema; **row-level** tenancy via `institute_id`                                                                               |
| Schema     | Every business table has `institute_id NOT NULL`                                                                                         |
| Uniqueness | Scoped per tenant, e.g. `(institute_id, student_code)`                                                                                   |
| Auth       | JWT carries `institute_id`; TenantContext on every request                                                                               |
| Queries    | Tenant filter mandatory — no unscoped `findAll`                                                                                          |
| Storage    | Object keys under `institutes/{institute_id}/...`                                                                                        |
| Platform   | `PLATFORM_ADMIN` creates/suspends institutes; cannot be used as a bypass for normal institute data APIs without explicit support tooling |
| Acceptance | QA must run the vertical slice on **two** institutes with zero cross-tenant leakage                                                      |


Master table:

```sql
CREATE TABLE institutes (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  code            VARCHAR(32) NOT NULL UNIQUE,
  name            VARCHAR(255) NOT NULL,
  status          ENUM('ACTIVE','SUSPENDED','CLOSED') NOT NULL DEFAULT 'ACTIVE',
  timezone        VARCHAR(64) NOT NULL DEFAULT 'Asia/Kolkata',
  mobile          VARCHAR(32) NULL,
  admin_email     VARCHAR(255) NULL,
  website         VARCHAR(512) NULL,
  address_line1   VARCHAR(255) NULL,
  address_line2   VARCHAR(255) NULL,
  city            VARCHAR(128) NULL,
  state           VARCHAR(128) NULL,
  postal_code     VARCHAR(32) NULL,
  country         VARCHAR(128) NULL,
  icon_url        VARCHAR(1024) NULL, -- URL for now; MinIO object key later
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```



### 1.4 Companion specifications (required reading)


| Doc                                                               | Covers                      |
| ----------------------------------------------------------------- | --------------------------- |
| [03-functional-requirements.md](./03-functional-requirements.md)  | Capabilities & workflows    |
| [04-business-rules.md](./04-business-rules.md)                    | Rules & state transitions   |
| [05-domain-model.md](./05-domain-model.md)                        | Aggregates & events         |
| [06-authorization-matrix.md](./06-authorization-matrix.md)        | Roles × capabilities        |
| [07](./07-api-guidelines.md)–[10](./10-reporting-requirements.md) | API, security, ops, reports |


---



## 2. High-level architecture (v1)

```
┌─────────────────────────────────────────────────────────────┐
│                     Angular 22 SPA                           │
│     auth · feature modules · responsive shell · design system │
└───────────────────────────┬─────────────────────────────────┘
                            │ HTTPS
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    API Gateway                               │
│  routing · rate limit · CORS · JWT validation · tracing      │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│              ims-application (modular monolith)              │
│  identity │ people │ admissions │ academic │ finance         │
│  operations │ documents │ notification* │ reporting*         │
└───────────────────────────┬─────────────────────────────────┘
                            │
              ┌─────────────┼─────────────┐
              ▼             ▼             ▼
           MySQL 8        MinIO         Jaeger
            (ims)       (private)       (traces)
```

 notification / reporting: designed now; depth in later phases.

### 2.1 Design principles

- **One business capability per module** (bounded context).
- Modules call each other via **application services / ports / domain events** — never another module’s repositories or tables.
- Thin `ims-common`: error contract, tracing, pagination, security primitives only — **no domain DTOs/entities**.
- Angular talks **only** to the Gateway.
- Same boundaries enable later extraction to microservices without a rewrite of the domain language.



### 2.2 Modules (v1 ownership)


| Module                                                       | Responsibility                                                                | Owns data for                                                      |
| ------------------------------------------------------------ | ----------------------------------------------------------------------------- | ------------------------------------------------------------------ |
| `api-gateway`                                                | Entry, routing, JWT, rate limits, trace propagation                           | None                                                               |
| `identity`                                                   | Users, roles, permissions, tokens, login audit; institute membership on users | users, roles, permissions, refresh_tokens                          |
| `institutes` *(platform, may live under identity or config)* | Create/suspend tenants                                                        | institutes                                                         |
| `people`                                                     | Students, faculties, staff, guardians, addresses, doc metadata                | students, faculties, staff, guardians, addresses, person_documents |
| `admissions`                                                 | Enquiries, applications, approve/reject                                       | enquiries, admission_applications                                  |
| `academic`                                                   | Courses, fee **catalog**, batches, enrollments, assignments                   | courses, fee plans, batches, enrollments, …                        |
| `finance`                                                    | Fee accounts, invoices, payments, receipts, refunds, outstanding              | fee accounts, invoices, payments, receipts, refunds                |
| `operations`                                                 | Rooms, timetable, attendance, exams/results                                   | attendance, timetable, exams                                       |
| `documents`                                                  | Certificate/receipt generation orchestration                                  | templates, generated_documents                                     |
| `notification`                                               | Channel-abstract notifications (email first)                                  | templates, deliveries                                              |
| `reporting`                                                  | Heavy reports / exports (many ★ reports start as module queries)              | optional read models later                                         |


> **Fees:** Catalog in **academic**. Ledger, receipts, outstanding in **finance**. Finance snapshots plan amounts at enrollment; does not use live catalog for balance math.



### 2.3 Evolution path

```text
Modular monolith → extract Finance / Notification / Reporting when justified
```

---



## 3. Module catalog (functional)

Detailed FRs in doc 03. Summary below. “Owner” = module of record.

### 3.1 Identity & access

Login/logout, RBAC, user↔person link, password reset, login audit. Matrix: doc 06.

### 3.2 Institute setup

Institute profile, academic years, departments, rooms, holidays; **financial years** (finance module).

### 3.3 Admissions (first-class)

Enquiry → Application → APPROVED/REJECTED/CANCELLED → Student (+ enrollment path).  
Statuses and rules: doc 04 §1. **Do not overload** `students`**/**`enrollments` **with admission states.**

### 3.4 Course & academic

Course catalog, fee plans (with **fee categories**), subjects, batches, prerequisites, faculty assignment, enrollments, transfer/withdraw/complete.

### 3.5 Student, faculty, guardians

Students/faculties/staff; **Guardian** many-to-many via `student_guardians` (siblings share a parent).

### 3.6 Fee & finance (including outstanding)

Fee account per enrollment; invoices by category; payments; **receipts**; allocations; adjustments; **refund lifecycle**; reversals; outstanding snapshot; concurrency + idempotency (docs 04, 07).

**Outstanding (server truth):**

```text
outstanding = due invoices − successful payments − discounts/waivers/scholarships + penalties
```

(Refunds via refund lifecycle — see BR-RF-*).

### 3.7 Attendance

Sessions with `DRAFT | SUBMITTED | LOCKED`; records; % rules (LATE=present; EXCUSED excluded from denominator).

### 3.8 Timetable & examinations

Conflict-checked timetable; exams with **components** (theory/practical/internal), publish/lock, ABSENT/WITHHELD.

### 3.9 Documents & certificates

Fee receipts, bonafide/completion (minimal v1); private object storage.

### 3.10 Communication & reporting

Designed now (docs 05, 10); email channel first; ★ reports in v1 screens/exports.

### 3.11 Import / export

CSV students/faculty; exports for fees/attendance; bulk marks; async for large files.

### 3.12 Angular UI modules

Auth, Dashboard, Admissions, Students, Faculties, Courses/Batches, Finance, Attendance, Timetable, Exams, Documents, Admin, Profile — plus design system (Button, Input, Table/Card, Modal/Drawer, Empty/Loading/Error, …) **before** feature sprawl.

### 3.13 Responsive UI — mandatory


| Name   | Width        | Expectation                            |
| ------ | ------------ | -------------------------------------- |
| Mobile | `< 768px`    | Drawer/hamburger; cards; stacked forms |
| Tablet | `768–1023px` | Collapsible nav; hybrid list/table     |
| Laptop | `≥ 1024px`   | Sidebar; full tables                   |


Rules: no page-level horizontal scroll; tables→cards or contained scroll on small screens; timetable day/agenda on mobile; touch targets ≥ 44px; Playwright at 375 / 768 / 1440. Full detail retained from prior revision; standards in doc 02 §6.6.

### 3.14 UX states & accessibility

Every screen: Loading, Empty, Success, Validation error, Forbidden, Not found, Server/Network error, Retry.  
Target: **WCAG 2.2 AA** on primary flows (doc 02).

---



## 4. Cross-cutting platform capabilities



### 4.1 Identity flow

Authenticate via identity module (through gateway) → JWT + refresh → gateway validates → use-case permission checks → object-level authorization (self / assigned).

### 4.2 Logging, tracing, audit

Correlation/`traceparent` across gateway → app. Console logs with MDC `traceId`/`spanId`/`requestId`/`userId` (JSON encoder optional later).  
**Audit** beyond finance: all business-critical transitions (doc 08).

### 4.3 Error contract

Unified JSON with `traceId`, `status`, `error`, `message`, `details`.

### 4.4 Idempotency & concurrency

`Idempotency-Key` + `idempotency_records` for payments, invoice generation, refunds.  
Optimistic locking on fee accounts; concurrent payment test required.

### 4.5 Health

`/actuator/health/liveness` & `readiness` on app and gateway.

---



## 5. Database structure (MySQL 8)

**One database** `ims`**.** Conventions: InnoDB, `utf8mb4`, BIGINT PKs, `DECIMAL(12,2)` money, audit columns, soft delete where needed. **No cross-module FKs.**

### Tenant column (mandatory)

```text
institute_id BIGINT NOT NULL
```

- Present on **every** business table (people, admissions, academic, finance, operations, documents, notification, reporting seeds, idempotency, audit_logs, etc.).
- Indexes typically `(institute_id, …)`.
- Natural keys: `UNIQUE (institute_id, <code>)` — never globally unique codes across tenants (except `institutes.code` and platform-level username policy — see identity notes).
- `institutes` itself has no `institute_id`.

Schemas below are module-owned. Full business meaning in docs 04–05. Examples show `institute_id`; apply the same pattern to all omitted tables.

### 5.1 Identity

`users` include `institute_id` (NULL only for `PLATFORM_ADMIN`). Login resolves tenant from the user record into the JWT. Roles/permissions may be global catalog; **user_roles** are per user (hence per institute). Prefer permission checks still scoped by tenant data access.

Unique username/email: **per institute** — `UNIQUE (institute_id, username)`, `UNIQUE (institute_id, email)` — so two institutes may both have `admin@…` locally if desired; platform admins use a separate namespace.

### 5.2 People

```sql
-- students, faculties, staff as before (extend only with justified fields)
-- ALL include institute_id; student_code unique per institute:

-- UNIQUE KEY uk_student_code (institute_id, student_code)

CREATE TABLE guardians (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id BIGINT NOT NULL,
  name         VARCHAR(128) NOT NULL,
  phone        VARCHAR(32) NULL,
  email        VARCHAR(255) NULL,
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at   DATETIME NULL,
  UNIQUE KEY uk_guardian_phone (institute_id, phone),
  UNIQUE KEY uk_guardian_email (institute_id, email),
  KEY idx_guardians_institute (institute_id)
);

CREATE TABLE student_guardians (
  institute_id BIGINT NOT NULL,
  student_id   BIGINT NOT NULL,
  guardian_id  BIGINT NOT NULL,
  relation     VARCHAR(64) NOT NULL,
  is_primary   TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (student_id, guardian_id),
  KEY idx_sg_institute (institute_id)
);

CREATE TABLE student_addresses (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  line1 VARCHAR(255) NOT NULL,
  line2 VARCHAR(255) NULL,
  city VARCHAR(64) NOT NULL,
  state VARCHAR(64) NULL,
  postal_code VARCHAR(16) NULL,
  country VARCHAR(64) NOT NULL DEFAULT 'India',
  is_primary TINYINT(1) NOT NULL DEFAULT 1,
  FOREIGN KEY (student_id) REFERENCES students(id)
);
-- similarly faculty_addresses, staff_addresses (each with institute_id)

CREATE TABLE person_documents (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id BIGINT NOT NULL,
  owner_type ENUM('STUDENT','FACULTY','STAFF') NOT NULL,
  owner_id BIGINT NOT NULL,
  doc_type VARCHAR(64) NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  storage_key VARCHAR(512) NOT NULL, -- institutes/{institute_id}/...
  content_type VARCHAR(128) NOT NULL,
  file_size BIGINT NOT NULL,
  checksum VARCHAR(128) NULL,
  status ENUM('ACTIVE','DELETED') NOT NULL DEFAULT 'ACTIVE',
  uploaded_by BIGINT NULL,
  uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_docs_institute (institute_id)
);
```



### 5.3 Admissions

```sql
CREATE TABLE enquiries (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  phone VARCHAR(32) NULL,
  email VARCHAR(255) NULL,
  interested_course_id BIGINT NULL,
  status ENUM('OPEN','CONVERTED','CLOSED') NOT NULL DEFAULT 'OPEN',
  notes TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_enquiry_institute (institute_id)
);

CREATE TABLE admission_applications (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id BIGINT NOT NULL,
  application_no VARCHAR(32) NOT NULL,
  enquiry_id BIGINT NULL,
  course_id BIGINT NOT NULL,
  student_id BIGINT NULL,
  status ENUM('DRAFT','SUBMITTED','PENDING_REVIEW','APPROVED','REJECTED','CANCELLED') NOT NULL,
  reason VARCHAR(512) NULL,
  submitted_at DATETIME NULL,
  decided_at DATETIME NULL,
  decided_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_application_no (institute_id, application_no),
  KEY idx_appl_institute (institute_id)
);
```



### 5.4 Academic (excerpt — fee category + existing course/batch/enrollment)

```sql
CREATE TABLE fee_categories (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id BIGINT NOT NULL,
  code VARCHAR(32) NOT NULL, -- ADMISSION_FEE, TUITION_FEE, ...
  name VARCHAR(128) NOT NULL,
  UNIQUE KEY uk_fee_cat (institute_id, code)
);

-- courses, batches, enrollments, etc.: all have institute_id
-- UNIQUE (institute_id, code) for courses/batches
```

Retain `courses`, `course_fee_plans`, `course_fee_installments`, `subjects`, `batches`, `enrollments`, `batch_faculty_assignments`, `course_prerequisites`, `academic_years`, `departments` from prior design — **each with** `institute_id`, aligned to business rules.

### 5.5 Finance

All finance tables include `institute_id`. Document numbers unique **per institute**.

```sql
CREATE TABLE financial_years (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id BIGINT NOT NULL,
  name VARCHAR(64) NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  is_active TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_fy_name (institute_id, name)
);

CREATE TABLE student_fee_accounts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  enrollment_id BIGINT NOT NULL,
  currency CHAR(3) NOT NULL DEFAULT 'INR',
  status ENUM('OPEN','CLOSED') NOT NULL DEFAULT 'OPEN',
  outstanding_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_enrollment (institute_id, enrollment_id),
  KEY idx_outstanding (institute_id, outstanding_amount)
);

CREATE TABLE invoices (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id BIGINT NOT NULL,
  fee_account_id BIGINT NOT NULL,
  invoice_no VARCHAR(32) NOT NULL,
  fee_category_id BIGINT NOT NULL,
  financial_year_id BIGINT NOT NULL,
  description VARCHAR(255) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  due_date DATE NOT NULL,
  status ENUM('DUE','PARTIAL','PAID','CANCELLED','WAIVED') NOT NULL DEFAULT 'DUE',
  installment_no INT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_invoice_no (institute_id, invoice_no),
  FOREIGN KEY (fee_account_id) REFERENCES student_fee_accounts(id)
);

CREATE TABLE payments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id BIGINT NOT NULL,
  fee_account_id BIGINT NOT NULL,
  payment_no VARCHAR(32) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  method ENUM('CASH','UPI','CARD','BANK_TRANSFER','CHEQUE','OTHER') NOT NULL,
  paid_at DATETIME NOT NULL,
  reference VARCHAR(128) NULL,
  received_by BIGINT NULL,
  financial_year_id BIGINT NOT NULL,
  status ENUM('SUCCESS','FAILED','REVERSED') NOT NULL DEFAULT 'SUCCESS',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_payment_no (institute_id, payment_no),
  FOREIGN KEY (fee_account_id) REFERENCES student_fee_accounts(id)
);

CREATE TABLE payment_allocations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id BIGINT NOT NULL,
  payment_id BIGINT NOT NULL,
  invoice_id BIGINT NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  FOREIGN KEY (payment_id) REFERENCES payments(id),
  FOREIGN KEY (invoice_id) REFERENCES invoices(id)
);

CREATE TABLE receipts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id BIGINT NOT NULL,
  payment_id BIGINT NOT NULL UNIQUE,
  receipt_no VARCHAR(32) NOT NULL,
  issued_at DATETIME NOT NULL,
  status ENUM('ISSUED','CANCELLED') NOT NULL DEFAULT 'ISSUED',
  storage_key VARCHAR(512) NULL,
  UNIQUE KEY uk_receipt_no (institute_id, receipt_no),
  FOREIGN KEY (payment_id) REFERENCES payments(id)
);

CREATE TABLE fee_adjustments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id BIGINT NOT NULL,
  fee_account_id BIGINT NOT NULL,
  type ENUM('DISCOUNT','SCHOLARSHIP','WAIVER','PENALTY') NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  reason VARCHAR(512) NOT NULL,
  approved_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (fee_account_id) REFERENCES student_fee_accounts(id)
);

CREATE TABLE refund_requests (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id BIGINT NOT NULL,
  fee_account_id BIGINT NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  status ENUM('REQUESTED','APPROVED','REJECTED','PROCESSING','COMPLETED','FAILED') NOT NULL,
  reason VARCHAR(512) NOT NULL,
  requested_by BIGINT NULL,
  decided_by BIGINT NULL,
  processed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (fee_account_id) REFERENCES student_fee_accounts(id)
);

CREATE TABLE finance_audit (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id BIGINT NOT NULL,
  entity_type VARCHAR(64) NOT NULL,
  entity_id BIGINT NOT NULL,
  action VARCHAR(64) NOT NULL,
  before_json JSON NULL,
  after_json JSON NULL,
  reason VARCHAR(512) NULL,
  actor_user_id BIGINT NULL,
  request_id VARCHAR(64) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_fin_audit_inst (institute_id, created_at)
);

CREATE TABLE idempotency_records (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id BIGINT NOT NULL,
  idempotency_key VARCHAR(128) NOT NULL,
  user_id BIGINT NOT NULL,
  operation VARCHAR(64) NOT NULL,
  request_hash VARCHAR(128) NOT NULL,
  response_status INT NOT NULL,
  response_body JSON NOT NULL,
  expires_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_idem (institute_id, user_id, operation, idempotency_key)
);
```



### 5.6 Operations

All operations tables include `institute_id`. Attendance/exam unique keys are tenant-scoped.

```sql
-- rooms, calendar_holidays, timetable_slots: + institute_id

CREATE TABLE attendance_sessions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id BIGINT NOT NULL,
  batch_id BIGINT NOT NULL,
  session_date DATE NOT NULL,
  subject_id BIGINT NULL,
  status ENUM('DRAFT','SUBMITTED','LOCKED') NOT NULL DEFAULT 'DRAFT',
  marked_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_batch_session (institute_id, batch_id, session_date, subject_id)
);

CREATE TABLE attendance_records (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id BIGINT NOT NULL,
  session_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  status ENUM('PRESENT','ABSENT','LATE','EXCUSED') NOT NULL,
  note VARCHAR(255) NULL,
  FOREIGN KEY (session_id) REFERENCES attendance_sessions(id),
  UNIQUE KEY uk_session_student (session_id, student_id)
);

CREATE TABLE exams (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id BIGINT NOT NULL,
  batch_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  exam_date DATE NOT NULL,
  status ENUM('SCHEDULED','DRAFT_MARKS','PUBLISHED','LOCKED','CANCELLED') NOT NULL DEFAULT 'SCHEDULED'
);

CREATE TABLE exam_components (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id BIGINT NOT NULL,
  exam_id BIGINT NOT NULL,
  subject_id BIGINT NULL,
  code VARCHAR(32) NOT NULL,
  max_marks DECIMAL(8,2) NOT NULL,
  pass_marks DECIMAL(8,2) NOT NULL,
  weightage DECIMAL(5,2) NULL,
  FOREIGN KEY (exam_id) REFERENCES exams(id)
);

CREATE TABLE exam_results (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id BIGINT NOT NULL,
  exam_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  result_status ENUM('MARKS','ABSENT','WITHHELD') NOT NULL DEFAULT 'MARKS',
  total_marks DECIMAL(8,2) NULL,
  grade VARCHAR(8) NULL,
  remarks VARCHAR(255) NULL,
  FOREIGN KEY (exam_id) REFERENCES exams(id),
  UNIQUE KEY uk_exam_student (institute_id, exam_id, student_id)
);

CREATE TABLE exam_component_marks (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id BIGINT NOT NULL,
  exam_result_id BIGINT NOT NULL,
  exam_component_id BIGINT NOT NULL,
  marks DECIMAL(8,2) NULL,
  FOREIGN KEY (exam_result_id) REFERENCES exam_results(id),
  FOREIGN KEY (exam_component_id) REFERENCES exam_components(id),
  UNIQUE KEY uk_result_component (exam_result_id, exam_component_id)
);
```



### 5.7 Platform audit

```sql
CREATE TABLE audit_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id BIGINT NULL, -- NULL only for platform-level actions
  module VARCHAR(64) NOT NULL,
  entity_type VARCHAR(64) NOT NULL,
  entity_id BIGINT NOT NULL,
  action VARCHAR(64) NOT NULL,
  before_json JSON NULL,
  after_json JSON NULL,
  reason VARCHAR(512) NULL,
  actor_user_id BIGINT NULL,
  request_id VARCHAR(64) NULL,
  ip_address VARCHAR(64) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_entity (institute_id, entity_type, entity_id),
  KEY idx_actor (actor_user_id, created_at)
);
```

---



## 6. Delivery plan — foundations, then vertical slice

**Authoritative step-by-step roadmap (MVP vs v1, every step expanded):**  
→ **[11-mvp-v1-step-by-step-development.md](./11-mvp-v1-step-by-step-development.md)**

- **MVP** = Phases 0–2 (foundations, identity/shell, fee vertical slice + two-tenant isolation)  
- **v1** = MVP + Phases 3–7  
- **Post-v1** = Phase 8 (extraction / broker)

Summary below. Rules: every step has **Deliverable / Test plan / Exit criteria**. Prefer unit → module integration (Testcontainers) → API → UI e2e (incl. responsive viewports).

### Phase 0 — Foundations (no domain features yet)


| Step | Deliverable                                                                                                    | Test / exit                                         |
| ---- | -------------------------------------------------------------------------------------------------------------- | --------------------------------------------------- |
| 0.1  | Monorepo: `frontend`, `backend/ims-application` (module packages), `api-gateway`, `ims-common` (thin), `infra` | Skeleton builds                                     |
| 0.2  | Compose: MySQL, MinIO, Jaeger                                                                                  | Healthchecks green                                  |
| 0.3  | Docs/ADRs accepted; ArchUnit module rules stub                                                                 | Review sign-off                                     |
| 0.4  | `ims-common` errors/tracing/page                                                                               | Unit tests                                          |
| 0.5  | Flyway baseline + institutes + audit + idempotency + **tenant column conventions**                             | Migrate twice OK                                    |
| 0.6  | TenantContext + repository filter strategy + cross-tenant isolation test harness                               | Test: institute A cannot read institute B row by id |




### Phase 1 — Identity + Gateway + Angular shell + design system


| Step    | Focus                                              | Exit                               |
| ------- | -------------------------------------------------- | ---------------------------------- |
| 1.1–1.4 | Identity APIs, gateway, tracing                    | Login via gateway; trace in Jaeger |
| 1.5     | Angular auth + **responsive shell**                | 375/768/1440 OK                    |
| 1.6     | **Design system** primitives + UX state components | Mandatory before features          |
| 1.7     | Accessibility baseline (keyboard, focus, labels)   | Checklist on shell/login           |




### Phase 2 — Vertical slice (business value first)

Prove the spine end-to-end **before** expanding peripheral screens:

```text
Login → Create Student → Create Course + Fee Plan → Batch
  → Admission approve (or ADMIN waiver) → Enroll
  → Fee account + invoices → Collect payment + Receipt
  → View outstanding
```


| Step | Deliverable                                               | Test                                                                              |
| ---- | --------------------------------------------------------- | --------------------------------------------------------------------------------- |
| 2.1  | People: student CRUD + guardians M:N + addresses          | Integration + authz                                                               |
| 2.2  | Academic: course, fee plan+category, batch                | Installment sum rule                                                              |
| 2.3  | Admissions approve → student link                         | State transition tests                                                            |
| 2.4  | Enrollment activate → finance account/invoices idempotent | Idempotency + outstanding = total                                                 |
| 2.5  | Payment + allocation + receipt + concurrent payment test  | Ledger invariants                                                                 |
| 2.6  | Angular slice UI (responsive)                             | e2e spine + viewports                                                             |
| 2.7  | Audit entries for slice mutations                         | Assert audit rows                                                                 |
| 2.8  | **Multi-tenant isolation** on slice APIs                  | Two institutes seeded; IDOR attempts return 404/403; codes unique per tenant only |




### Phase 3 — Expand People / Academic / Admissions UX

Full lists, search, transfer/withdraw, capacity override, document upload (pre-signed).

### Phase 4 — Finance depth

Refunds, adjustments, aging outstanding, financial year, category reports, statement PDF.

### Phase 5 — Operations

Attendance DRAFT/SUBMITTED/LOCKED; timetable conflicts; exams/components/publish/lock.

### Phase 6 — Documents, import/export, ★ reports

Certificates; CSV import/export; report screens from doc 10.

### Phase 7 — Hardening

Security scan, DR drill doc, performance baseline, responsive QA matrix, OpenAPI gate, notification email MVP.

### Phase 8 — Optional extraction / broker

Extract module only with ADR; add message broker if needed.

---



## 7. Suggested order (summary)

```text
0 Foundations → 1 Identity+Gateway+Shell+Design system
→ 2 Vertical slice (Student…Payment…Outstanding)
→ 3–5 Expand modules → 6 Reports/docs/import → 7 Harden
```

---



## 8. Indicative API surface


| Method   | Path                                            | Module     |
| -------- | ----------------------------------------------- | ---------- |
| POST     | `/api/v1/auth/login`                            | identity   |
| GET/POST | `/api/v1/students`                              | people     |
| POST     | `/api/v1/admissions/applications`               | admissions |
| POST     | `/api/v1/admissions/applications/{id}/approve`  | admissions |
| GET/POST | `/api/v1/courses`                               | academic   |
| POST     | `/api/v1/batches/{id}/enrollments`              | academic   |
| GET      | `/api/v1/finance/accounts?outstandingOnly=true` | finance    |
| POST     | `/api/v1/finance/accounts/{id}/payments`        | finance    |
| GET      | `/api/v1/finance/payments/{id}/receipt`         | finance    |
| POST     | `/api/v1/attendance/sessions`                   | operations |


OpenAPI is the contract (doc 07).

---



## 9. Roles

See **[06-authorization-matrix.md](./06-authorization-matrix.md)** (replaces the abbreviated matrix). Keep that file authoritative.

---



## 10. Risks & decisions


| Topic                     | Decision                                                        |
| ------------------------- | --------------------------------------------------------------- |
| Microservices vs monolith | **Modular monolith** (ADR 0001)                                 |
| DB per service            | **One DB**, module-owned tables (ADR 0002)                      |
| Multi-tenancy             | **Option B — multi-tenant from day one** (ADR 0004)             |
| Institute type            | Coaching/training (ADR 0005)                                    |
| Enrollment → fees         | In-process orchestration + idempotency; events for side effects |
| JWT validation            | Gateway + application authorization                             |
| Money rounding            | HALF_UP scale 2                                                 |
| File storage              | Private object storage + pre-signed URLs                        |
| Guardian model            | First-class + M:N                                               |
| Addresses                 | Separate tables per owner type                                  |
| CSS                       | Design tokens + shared layout; mobile-first                     |


---



## 11. Review checklist

- [x] Service/module boundaries reconsidered → modular monolith  
- [x] Admission first-class  
- [x] Enrollment/finance/attendance/exam rules documented (doc 04)  
- [x] Authorization matrix (doc 06)  
- [x] Multi-tenancy strategy (ADR 0004) — **Option B from day one**  
- [x] Reporting list (doc 10)  
- [x] Backup/DR & CI/CD outlined (doc 09)  
- [x] Stakeholder confirms institute type (ADR 0005) still matches real customer  
- [x] FRONT_DESK payment privileges confirmed  
- [x] Any college/semester needs that would invalidate ADR 0005  

---



## 12. Document history


| Version | Date       | Notes                                                                                          |
| ------- | ---------- | ---------------------------------------------------------------------------------------------- |
| 0.1     | 2026-08-08 | Initial microservices-oriented plan                                                            |
| 0.2     | 2026-08-08 | Responsive UI requirements                                                                     |
| 0.3     | 2026-08-08 | Review uptake: modular monolith, admissions, finance/ops depth, vertical slice, companion docs |
| 0.4     | 2026-08-08 | Multi-tenancy Option B from day one (`institute_id` everywhere)                                |
| 0.5     | 2026-08-08 | Point §6 to doc 11 as authoritative MVP/v1 roadmap                                             |


