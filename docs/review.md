## Response to this review (applied 2026-08-08)

Agreed with the core recommendation: **do not start coding yet**; complete the domain/requirements pass first.

| Review theme | Action taken |
| --- | --- |
| Architecture-first gap / workflows | Added `03`, `04`, `05`; admission spine documented |
| Microservices vs monolith | **Accepted modular monolith** — ADR 0001; doc 01 rewritten |
| Multi-tenancy | **Option B** — multi-tenant from day one (revised per product decision) — ADR 0004 |
| Institute type | Coaching/training v1 — ADR 0005 (confirm with stakeholder) |
| Admission first-class | Admissions module + schema |
| Guardian M:N | Domain + SQL in docs 01/05 |
| Finance receipts/refunds/categories/FY | Doc 01 schema + doc 04 rules |
| Attendance/exam depth | Statuses, components, BRs |
| Authz matrix / events / reports / ops / API / security | Docs `06`–`10` |
| Vertical slice delivery | Phase 2 in doc 01 |
| Coding standards gaps | Doc 02 v0.3 (design system, UX states, WCAG, ArchUnit, OpenAPI-first) |

**Still needs human confirmation before coding:** ADR 0005 matches the real institute; FRONT_DESK payment scope; any college/semester needs.  
**Resolved:** Multi-tenancy = Option B from day one.

Index: [README.md](./README.md)

---

## My overall assessment

| Area                        | Assessment                                            |
| --------------------------- | ----------------------------------------------------- |
| Product scope               | 🟢 Good                                               |
| Module breakdown            | 🟢 Good, but some modules missing                     |
| Finance design              | 🟢 Very good foundation                               |
| Service boundaries          | 🟡 Needs reconsideration                              |
| Database design             | 🟢 Good starting point, some important gaps           |
| Security                    | 🟢 Good foundation, needs privacy/operational details |
| Angular standards           | 🟢 Very good                                          |
| Testing                     | 🟢 Very good                                          |
| CI/CD & deployment          | 🟡 Needs more detail                                  |
| Audit/history               | 🟡 Needs expansion                                    |
| Multi-tenancy               | 🟡 Decision should be made earlier                    |
| Reporting                   | 🟡 Too lightly defined                                |
| Requirements/business rules | 🔴 Biggest gap                                        |

---

# 1. Biggest recommendation: define the actual institute workflow first

The documents are currently **architecture-first**.

You have defined:

> Student → Enrollment → Fee Account → Invoice → Payment

and:

> Batch → Faculty → Attendance → Exam → Result

That's good, but before implementation I would document the **real-life workflows**.

For example:

### Admission workflow

```text
Enquiry
   ↓
Application
   ↓
Admission approval
   ↓
Student registration
   ↓
Course selection
   ↓
Batch allocation
   ↓
Fee plan selection
   ↓
Invoice generation
   ↓
Payment
   ↓
Student active
```

Your current model starts almost directly at **Student + Enrollment**.

You should decide whether you need:

* Enquiry/lead
* Application
* Admission
* Admission approval/rejection
* Registration
* Enrollment
* Transfer between batches
* Course change
* Withdrawal
* Re-admission

This is important because otherwise `students` and `enrollments` will eventually become overloaded with admission-state logic.

---

# 2. Add a proper Requirements / Business Rules document

I strongly recommend adding:

```text
03-functional-requirements.md
04-business-rules.md
05-domain-model.md
```

The current delivery document says what modules exist, but not enough about **how the institute actually operates**.

For example:

### Enrollment rules

You should explicitly decide:

* Can a student enroll in multiple courses simultaneously?
* Can they enroll in two batches of the same course?
* Can a student transfer batches?
* What happens to fees when transferring?
* Can an enrollment be cancelled?
* Can an enrollment be suspended?
* Can completed students be re-enrolled?
* Can a batch exceed capacity with admin override?

You already have capacity and duplicate-enrollment tests planned. 

But the **business rules need to exist independently of the tests**.

---

# 3. Admission should probably be its own concept

This is one of the biggest domain gaps I see.

Currently:

```text
people-service
    Student
        ↓
academic-service
    Enrollment
```

I would consider:

```text
Admission
   ↓
Student
   ↓
Enrollment
```

An admission can have:

```text
APPLICATION
PENDING
APPROVED
REJECTED
CANCELLED
```

Then:

```text
Student
   ↓
Enrollment
   ↓
Batch
```

This makes the system much easier to extend later.

---

# 4. Consider Student Guardian/Family as a first-class model

You currently have guardians, which is good.

But I would expand the domain slightly.

Instead of only:

```text
guardian
student_id
name
relation
phone
email
```

consider whether you need:

```text
Student
   ↕
StudentGuardian
   ↓
Guardian
```

because the same parent/guardian may have multiple students in the institute.

For example:

```text
Parent A
 ├── Student 001
 └── Student 002
```

That becomes especially useful for:

* fee communication
* attendance notifications
* exam results
* announcements
* multiple children
* parent login later

---

# 5. Reconsider the microservice count

This is probably the **most important architectural discussion**.

Your current architecture has:

```text
Gateway
Identity
Academic
People
Finance
Operations
Notification
Reporting
```

with separate databases per service. 

Technically this is clean.

But for an institute management system, I would seriously consider starting with a **modular monolith** rather than deploying five independent business services immediately.

For example:

```text
Angular
   ↓
API Gateway
   ↓
Institute Management Application
   ├── Identity
   ├── People
   ├── Admissions
   ├── Academic
   ├── Finance
   ├── Attendance
   ├── Examination
   ├── Communication
   └── Reporting
```

with strict module boundaries.

Why?

Your own coding document already emphasizes loose coupling, ports, domain boundaries, and avoiding cross-module dependencies. 

You can preserve those boundaries **without immediately paying the operational cost of microservices**.

Later:

```text
Modular Monolith
       ↓
identify high-load/high-change module
       ↓
extract Finance
       ↓
extract Notification
       ↓
extract Reporting
```

This would also make development, debugging, local setup, deployment and transactions considerably simpler.

**If your expected scale is one/few institutes rather than thousands of tenants, I would strongly prefer this approach for v1.**

---

# 6. Multi-tenancy decision needs to happen earlier

The document says:

> multi-tenant-ready

but also:

> v1 single institute

and recommends adding `institute_id` later. 

I would **not leave this ambiguous**.

Choose one of these explicitly:

### Option A — Single institute

No `institute_id`.

Simplest.

### Option B — Multi-tenant from day one

Every business entity has:

```text
institute_id
```

and every query is tenant-aware.

### Option C — Architecture-ready, but single tenant

Design module boundaries so tenancy can be introduced later, but don't pretend the current database is safely multi-tenant.

I recommend **C** unless SaaS is actually the immediate product goal.

If you eventually add multi-tenancy, you don't want to discover that:

```sql
SELECT * FROM students;
```

was used everywhere and tenant isolation wasn't designed into repositories/services.

---

# 7. Finance needs a few more concepts

The finance section is one of the strongest parts of the document.

The separation between:

```text
Course fee catalog
        ↓
Student fee account
        ↓
Invoice
        ↓
Payment
        ↓
Payment allocation
```

is good.

Your outstanding calculation and the requirement to update the denormalized balance transactionally are also well thought out. 

However, I would add:

### Payment receipt

```text
Payment
   ↓
Receipt
```

with:

* receipt number
* generated date
* payment method
* transaction reference
* collected by
* printable/downloadable receipt
* cancellation/reversal status

### Payment allocation rules

You already have `payment_allocations`.

Define explicitly:

```text
Payment ₹5,000
    ↓
Invoice 1 → ₹3,000
Invoice 2 → ₹2,000
```

and decide whether allocation can be changed after payment.

### Refund lifecycle

Don't leave refund as just an adjustment type.

Define:

```text
Refund requested
     ↓
Approved
     ↓
Processed
     ↓
Completed / Failed
```

### Financial period

Consider:

```text
financial_year
```

separate from:

```text
academic_year
```

These are not necessarily the same.

---

# 8. Add fee categories

Currently fee plans are fairly generic.

I would consider:

```text
ADMISSION_FEE
TUITION_FEE
EXAM_FEE
LAB_FEE
LIBRARY_FEE
HOSTEL_FEE
TRANSPORT_FEE
OTHER
```

Then invoices can have a category.

This will dramatically improve reporting later:

```text
Total tuition collection
Total exam fee
Total admission fee
Outstanding tuition
Outstanding exam fee
```

---

# 9. Attendance needs more business rules

Current model:

```text
attendance_session
attendance_record
```

is good. 

But define:

* Can attendance be edited after submission?
* Who can edit?
* Can faculty edit only their own sessions?
* Can admin override?
* Attendance correction workflow?
* Attendance locking?
* What counts toward attendance percentage?
* Does `LATE` count as present?
* Does `EXCUSED` count?
* Minimum attendance requirement?
* Can attendance be marked for a student who joined mid-course?

I'd also add:

```text
attendance_session.status
```

such as:

```text
DRAFT
SUBMITTED
LOCKED
```

This prevents accidental modification of historical attendance.

---

# 10. Examination needs more depth

Current model is:

```text
Exam
 ↓
Exam Result
```

Good starting point, but likely insufficient for real institutes.

Consider:

```text
Exam
 ├── Exam subjects
 ├── Maximum marks
 ├── Passing marks
 ├── Weightage
 ├── Exam schedule
 └── Results
```

You may eventually need:

* internal assessment
* practical marks
* theory marks
* grace marks
* absent
* withheld
* re-exam
* supplementary exam
* grade scale
* GPA/percentage
* result publication
* result correction
* result locking

I'd define this before implementing the Operations DB.

---

# 11. Add academic progression

This is currently missing.

Consider:

```text
Academic progression

Student
   ↓
Enrollment
   ↓
Semester/Term
   ↓
Promotion
   ↓
Completion
```

Depending on the type of institute, you may need:

* semester
* term
* academic level/year
* promotion
* repeat
* backlog
* completion
* certificate

Your current `academic_year` + `batch` model may be enough for a coaching/training institute, but potentially insufficient for a college/school-like institute.

This is something I'd settle based on the actual institute type.

---

# 12. Add certificate/document generation

I would add a module such as:

```text
Documents / Certificates
```

Examples:

* admission receipt
* fee receipt
* bonafide certificate
* course completion certificate
* marksheet
* transfer certificate
* ID card

This will become a common requirement for institute management.

---

# 13. Add document storage architecture explicitly

You already store:

```text
storage_key
```

for documents.

And the delivery plan recommends object storage/MinIO locally. 

Good.

But define:

```text
Angular
   ↓
Backend
   ↓
Object Storage
```

and whether you use:

* direct upload using pre-signed URLs
* backend proxy upload
* virus scanning
* file type restrictions
* maximum size
* retention
* private/public access
* signed download URLs

For student identity documents, this is particularly important.

---

# 14. Add audit history beyond finance

You have finance audit and login audit.

I recommend a general:

```text
audit-service / audit module
```

or consistent audit tables.

Important actions:

```text
Student created
Student updated
Student deleted
Enrollment created
Enrollment transferred
Fee plan changed
Invoice cancelled
Payment created
Payment reversed
Refund approved
Attendance modified
Marks changed
Result published
User role changed
Password reset
```

For sensitive changes, record:

```text
who
what
when
before
after
reason
IP/device where appropriate
```

Your standards already require audit trails for money and role changes. 

I'd expand that requirement to **all business-critical state transitions**.

---

# 15. Add explicit workflow/status design

I see many enums:

```text
ACTIVE
INACTIVE
SUSPENDED
COMPLETED
CANCELLED
...
```

That's fine, but I recommend documenting **allowed state transitions**.

For example:

```text
ENROLLMENT

APPLIED
   ↓
ACTIVE
   ├── SUSPENDED
   │      ↓
   │    ACTIVE
   │
   ├── WITHDRAWN
   │
   └── COMPLETED
```

Then define:

```text
Who can perform transition?
What validations are required?
Can it be reversed?
Does it generate an event?
Does it affect finance?
```

This will prevent a lot of controller-level business logic.

---

# 16. Add notifications as a proper domain capability

Phase 2 is reasonable.

But don't model it only as:

```text
Email/SMS
```

I'd define:

```text
Notification
 ├── Template
 ├── Channel
 ├── Recipient
 ├── Trigger
 ├── Status
 ├── Retry
 └── Delivery history
```

Eventually:

```text
EMAIL
SMS
WHATSAPP
PUSH
```

You don't need all of them in v1.

But the domain should support channel abstraction.

---

# 17. Reporting should not be an afterthought

You currently defer reporting to phase 2. That's reasonable architecturally, but I'd define the **required reports now**.

For example:

### Student

* student list
* active students
* new admissions
* dropout/withdrawal
* batch-wise students

### Finance

* daily collection
* payment method collection
* outstanding fees
* overdue invoices
* student ledger
* refund report
* discounts/scholarships

### Attendance

* student attendance
* batch attendance
* low attendance
* faculty attendance

### Academic

* batch performance
* exam results
* pass/fail
* subject performance

This will influence database indexes and APIs.

---

# 18. Add import/export

Very useful for institutes.

I'd put:

```text
Data Import / Export
```

Examples:

```text
Import students from Excel/CSV
Import faculty
Bulk enrollment
Bulk marks upload
Bulk attendance
Export students
Export fees
Export attendance
Export results
```

For bulk operations, define asynchronous processing if files can be large.

---

# 19. Add backup and disaster recovery

This is a significant missing operational requirement.

You have:

* logging
* tracing
* migrations
* testing

but I don't see enough around:

```text
Backup
Restore
Disaster Recovery
```

Define:

* database backup frequency
* object storage backup
* retention
* restore procedure
* RPO
* RTO
* backup encryption
* restore testing

For an institute system, **financial data and student documents cannot depend only on database redundancy**.

---

# 20. Add CI/CD and environment architecture

The coding document mentions CI build/test/lint/migration checks. 

I'd add an explicit environment diagram:

```text
Developer
   ↓
Local
   ↓
CI
   ↓
Development
   ↓
QA / Test
   ↓
Staging
   ↓
Production
```

And define:

```text
Who can deploy?
How migrations run?
How secrets are managed?
Rollback strategy?
Database rollback strategy?
Container versioning?
Health checks?
Readiness/liveness?
```

---

# 21. Add health/readiness endpoints

Every backend service should have:

```text
/actuator/health
```

and ideally:

```text
liveness
readiness
```

Also define:

* startup failure behavior
* DB unavailable behavior
* downstream service unavailable behavior
* graceful shutdown

This becomes important once you have multiple services.

---

# 22. Add API idempotency more formally

You already have a good rule:

> mutating POSTs that may be retried accept `Idempotency-Key`

especially payments and enrollment. 

I'd make this a concrete design:

```text
idempotency_key
request_hash
user_id
operation
response_status
response_body
expires_at
```

This is particularly important for:

```text
Collect payment
Create enrollment
Generate invoice
Refund
```

Otherwise a network retry can accidentally create duplicate financial records.

---

# 23. Add concurrency rules to finance

You already mention optimistic locking for fee accounts. 

I'd explicitly test this scenario:

```text
Account outstanding = ₹10,000

User A → pays ₹7,000
User B → simultaneously pays ₹5,000
```

The system must prevent the resulting ledger from becoming inconsistent.

This deserves an integration/concurrency test.

---

# 24. Clarify `ims-common`

You correctly warn against making it a dumping ground. 

I would go even further.

Prefer:

```text
ims-common
 ├── Error contract
 ├── Trace utilities
 ├── Common pagination
 └── perhaps security primitives
```

Avoid:

```text
StudentDTO
CourseEntity
FinanceEntity
BusinessService
FeeCalculator
```

The second category destroys service/module independence.

---

# 25. Add OpenAPI-first API design

You already require OpenAPI, which is good.

I'd make it:

```text
API contract
    ↓
OpenAPI
    ↓
contract validation
    ↓
implementation
```

rather than letting the implementation become the implicit API contract.

Also consider generating Angular API clients from OpenAPI rather than manually maintaining every HTTP DTO/client.

---

# 26. Angular architecture is already quite good

I don't see major architectural problems here.

The rules around:

* standalone components
* Signals
* feature isolation
* responsive design
* typed forms
* lazy routes
* Playwright
* mobile/tablet/desktop testing

are strong. 

One thing I'd add is a formal **design system**:

```text
Button
Input
Select
DatePicker
Modal
Drawer
Table
Card
EmptyState
LoadingState
ErrorState
ConfirmationDialog
Toast
Pagination
```

You already mention shared UI primitives and responsive data views in the delivery plan. 

I'd make those components mandatory before feature teams start building screens.

---

# 27. Add UX states to every screen

Don't only define:

```text
Loading → Data
```

Every screen should handle:

```text
Loading
Empty
Success
Validation error
Authorization error
Not found
Server error
Network error
Partial data
```

For example:

### Student list

```text
Loading students...
No students found
Could not load students
You don't have permission
Retry
```

This should be part of your Definition of Done.

---

# 28. Add accessibility as a formal acceptance criterion

You mention accessibility, semantic HTML, keyboard focus and contrast. 

I'd explicitly target:

```text
WCAG 2.2 AA
```

or whatever level you decide.

Then include:

* keyboard-only navigation
* focus management
* screen reader labels
* color contrast
* accessible error messages
* modal focus trap
* table accessibility

---

# 29. Database improvements I'd make before coding

A few specific things deserve another pass.

### `addresses`

You currently use:

```text
owner_type
owner_id
```

This is a polymorphic association.

It's flexible but loses FK integrity.

For a system like this, I'd consider:

```text
student_addresses
faculty_addresses
staff_addresses
```

or a proper `persons` model if students/faculty/staff are conceptually people.

### `students`

Consider whether:

```text
first_name
middle_name
last_name
```

is sufficient for your institute's actual naming requirements.

Also consider:

* nationality
* blood group if actually required
* category
* identification numbers
* admission number
* preferred language

But **only add fields based on actual requirements**, not because they are common.

### `documents`

Add:

```text
content_type
file_size
checksum
version
status
uploaded_by
```

if document management matters.

---

# 30. One important conceptual improvement: separate "Person" from "Role"

You currently have:

```text
students
faculties
staff
```

and Identity has:

```text
person_type
person_id
```

This works.

But consider whether a person can have multiple relationships with the institute.

Example:

```text
Person
 ├── Faculty
 ├── Staff
 └── User
```

or:

```text
Faculty + Administrator
```

Your current model can potentially represent this through roles, but the person model should be explicit about what is a **person**, what is an **institute role**, and what is an **application identity**.

I'd document this relationship before implementation.

---

# 31. Add authorization matrix

You currently have roles:

```text
ADMIN
ACCOUNTANT
ACADEMIC_COORDINATOR
FACULTY
STUDENT
FRONT_DESK
```

and permissions such as:

```text
fee:write
student:read
```

Good.

But create a matrix like:

| Capability      | Admin | Accountant | Academic | Faculty | Front Desk | Student |
| --------------- | ----: | ---------: | -------: | ------: | ---------: | ------: |
| Create student  |     ✓ |            |          |         |          ✓ |         |
| Edit student    |     ✓ |            |        ✓ |         |          ✓ |         |
| Collect payment |     ✓ |          ✓ |          |         |          ✓ |         |
| View own fees   |     ✓ |          ✓ |          |         |            |       ✓ |
| Mark attendance |     ✓ |            |          |       ✓ |            |         |
| Enter marks     |     ✓ |            |        ✓ |       ✓ |            |         |
| Publish results |     ✓ |            |        ✓ |         |            |         |
| Manage users    |     ✓ |            |          |         |            |         |

This will reveal authorization gaps **before coding**.

---

# 32. Add business-event catalogue

You're already planning domain events/outbox later. 

Define events now:

```text
StudentCreated
AdmissionApproved
EnrollmentCreated
EnrollmentCancelled
InvoiceGenerated
PaymentReceived
PaymentReversed
RefundProcessed
AttendanceSubmitted
ExamResultPublished
StudentGraduated
```

Then document:

```text
Producer
Consumers
Payload
Retry
Idempotency
Ordering requirements
```

You don't have to implement Kafka/RabbitMQ now.

---

# 33. Delivery plan: add vertical slices

Your current delivery plan is technically good:

> unit → integration → contract → e2e

and each step has deliverable/test/exit criteria. 

But I'd change some delivery sequencing to demonstrate **real business value earlier**.

Instead of building:

```text
Identity
↓
People
↓
Academic
↓
Finance
↓
Operations
```

consider an early vertical slice:

```text
Login
 ↓
Create Student
 ↓
Create Course
 ↓
Create Batch
 ↓
Enroll Student
 ↓
Generate Fee
 ↓
Collect Payment
 ↓
View Outstanding
```

That proves your most important business workflow end-to-end very early.

Then expand.

---

# 34. What I would add to your document set

I would evolve your current two documents into:

```text
docs/
│
├── 01-product-and-module-plan.md
├── 02-coding-standards.md
│
├── 03-functional-requirements.md       ← ADD
├── 04-business-rules.md                ← ADD
├── 05-domain-model.md                  ← ADD
├── 06-authorization-matrix.md          ← ADD
├── 07-api-guidelines.md                ← ADD
├── 08-security-and-privacy.md           ← ADD
├── 09-deployment-and-operations.md      ← ADD
├── 10-reporting-requirements.md         ← ADD
│
└── adr/
    ├── 0001-architecture.md
    ├── 0002-database-strategy.md
    ├── 0003-authentication.md
    └── 0004-multi-tenancy.md
```

You already have the beginnings of ADR discipline in the coding standards, which is good. 

---

# My priority ranking

If I were reviewing this before approving development, I'd classify the changes like this:

### 🔴 Must resolve before coding

1. **Define the actual institute type/use case**
2. **Admission/application workflow**
3. **Enrollment lifecycle**
4. **Academic progression model**
5. **Finance lifecycle + refund/reversal rules**
6. **Authorization matrix**
7. **Single institute vs future multi-tenancy strategy**
8. **Microservices vs modular monolith decision**
9. **Exam/result business rules**
10. **Audit requirements**

### 🟠 Should resolve before Phase 1

11. Student/guardian model
12. Document storage/security
13. Notification architecture
14. Reporting requirements
15. Import/export requirements
16. Backup/restore/DR
17. CI/CD/environment strategy
18. API contract strategy
19. State transition rules
20. Concurrency/idempotency rules

### 🟢 Already strong

* Angular responsive strategy
* Java/Spring layering
* SOLID guidance
* DTO/API separation
* database migration discipline
* finance ownership
* outstanding balance concept
* Testcontainers
* Playwright
* OpenAPI
* tracing/correlation IDs
* JWT validation
* API Gateway
* coding anti-patterns
* Definition of Done
* mobile/tablet/laptop acceptance criteria

The coding standards are particularly solid: the inward dependency rule, explicit layering, no cross-service entity sharing, migration discipline, JWT validation, timeout/resilience rules, and testing expectations give the project a good engineering foundation.  

## Bottom line

**I'd rate the current documents around 8/10 as an engineering foundation, but only around 6.5/10 as a complete product specification.**

The missing part isn't more technology. You already have plenty of technology defined.

The next step should be to answer:

> **"Exactly how does an institute operate from admission → enrollment → teaching → attendance → examination → fees → completion?"**

Once that workflow is nailed down, the architecture and database can be adjusted around the actual business rather than the other way around.

**Most importantly, I would settle the microservices/modular-monolith decision before creating all those service repositories and databases.** Your current documents have excellent boundaries, so those boundaries can be retained even if the first deployment is a modular monolith.
