# 05 — Domain model

**Status:** Draft after architecture review  
**Ubiquitous language** for the modular monolith modules.

---

## 1. Context map (modules)

```text
platform/institutes (tenancy root)
identity ──belongs to──► institute (users.institute_id)
people / admissions / academic / finance / operations / …
   └── all aggregates carry institute_id
```

```text
identity ──links──► people (person_id) [same institute]
admissions ──creates──► people.Student
admissions ──feeds──► academic.Enrollment
academic.Enrollment ──activates──► finance.FeeAccount
academic ──catalog fees──► finance (copy plan snapshot at enrollment)
operations ──reads──► academic (batch/enrollment ids), people (student/faculty ids)
documents ──owned by──► people / finance / academic artifacts
notification ──subscribes──► domain events
reporting ──reads──► projections / queries (prefer read models)
```

**Rules:** Module A never writes Module B’s tables. **No cross-tenant joins or reads.**

### Institute (tenant root)

- **Institute** — `code`, `name`, `status`, `timezone`
- All other aggregates reference `institute_id`

---

## 2. Person vs role vs identity

| Concept | Meaning | Persistence |
| --- | --- | --- |
| **Identity User** | Login credential + roles/permissions | `users`, `roles`, … |
| **Student / Faculty / Staff** | Institute relationship (role-in-institute) | `students`, `faculties`, `staff` |
| **Guardian** | External person related to students | `guardians` + `student_guardians` |

v1 does **not** introduce a unified `persons` table (avoids big-bang refactor). Dual teaching+admin is handled by **User roles** + optional faculty profile.

Future: optional `persons` consolidation if parent login / unified CRM needs it.

---

## 3. Core aggregates

### Admissions

- **Enquiry** — optional lead  
- **AdmissionApplication** — status workflow; references course interest; on approve → Student  

### People

- **Student**, **Faculty**, **Staff**
- **Guardian** + **StudentGuardian** (many-to-many)
- **StudentAddress** / **FacultyAddress** / **StaffAddress** (separate tables; FK integrity)
- **PersonDocument** metadata → object storage

### Academic

- **Department**, **Course**, **Subject**, **CoursePrerequisite**
- **CourseFeePlan** + **CourseFeeInstallment** + **FeeCategory**
- **AcademicYear**, **Batch**
- **Enrollment** (student_id, batch_id, course_id, fee_plan_id, status)
- **BatchFacultyAssignment**

### Finance

- **FinancialYear**
- **StudentFeeAccount** (enrollment_id, outstanding snapshot, version)
- **Invoice** (+ category)
- **Payment** + **PaymentAllocation** + **Receipt**
- **FeeAdjustment**
- **RefundRequest**
- **FinanceAudit** + general **AuditLog** (platform)
- **IdempotencyRecord**

### Operations

- **Room**, **CalendarHoliday**, **TimetableSlot**
- **AttendanceSession** (status) + **AttendanceRecord**
- **Exam** + **ExamComponent** + **ExamResult** (+ component marks)

### Documents / certificates

- **CertificateTemplate**, **GeneratedDocument**

---

## 4. Key relationships

```text
Guardian 1──* StudentGuardian *──1 Student

AdmissionApplication 0..1──1 Student (after approve)
Student 1──* Enrollment *──1 Batch *──1 Course

Enrollment 1──1 StudentFeeAccount 1──* Invoice
StudentFeeAccount 1──* Payment 1──1 Receipt
Payment 1──* PaymentAllocation *──1 Invoice

Batch 1──* AttendanceSession 1──* AttendanceRecord
Batch 1──* Exam 1──* ExamComponent
Exam 1──* ExamResult
```

---

## 5. Fee catalog vs ledger

| Catalog (Academic) | Ledger (Finance) |
| --- | --- |
| CourseFeePlan definition | Snapshot amounts onto invoices at enrollment |
| Editable for future enrollments | Historical invoices immutable except cancel/reverse flows |

Finance stores **copied** amounts/categories; does not join live to catalog for balance math.

**Operational walkthrough** (entity map, management UI, lifecycle): [12-course-fee-structure-and-relationships.md](./12-course-fee-structure-and-relationships.md).

---

## 6. Domain events catalogue (define now; bus later)

| Event | Producer | Likely consumers |
| --- | --- | --- |
| `StudentCreated` | people | notification, reporting |
| `AdmissionApproved` | admissions | people, academic, notification |
| `EnrollmentCreated` | academic | finance |
| `EnrollmentActivated` | academic | finance, notification |
| `EnrollmentTransferred` | academic | operations, reporting |
| `EnrollmentCancelled` | academic | finance |
| `InvoiceGenerated` | finance | notification |
| `PaymentReceived` | finance | notification, reporting |
| `PaymentReversed` | finance | reporting |
| `RefundProcessed` | finance | notification |
| `AttendanceSubmitted` | operations | notification |
| `ExamResultPublished` | operations | notification, people |
| `StudentGraduated` / `EnrollmentCompleted` | academic | documents, notification |

v1 transport: **Spring application events** (+ transactional outbox table prepared). External broker in phase 2+.

Payloads: IDs + minimal denormalized fields; consumers are idempotent.

---

## 7. Academic progression (v1)

For coaching/training:

```text
Enrollment ACTIVE → (learning) → COMPLETED | WITHDRAWN
```

No semester promotion aggregate in v1. Certificate generation may follow COMPLETED.
