# 06 — Authorization matrix

**Status:** Draft after architecture review  
**Legend:** RW = create/update/delete (as allowed by BR) · R = read · — = none · S = self only · A = assigned batch/course only

Permissions are enforced as authorities (examples: `student:write`, `fee:collect`). Roles are bundles of permissions.

---

## 1. Roles

| Role | Typical users |
| --- | --- |
| PLATFORM_ADMIN | Platform operator — create/suspend institutes only |
| ADMIN | Institute administrator |
| ACCOUNTANT | Fee collection & refunds |
| ACADEMIC_COORDINATOR | Courses, batches, exams publish |
| FACULTY | Teaching, attendance, marks entry |
| FRONT_DESK | Admissions, student registration |
| STUDENT | Own data |
| GUARDIAN *(phase 2)* | Linked students — reserved |

All institute roles operate **only within their JWT `institute_id`**.

**Implemented:** Institute ADMIN provisions users via `user:manage` (`/api/v1/users`, `/api/v1/roles`, UI **Users & roles**). Assignable roles exclude `PLATFORM_ADMIN`. Staff/Faculty people records are separate from login users. Optional enquiries and applications are under `admission:write` / `admission:approve`. FRONT_DESK enrolls with `enrollment:write` (not course catalog write).

---

## 2. Capability matrix

| Capability | PLATFORM_ADMIN | ADMIN | ACCOUNTANT | ACADEMIC_COORD | FACULTY | FRONT_DESK | STUDENT |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Create/suspend institute | RW | — | — | — | — | — | — |
| Manage users/roles (own institute) | — | RW | — | — | — | — | — |
| Institute settings (own) | — | RW | — | R | — | R | — |
| Create/edit student | — | RW | R | RW | R (A) | RW | S |
| View student | — | RW | R | RW | R (A) | RW | S |
| Manage guardians | — | RW | R | R | — | RW | — |
| Manage faculty/staff | — | RW | R | RW | S | R | — |
| Enquiry/application create | — | RW | — | RW | — | RW | — |
| Approve/reject admission | — | RW | — | RW | — | — | — |
| Manage courses/fee catalog | — | RW | R | RW | R | R | R (published) |
| Manage batches | — | RW | R | RW | R (A) | R | R (own) |
| Enroll student | — | RW | R | RW | — | RW | — |
| Transfer/withdraw enrollment | — | RW | R | RW | — | R | — |
| Capacity override | — | RW | — | — | — | — | — |
| View fee catalog | — | RW | RW | RW | R | R | R |
| Collect payment | — | RW | RW | — | — | RW* | — |
| View any fee ledger | — | RW | RW | R | — | R* | — |
| View own fees/statement | — | RW | RW | R | — | R | S |
| Issue refund approve/process | — | RW | RW | — | — | — | — |
| Adjust discount/waiver | — | RW | RW | — | — | — | — |
| Cancel invoice / reverse payment | — | RW | RW | — | — | — | — |
| Mark attendance | — | RW | — | R | RW (A) | — | — |
| Unlock attendance session | — | RW | — | RW | — | — | — |
| View own attendance | — | RW | — | R | R (A) | — | S |
| Manage timetable | — | RW | — | RW | R (A) | R | R (own) |
| Enter exam marks | — | RW | — | RW | RW (A) | — | — |
| Publish/lock results | — | RW | — | RW | — | — | — |
| View own results | — | RW | — | R | R (A) | — | S |
| Upload person documents | — | RW | R | R | S | RW | S (own) |
| Generate certificates | — | RW | R | RW | — | R | S (own issued) |
| Import/export data | — | RW | RW (finance exports) | RW | — | R | — |
| View audit logs (own institute) | — | RW | R (finance) | R (academic) | — | — | — |

\*FRONT_DESK payment/ledger: **collect + limited view** (today’s receipts, no refund approve) — configurable; default as table.

---

## 3. Permission codes (initial catalog)

```text
institute:manage
user:manage
student:read student:write
faculty:read faculty:write
admission:write admission:approve
course:read course:write
batch:write
enrollment:write enrollment:override_capacity
fee_catalog:write
fee:read fee:collect fee:adjust fee:refund fee:reverse
attendance:mark attendance:unlock
timetable:write
exam:mark exam:publish
document:write document:read
audit:read
report:read
import:write
```

---

## 4. Gaps to revisit before coding

- [ ] Can FRONT_DESK see full outstanding aged reports? (default: no — accountant)
- [ ] Can students download fee receipts without finance role? (default: yes, own)
- [ ] Guardian portal permissions (phase 2)
- [x] Multi-tenant isolation for all institute roles (ADR 0004)
- [x] Institute ADMIN UI/API to create users and assign roles (`user:manage`)
- [x] FRONT_DESK can enroll via `enrollment:write` (not full `course:write` / catalog)
