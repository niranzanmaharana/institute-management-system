# 10 — Reporting requirements

**Status:** Draft — define now to influence indexes & APIs; full reporting module may be phase 2  
**v1:** operational screens + CSV exports for starred items

---

## 1. Student / admissions

| ID | Report | v1 |
| --- | --- | --- |
| RP-ST-01 | Student list (filters: status, batch, course) | ★ |
| RP-ST-02 | Active students count by batch/course | ★ |
| RP-ST-03 | New admissions in date range | ★ |
| RP-ST-04 | Withdrawals / dropouts | ★ |
| RP-ST-05 | Batch-wise roster | ★ |

## 2. Finance

| ID | Report | v1 |
| --- | --- | --- |
| RP-FI-01 | Daily collection | ★ |
| RP-FI-02 | Collection by payment method | ★ |
| RP-FI-03 | Outstanding fees (student/batch) | ★ |
| RP-FI-04 | Overdue invoices (aging buckets) | ★ |
| RP-FI-05 | Student fee ledger / statement | ★ |
| RP-FI-06 | Refunds | ★ |
| RP-FI-07 | Discounts / scholarships | ★ |
| RP-FI-08 | Collection by fee category | ★ |

## 3. Attendance

| ID | Report | v1 |
| --- | --- | --- |
| RP-AT-01 | Student attendance % | ★ |
| RP-AT-02 | Batch attendance summary | ★ |
| RP-AT-03 | Low attendance below threshold | ★ |

## 4. Academic / exams

| ID | Report | v1 |
| --- | --- | --- |
| RP-EX-01 | Exam results by batch | ★ |
| RP-EX-02 | Pass/fail summary | ○ phase 2 polish |
| RP-EX-03 | Subject performance | ○ |

★ = available as screen and/or export in v1 spine  
○ = may wait for reporting module polish

---

## 5. Index / API implications

- Index outstanding, due_date, paid_at, enrollment(student,batch), attendance(session,student)  
- Prefer dedicated query services / SQL views for heavy reports rather than abusing write models  
- Category on invoices required for RP-FI-08  

---

## 6. Import / export (related)

| Capability | Notes |
| --- | --- |
| Import students CSV | Validate then insert; error report file |
| Import faculty | Same pattern |
| Bulk enrollment | Async job if > N rows |
| Bulk marks | Async; idempotent per exam+student |
| Bulk attendance | Optional |
| Exports | CSV for ★ reports; PDF for receipts/statements |
