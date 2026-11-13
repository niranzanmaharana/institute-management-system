# 14 — User manual (how to use IMS)

**Audience:** institute staff (Front Desk, Coordinator, Accountant, Admin) and platform operators  
**Purpose:** plain-language, step-by-step guide — **who does what, and what happens next**  
**Keep this file updated** whenever a feature ships (add a short “How to use it” section here).

**Related:** [authorization matrix](./06-authorization-matrix.md) · [business rules](./04-business-rules.md) · [course/fee relationships](./12-course-fee-structure-and-relationships.md)

---

## 0. Quick start

| Item | Value |
| --- | --- |
| UI | http://localhost:4200/login |
| Demo institute admin | username `admin` · password `Password@123` · institute `DEMO_A` or `DEMO_B` |
| Platform operator | username `platform` · password `Password@123` · **leave institute blank** |

After database migrations that add permissions, **log out and log in again** so your JWT includes the new rights.

---

## 1. Roles — who is who

IMS is **multi-tenant**. Each institute’s data is isolated. Login users belong to **one institute** (except platform).

| Role | What they are for | Typical menu |
| --- | --- | --- |
| **PLATFORM_ADMIN** | Create / manage institutes on the platform | Institutes |
| **ADMIN** | Full institute control: users, catalog, admissions approve, fees, overrides | Almost everything |
| **ACADEMIC_COORDINATOR** | Courses, batches, admissions approve, enrollments | Courses, Batches, Admissions, Enrollments, People |
| **FRONT_DESK** | Walk-in work: enquiries, applications, students, enroll, collect payment | Students, Enquiries, Admissions, Enrollments, Outstanding (collect) |
| **ACCOUNTANT** | Fees: collect, adjust, refund, reverse | Outstanding, Refunds |
| **FACULTY** *(login role)* | Teaching later (attendance/exams). Separate from **Faculty people** master | Limited |
| **STAFF / FACULTY people records** | HR-style master data (not the same as login users) | Faculty / Staff screens |

**Important distinctions**

| Term | Meaning |
| --- | --- |
| **Login user** | Someone who can sign in (`Users & roles`) |
| **Student / Faculty / Staff** | People master records (profiles, documents) |
| **Applicant** | Person on an **admission application** (may become a Student when approved) |
| **Enquiry** | Optional lead *before* a formal application |

There is **no public student self-signup portal yet**. Staff create enquiries/applications/students inside the institute.

---

## 2. Golden path (happy day)

Do work in this order the first time for an institute:

```text
1. Platform: institute exists
2. Admin: create login users for Front Desk / Accountant / Coordinator
3. Coordinator/Admin: Academic year → Course → Fee category → Fee plan → Batch
4. Front Desk: (optional) Enquiry → Application
5. Coordinator/Admin: Approve or Reject application
6. Front Desk/Coordinator: Enroll student into batch + fee plan
7. Coordinator/Admin: Activate enrollment → fee account + invoices appear
8. Front Desk/Accountant: Collect payments on Outstanding
9. Accountant/Admin: Adjustments / Refunds / Reverse mistaken payments as needed
```

---

## 3. Platform — institutes

**Who:** PLATFORM_ADMIN only (`platform` user).

### Create an institute

1. Log in as `platform` (no institute code).
2. Open **Institutes** → **New**.
3. Enter institute details and the first **ADMIN** username/password for that institute.
4. Save.

**What happens:** A new tenant exists. That ADMIN can log in with the new institute code and set up the rest.

### View / edit institute

Use **Institutes** list → open or edit. DEMO_A and DEMO_B are seed institutes for demos.

---

## 4. Users & roles (login accounts)

**Who:** Institute **ADMIN** (`Users & roles`).

### Create a staff login

1. Log in as institute ADMIN.
2. Open **Users & roles** → **New**.
3. Username, email, password, and one or more roles (e.g. `FRONT_DESK`, `ACCOUNTANT`).
4. Save.

**Rules**

- You cannot assign `PLATFORM_ADMIN`.
- You cannot deactivate yourself.
- You cannot remove the last ACTIVE ADMIN.

**After creating a user:** they log in with the **same institute code** as you.

---

## 5. Academic catalog (must exist before fees/enroll)

**Who:** ADMIN or ACADEMIC_COORDINATOR.

### 5.1 Create a course

1. **Courses** → **New**.
2. Name (code optional — use **Generate** or leave blank for auto-code).
3. Open the course.

### 5.2 Fee category + fee plan

On **course detail**:

1. If no categories yet, create a **fee category** (e.g. Tuition).
2. Create a **fee plan**: total amount + at least one installment.
3. Installment amounts **must sum exactly** to the plan total.

### 5.3 Subjects (optional, for teaching assignment)

Still on **course detail** → **Subjects** → add name (code optional).

### 5.4 Academic year + batch

1. **Batches** → **New**.
2. Pick course + academic year (create year if prompted), capacity, dates.
3. Open **batch detail** to assign faculty later.

### 5.5 Assign faculty to a batch

**Who:** ADMIN / ACADEMIC_COORDINATOR (`course:write`).

1. Ensure **Faculty** people exist (**Faculty** → New).
2. Open **Batches** → batch → **Faculty assignments**.
3. Choose faculty, optional subject, role (Teacher / Coordinator) → **Assign**.

**What happens:** Teaching assignment is stored for that batch (and optional subject). Used later for attendance/authz.

---

## 6. People master data

### Students / Faculty / Staff

**Who:** ADMIN, FRONT_DESK (students write); coordinators as per permissions.

1. Open **Students** / **Faculty** / **Staff**.
2. Search / filter / create / view / edit.
3. On **detail**, upload **documents** and **profile photo** (needs MinIO running).

**Document rules:** PDF / JPEG / PNG, max 5 MB.  
**Who can upload:** ADMIN, FRONT_DESK. Others may read depending on role.

---

## 7. Enquiries (optional lead)

**Who creates:** FRONT_DESK, ACADEMIC_COORDINATOR, ADMIN.  
**Screen:** **Enquiries**.

### Flow

```text
OPEN ──convert──► creates Admission application; enquiry becomes CONVERTED
  └──close──────► CLOSED
```

### Steps

1. **Enquiries** → **New** → name, phone/email, optional interested course.
2. Later: open enquiry → **Convert** (starts an application) **or** **Close**.

**What happens on convert:** An admission application is created; enquiry status becomes `CONVERTED`. Continue with admissions (next section).

---

## 8. Admissions (applications)

**Who creates applications:** FRONT_DESK, COORDINATOR, ADMIN.  
**Who approves / rejects:** ADMIN or ACADEMIC_COORDINATOR (**not** Front Desk).  
**Screen:** **Admissions**.

### Statuses (plain language)

| Status | Meaning |
| --- | --- |
| **SUBMITTED** | Waiting for decision |
| **APPROVED** | Accepted — student linked/created; ready to enroll |
| **REJECTED** | Turned down (reason required, min 5 characters) |
| **CANCELLED** | Withdrawn by staff (reason required) |

### Create an application

1. **Admissions** → **New** (or convert from enquiry).
2. Applicant name, phone/email, course.
3. Save → status **SUBMITTED**.

### Approve

**Who:** ADMIN / ACADEMIC_COORDINATOR.

1. Open application → **Approve**.
2. Optionally create a new student profile in the approve form (or link if one already exists).

**What happens when accepted**

- Application → **APPROVED**.
- A **Student** record is created (if requested) or linked.
- Application is tied to that student.
- **Next:** go to **Enrollments** and enroll that student into a batch + fee plan (approval alone does **not** create fees).

### Reject

1. Open application → **Reject**.
2. Enter reason (at least 5 characters).

**What happens when rejected**

- Application → **REJECTED**.
- **No student** is created from this step.
- Applicant can apply again later under institute rules (duplicate active applications for same person + course are blocked).

### Cancel

Same as reject for reasons: staff cancels a SUBMITTED application with a reason → **CANCELLED**. No enrollment/fees from this application.

---

## 9. Enrollments

**Who creates:** ADMIN, COORDINATOR, FRONT_DESK (`enrollment:write`).  
**Who activates / lifecycle:** usually ADMIN / COORDINATOR (`course:write`); Front Desk may create but activation rules still apply.  
**Screen:** **Enrollments**.

### Statuses

```text
APPLIED → ACTIVE → COMPLETED
              ↘ SUSPENDED ↔ ACTIVE (resume)
              ↘ WITHDRAWN
APPLIED → CANCELLED
```

### Create enrollment

1. **Enrollments** → **New**.
2. Student + **batch** + **fee plan** (same course as batch).
3. Save → status **APPLIED**.

**Checks**

- Student should not already have APPLIED/ACTIVE enrollment in another batch of the **same course**.
- Batch capacity is enforced (ADMIN can override with reason).

### Activate

1. Open enrollment → **Activate**.

**What happens**

- Enrollment → **ACTIVE**.
- System creates a **fee account** and **invoices** from the fee plan installments.
- **Outstanding** = sum of invoice amounts (before payments/adjustments).
- Needs approved admission for that course **or** ADMIN admission waiver.

### Other actions (on detail)

| Action | Effect |
| --- | --- |
| **Cancel** (while APPLIED) | Stops enrollment; no fee account expected |
| **Suspend** | Blocks attendance later; fees still owed |
| **Resume** | Back to ACTIVE |
| **Withdraw** | Leaves the program; history kept |
| **Complete** | Finished course; outstanding **not** auto-waived |
| **Transfer** | Move to another batch **same course** (capacity rules) |

---

## 10. Finance

**Screens:** **Outstanding**, **Refunds**.

### Who can do what

| Action | ADMIN | ACCOUNTANT | FRONT_DESK |
| --- | --- | --- | --- |
| View outstanding | Yes | Yes | Limited / if `fee:read` |
| Collect payment | Yes | Yes | Yes (`fee:collect`) |
| Adjust (discount/scholarship/waiver/penalty) | Yes | Yes | No |
| Refunds (request/approve/process) | Yes | Yes | No |
| Reverse a payment | Yes | Yes | No |

### 10.1 Collect payment

1. **Outstanding** → choose account → **Collect**.
2. Amount (cannot exceed outstanding) + method (Cash / UPI / …).
3. Submit.

**What happens**

- Payment SUCCESS + **receipt** number.
- Money applies to oldest due invoices first.
- Outstanding decreases.

### 10.2 Adjustments

1. Outstanding → **Adjust**.
2. Type: Discount / Scholarship / Waiver / Penalty + amount + reason (≥ 5 chars).

| Type | Effect on outstanding |
| --- | --- |
| Discount / Scholarship / Waiver | Decreases |
| Penalty | Increases |

Credits cannot make outstanding negative.

### 10.3 Refunds

**Screen:** **Refunds** (also linked from Outstanding).

```text
REQUESTED → APPROVED → PROCESSING → COMPLETED
                                 ↘ FAILED
REQUESTED → REJECTED
```

**Steps**

1. Request refund (fee account id, amount, reason). Amount capped by net successful payments minus other refunds.
2. **Approve** or **Reject** (reason).
3. If approved → **Complete** (money returned: outstanding **goes up**) or **Fail** (ledger unchanged).

Export CSV from the Refunds page for the refund list report.

### 10.4 Reverse a mistaken payment

**Who:** ADMIN / ACCOUNTANT.  
**When:** Wrong amount/method recorded — **not** the same as a student refund.

1. Outstanding → **Payments** for that account.
2. Choose a **SUCCESS** payment → **Reverse** + reason (≥ 5 chars).

**What happens**

- Payment status → **REVERSED** (row is **kept**, not deleted).
- Invoice paid amounts restored.
- Receipt → **CANCELLED**.
- Outstanding recalculated (usually goes **up** again).

Then collect the correct payment if needed.

### 10.5 Finance reports & financial years

**Who:** anyone with `fee:read` (ADMIN, ACCOUNTANT, …).  
**Manage years:** ADMIN / ACCOUNTANT (`fee:adjust`).  
**Screen:** **Finance reports**.

| Report | What it shows |
| --- | --- |
| **Aging** | Open invoices by overdue bucket: Current, 1–30, 31–60, 61–90, 90+ days |
| **Outstanding** | Fee accounts still owing money |
| **Daily collection** | SUCCESS payments total per day (pick From/To) |
| **By method** | Cash / UPI / Card / … totals |
| **By category** | Collections allocated to fee categories (Tuition, etc.) |

Each table has **Export CSV**.

**Financial years**

1. List years (one is created automatically on first enrollment activate if none exists).
2. **Add financial year** (name, start, end, optionally activate).
3. **Activate** another year (previous active year turns inactive). New invoices stamp the **active** year.

---

## 11. Documents & photos

**Where:** Student / Faculty / Staff **detail** pages.

1. Start MinIO (`scripts\start-dev.ps1` or `docker compose up -d minio` in `infra`).
2. Upload PDF/JPEG/PNG ≤ 5 MB.
3. Profile photo: Add/Change photo in the header (JPEG/PNG).

---

## 12. Cheat sheet — “I’m stuck, what next?”

| You just did… | Next step |
| --- | --- |
| Created course | Add fee category + fee plan |
| Created fee plan | Create academic year + batch |
| Created application | Coordinator/Admin **Approve** or **Reject** |
| Approved application | **Enroll** student → then **Activate** |
| Activated enrollment | Open **Outstanding** → collect fees |
| Wrong payment | **Reverse** payment, then collect again |
| Student leaving with money paid | Use **Refunds** (not Reverse) |
| Need discount | **Adjust** waiver/scholarship/discount |
| Need aging / collection totals | **Finance reports** → pick dates → Export CSV |
| Login menu missing an item | Wrong role — ask ADMIN; or re-login after a permission migration |
| Document upload fails | Ensure MinIO is running |

---

## 13. Feature log (update when shipping)

Use this table as the maintenance checklist for this manual.

| When | Feature | Manual section |
| --- | --- | --- |
| MVP | Login, students, courses, admissions, enroll, pay, outstanding | §§3–10.1 |
| 3.1 | Faculty/staff, search | §6 |
| 3.2 | Enrollment lifecycle | §9 |
| 3.3 | Enquiries; Front Desk enroll | §§7–9 |
| 3.4 | Person documents / photo | §11 |
| 3.5 | Faculty–batch assignment | §5.5 |
| 4.1 | Fee adjustments | §10.2 |
| 4.2 | Refunds | §10.3 |
| 4.3 | Payment reversal | §10.4 |
| 4.4 | Aging + FY + collection reports | §10.5 |
| *Next* | Fee statement PDF / attendance / … | Add new § |

---

## 14. Words that confuse people

| Word in the app | Plain meaning |
| --- | --- |
| Institute code | Which school/tenant you belong to (`DEMO_A`) |
| Fee plan | Price list for a course (installments) |
| Batch | A class offering of a course in a year |
| Fee account | That student’s bill for one enrollment |
| Outstanding | How much is still owed |
| Adjustment | Change the bill without a payment (discount/penalty…) |
| Refund | Give money back that was already collected |
| Reverse payment | Undo a mistaken payment entry (receipt cancelled) |
| Activate enrollment | Officially start the student → creates the bill |
| Approve admission | Accept the applicant → creates/links Student |
