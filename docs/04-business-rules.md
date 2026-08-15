# 04 — Business rules & state transitions

**Status:** Draft after architecture review  
**Institute type:** Professional training / coaching (ADR 0005)

Rules are normative. Tests must encode these; UI must not invent conflicting logic.

---

## 0. Multi-tenancy

| Rule | Statement |
| --- | --- |
| BR-TN-01 | Every business write sets `institute_id` from authenticated TenantContext, not from request body |
| BR-TN-02 | Every business read filters by current `institute_id` |
| BR-TN-03 | Looking up a resource by id that belongs to another institute returns **404** (or 403) — never the data |
| BR-TN-04 | Unique business codes are enforced per `(institute_id, code)` |
| BR-TN-05 | `PLATFORM_ADMIN` may manage institutes; accessing institute data requires an explicit support mode (out of band) — normal APIs remain tenant-bound |
| BR-TN-06 | Users belong to exactly one institute in v1 (except platform admins) |

---

## 1. Admission

### Statuses

`DRAFT` → `SUBMITTED` → `PENDING_REVIEW` → `APPROVED` | `REJECTED`  
`SUBMITTED` / `PENDING_REVIEW` → `CANCELLED`

| Rule | Statement |
| --- | --- |
| BR-AD-01 | Only FRONT_DESK / ACADEMIC_COORD / ADMIN create applications |
| BR-AD-02 | APPROVE creates Student if none exists; links application → student |
| BR-AD-03 | REJECTED / CANCELLED require reason (min length 5) |
| BR-AD-04 | One **active** application per (person identity OR phone/email) + course **within the same institute**; duplicates blocked |
| BR-AD-05 | APPROVED application cannot revert to PENDING without ADMIN + audit reason |
| BR-AD-06 | Enquiry is optional; converting enquiry creates application and closes enquiry as CONVERTED |

---

## 2. Enrollment

### Statuses

```text
APPLIED → ACTIVE → COMPLETED
              ↘ SUSPENDED → ACTIVE
              ↘ WITHDRAWN
APPLIED → CANCELLED
```

| Rule | Statement |
| --- | --- |
| BR-EN-01 | Student **may** enroll in multiple courses simultaneously |
| BR-EN-02 | Student **may not** enroll in two **ACTIVE/APPLIED** batches of the **same course** |
| BR-EN-03 | Batch enrollment count cannot exceed `capacity` unless ADMIN override flag + reason |
| BR-EN-04 | Enrollment to ACTIVE requires approved admission for that course **or** ADMIN waiver |
| BR-EN-05 | Activating enrollment triggers fee account + invoice generation (idempotent) |
| BR-EN-06 | WITHDRAWN stops new attendance/exam entry; historical records retained |
| BR-EN-07 | SUSPENDED blocks attendance marking and new invoices; outstanding remains payable |
| BR-EN-08 | COMPLETED only if admin/coordinator confirms; does not auto-waive outstanding |
| BR-EN-09 | Re-enrollment after COMPLETED/WITHDRAWN is a **new** enrollment row |
| BR-EN-10 | CANCELLED applied enrollments generate no fee account (or void if created in error — ADMIN only) |

### Batch transfer (same course)

| Rule | Statement |
| --- | --- |
| BR-TR-01 | Transfer allowed ACTIVE → other batch same course with free capacity (or override) |
| BR-TR-02 | Fee account remains; invoices unchanged by default |
| BR-TR-03 | Fee plan change on transfer requires ACCOUNTANT/ADMIN and creates adjustment/audit |
| BR-TR-04 | Transfer emits domain event `EnrollmentTransferred` |

---

## 3. Fees & finance

### Outstanding formula (source of truth: server)

```text
outstanding =
  SUM(invoices.amount WHERE status NOT IN (CANCELLED, WAIVED))
  - SUM(payments.amount WHERE status = SUCCESS)
  - SUM(adjustments DISCOUNT|SCHOLARSHIP|WAIVER)
  + SUM(adjustments PENALTY)
```

Refunds are **not** a silent negative adjustment type alone — they follow refund lifecycle; completed refunds reduce net collections and update outstanding per BR-RF-*.

| Rule | Statement |
| --- | --- |
| BR-FI-01 | Invoice installment amounts must sum to fee plan total (±0.00) |
| BR-FI-02 | Default allocation: oldest due invoice first; remainder to next |
| BR-FI-03 | Manual allocation allowed for ACCOUNTANT/ADMIN at payment time only |
| BR-FI-04 | Allocation **cannot** be edited after payment SUCCESS; reverse + repay instead |
| BR-FI-05 | Every SUCCESS payment creates a Receipt with unique receipt_no |
| BR-FI-06 | Fee categories required on invoice lines/plans: ADMISSION, TUITION, EXAM, LAB, LIBRARY, HOSTEL, TRANSPORT, OTHER |
| BR-FI-07 | Financial postings stamp `financial_year_id` |
| BR-FI-08 | Optimistic lock on fee account; conflicting concurrent updates retry or fail clearly |
| BR-FI-09 | Idempotency-Key required for collect payment, generate invoices, refund process |
| BR-FI-10 | Cancelling an invoice requires zero allocations; else reverse payments first |

### Refund lifecycle

`REQUESTED` → `APPROVED` → `PROCESSING` → `COMPLETED` | `FAILED`  
`REQUESTED` → `REJECTED`

| Rule | Statement |
| --- | --- |
| BR-RF-01 | Refund amount ≤ net successful payments on account − prior completed refunds |
| BR-RF-02 | APPROVE requires ACCOUNTANT or ADMIN |
| BR-RF-03 | COMPLETED refund updates outstanding and writes finance audit |
| BR-RF-04 | FAILED leaves ledger unchanged except refund status |

### Payment reversal

| Rule | Statement |
| --- | --- |
| BR-PV-01 | Reversal marks payment REVERSED, releases allocations, recalculates invoices/outstanding |
| BR-PV-02 | Original receipt marked cancelled/superseded; not deleted |

---

## 4. Attendance

| Rule | Statement |
| --- | --- |
| BR-AT-01 | Sessions: DRAFT → SUBMITTED → LOCKED |
| BR-AT-02 | Faculty edits only DRAFT/SUBMITTED **own** sessions; ADMIN any |
| BR-AT-03 | LOCKED requires ADMIN to unlock (audited) before edit |
| BR-AT-04 | LATE counts as **present** for percentage |
| BR-AT-05 | EXCUSED counts as **neither** present nor absent (excluded from denominator) **or** as present — **v1 choice: excluded from denominator** |
| BR-AT-06 | Percentage = presentEffective / (presentEffective + ABSENT) where presentEffective = PRESENT + LATE |
| BR-AT-07 | Only students with ACTIVE enrollment in batch on session_date may be marked |
| BR-AT-08 | Mid-course joiners appear from enrollment start date onward |
| BR-AT-09 | Minimum attendance threshold is configurable per course; breach raises warning (not auto-block in v1) |

---

## 5. Examinations & results

| Rule | Statement |
| --- | --- |
| BR-EX-01 | Exam has one or more components (e.g. THEORY, PRACTICAL, INTERNAL) with max/pass marks |
| BR-EX-02 | Marks entry allowed in DRAFT; publish sets PUBLISHED; lock sets LOCKED |
| BR-EX-03 | Published results visible to students; draft not visible |
| BR-EX-04 | Correction after publish requires unlock by ACADEMIC_COORD/ADMIN + audit before/after |
| BR-EX-05 | ABSENT / WITHHELD allowed instead of numeric marks |
| BR-EX-06 | Grace marks: ADMIN/COORD only, audited, within configured cap |
| BR-EX-07 | Re-exam/supplementary = new Exam linked to original (phase 1.1 if needed; stub in domain) |
| BR-EX-08 | Simple pass/fail from totals; GPA **out of scope** for coaching v1 |

---

## 6. People & guardians

| Rule | Statement |
| --- | --- |
| BR-PE-01 | Student code unique and immutable after create |
| BR-PE-02 | Guardian entity unique by phone or email when provided; linked via StudentGuardian |
| BR-PE-03 | Soft-deleted students excluded from new enrollments |
| BR-PE-04 | Same Person cannot be two Student rows; faculty+staff dual role via separate profiles **or** single person — **v1: separate faculty/staff/student tables; dual employment = faculty + staff rows rare; prefer staff designation + faculty profile only when teaching** |

See domain model for Person vs Role clarification.

---

## 7. Documents

| Rule | Statement |
| --- | --- |
| BR-DC-01 | Allowed types: PDF, JPEG, PNG; max size 5 MB (identity docs) |
| BR-DC-02 | Private bucket; download via time-limited signed URL |
| BR-DC-03 | Upload via pre-signed URL preferred; virus scan hook optional in v1 |
| BR-DC-04 | Metadata stores content_type, file_size, checksum, uploaded_by, status |

---

## 8. Notifications (design rules)

| Rule | Statement |
| --- | --- |
| BR-NT-01 | Notifications are channel-abstract; v1 channel = EMAIL |
| BR-NT-02 | Triggers include: admission approved, payment received, fee overdue, result published |
| BR-NT-03 | Delivery attempts recorded; failed retries with backoff |

---

## 9. Audit

| Rule | Statement |
| --- | --- |
| BR-AU-01 | Business-critical transitions listed in security doc must write audit (who, when, before, after, reason, request id) |
| BR-AU-02 | Audit records are append-only |
