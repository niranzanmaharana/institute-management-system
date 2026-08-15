# 03 — Functional requirements

**Status:** Draft after architecture review  
**Depends on:** ADR 0004 (multi-tenancy), ADR 0005 (institute type), `04-business-rules.md`, `05-domain-model.md`  
**Audience:** Product + engineering — must be agreed before coding domain features

---

## 1. Primary end-to-end workflow (v1 spine)

```text
Enquiry (optional)
   ↓
Application / Admission
   ↓
Admission approval
   ↓
Student record active
   ↓
Course + batch selection
   ↓
Enrollment
   ↓
Fee plan selection → Fee account + invoices
   ↓
Payment(s) → Receipt
   ↓
Teaching (timetable)
   ↓
Attendance
   ↓
Examinations & results
   ↓
Enrollment completed / withdrawn
   ↓
Certificates / documents (as applicable)
```

The system must support this path as the first **vertical slice** (see delivery plan).

---

## 2. Capability requirements by area

### 2.0 Multi-tenancy (platform)

| ID | Requirement |
| --- | --- |
| FR-TN-01 | Platform can create/suspend institutes (`PLATFORM_ADMIN`), including contact profile (mobile, admin email, website, address, icon) |
| FR-TN-02 | All institute business data is isolated by `institute_id` |
| FR-TN-03 | JWT includes `institute_id`; APIs never accept tenant override from untrusted input |
| FR-TN-04 | Codes (student, course, invoice, …) unique per institute, not globally |
| FR-TN-05 | Object storage paths are tenant-prefixed |
| FR-TN-06 | Cross-tenant access attempts fail closed (404/403) and are testable |

### 2.1 Identity & access

| ID | Requirement |
| --- | --- |
| FR-ID-01 | Users can log in with username or email + password |
| FR-ID-02 | Access JWT + refresh token lifecycle (login, refresh, logout/revoke) |
| FR-ID-03 | Roles and permissions as per authorization matrix |
| FR-ID-04 | Admin can create users and assign roles; optional link to student/faculty/staff person (`person_type` / `person_id`) |
| FR-ID-05 | Password reset via time-limited token |
| FR-ID-06 | Login success/failure audited |

**Identity implementation notes:** FR-ID-01/02/03/06 and user+role provisioning (FR-ID-04 create/assign) are live (`/api/v1/users`, UI **Users & roles**). Person-link and FR-ID-05 password-reset token remain open.

### 2.2 People

| ID | Requirement |
| --- | --- |
| FR-PE-01 | Create/update/search students with unique student code |
| FR-PE-02 | Create/update/search faculties and staff |
| FR-PE-03 | Maintain addresses (per person type tables) |
| FR-PE-04 | Guardians are first-class; one guardian may link to multiple students |
| FR-PE-05 | Upload/list/download person documents (private object storage) |
| FR-PE-06 | Soft-deactivate people without destroying history |

### 2.3 Admissions

| ID | Requirement |
| --- | --- |
| FR-AD-01 | Capture enquiry (optional) with contact + interest course |
| FR-AD-02 | Create admission application with status workflow |
| FR-AD-03 | Approve/reject/cancel application with reason |
| FR-AD-04 | On approval, create Student (if new) and optionally draft enrollment |
| FR-AD-05 | Prevent duplicate active applications for same person+course (rule in BR) |

**Admissions implementation notes:** Enquiry is optional (`OPEN` / `CONVERTED` / `CLOSED`). Applications start as `SUBMITTED`; coordinators/admins approve or reject; front desk can cancel. Duplicate `SUBMITTED` applications for the same phone or email + course are blocked (BR-AD-04). Reject/cancel reasons require at least 5 characters (BR-AD-03). Enrollment after approve is a separate step (`enrollment:write` for FRONT_DESK).

### 2.4 Academic

| ID | Requirement |
| --- | --- |
| FR-AC-01 | Manage departments, courses, subjects, prerequisites |
| FR-AC-02 | Manage course fee **catalog** plans + installments + fee categories |
| FR-AC-03 | Manage academic years and batches (capacity, status) |
| FR-AC-04 | Enroll student into batch with fee plan selection |
| FR-AC-05 | Transfer enrollment between batches (same course) with fee impact rules |
| FR-AC-06 | Withdraw / suspend / complete enrollment |
| FR-AC-07 | Assign faculty to batch/subject |
| FR-AC-08 | Admin override of batch capacity (audited) |

### 2.5 Finance

| ID | Requirement |
| --- | --- |
| FR-FI-01 | On enrollment activation, create fee account + invoices from plan (idempotent) |
| FR-FI-02 | Record payments with method + reference; allocate to invoices |
| FR-FI-03 | Issue printable **receipt** per successful payment |
| FR-FI-04 | Maintain denormalized outstanding balance; expose list/statement APIs |
| FR-FI-05 | Discounts/scholarships/waivers/penalties as adjustments with approval trail |
| FR-FI-06 | Refund lifecycle: request → approve → process → complete/fail |
| FR-FI-07 | Payment reversal with audit; cannot silently delete financial history |
| FR-FI-08 | Financial year master separate from academic year |
| FR-FI-09 | Concurrent payments on same account must not corrupt ledger |

### 2.6 Operations — attendance

| ID | Requirement |
| --- | --- |
| FR-AT-01 | Create attendance session for batch/date/subject |
| FR-AT-02 | Mark PRESENT / ABSENT / LATE / EXCUSED per enrolled student |
| FR-AT-03 | Session statuses: DRAFT → SUBMITTED → LOCKED |
| FR-AT-04 | Edit rules per BR (who, when); corrections audited |
| FR-AT-05 | Attendance % with explicit LATE/EXCUSED counting rules |
| FR-AT-06 | Faculty marks only assigned batches (unless admin) |

### 2.7 Operations — timetable & exams

| ID | Requirement |
| --- | --- |
| FR-TT-01 | Maintain rooms and timetable slots; reject room/faculty conflicts |
| FR-EX-01 | Define exam with subjects, max marks, pass marks, schedule |
| FR-EX-02 | Enter theory/practical/internal components where configured |
| FR-EX-03 | Result states: draft entry → published → locked |
| FR-EX-04 | Support ABSENT / WITHHELD on results |
| FR-EX-05 | Students see own published results only |

### 2.8 Documents & certificates

| ID | Requirement |
| --- | --- |
| FR-DC-01 | Generate/download fee receipt PDF |
| FR-DC-02 | Templates for bonafide / completion (v1 minimal set) |
| FR-DC-03 | Private document storage with signed download URLs |

### 2.9 Communication (phase 2 depth; design now)

| ID | Requirement |
| --- | --- |
| FR-NT-01 | Channel-abstracted notifications (EMAIL first; SMS/WhatsApp later) |
| FR-NT-02 | Templates, triggers, delivery status, retry |

### 2.10 Reporting & import/export

| ID | Requirement |
| --- | --- |
| FR-RP-01 | Reports listed in `10-reporting-requirements.md` (v1 subset online) |
| FR-IE-01 | CSV import students; export students/fees/attendance |
| FR-IE-02 | Bulk marks upload; async job if file large |

### 2.11 UX / platform

| ID | Requirement |
| --- | --- |
| FR-UX-01 | Responsive on mobile, tablet, laptop (see doc 01 §3.13 & standards) |
| FR-UX-02 | Every list/detail screen supports loading / empty / error / forbidden states |
| FR-UX-03 | WCAG 2.2 AA target for primary flows |
| FR-UX-04 | Design system components mandatory before feature UIs |

---

## 3. Explicit non-requirements (v1)

- Native mobile apps  
- Full LMS / video  
- Payroll / HRIS  
- Commercial SaaS billing **to** institutes (charging tenants) — **multi-tenant data isolation is in scope**  
- Hostel/transport operational management (fee category codes reserved only)  
- Semester promotion / GPA (unless ADR 0005 revised)

---

## 4. Traceability

Functional IDs are referenced from business rules (`BR-*`), reports (`RP-*`), and delivery steps.
