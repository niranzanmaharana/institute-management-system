# 10 — Reporting requirements

**Status:** Draft — define now to influence indexes & APIs; full reporting module may be phase 2  
**v1:** operational screens + CSV exports for starred items

---

## 1. Student / admissions

| ID | Report | v1 | Status |
| --- | --- | --- | --- |
| RP-ST-01 | Student list (filters: status, batch, course) | ★ | Available — `/reports/students` + CSV |
| RP-ST-02 | Active students count by batch/course | ★ | Available — `/reports/students` + CSV |
| RP-ST-03 | New admissions in date range | ★ | Available — `/reports/admissions` + CSV |
| RP-ST-04 | Withdrawals / dropouts | ★ | Available — `/reports/students` (withdrawals) + CSV |
| RP-ST-05 | Batch-wise roster | ★ | Available — `/reports/students` (roster) + CSV |

## 2. Finance

| ID | Report | v1 | Status |
| --- | --- | --- | --- |
| RP-FI-01 | Daily collection | ★ | Available — `/finance-reports` |
| RP-FI-02 | Collection by payment method | ★ | Available — `/finance-reports` |
| RP-FI-03 | Outstanding fees (student/batch) | ★ | Available — `/finance-reports` |
| RP-FI-04 | Overdue invoices (aging buckets) | ★ | Available — `/finance-reports` |
| RP-FI-05 | Student fee ledger / statement | ★ | Available — Outstanding / fee account PDF+JSON |
| RP-FI-06 | Refunds | ★ | Available — `/refunds` + CSV |
| RP-FI-07 | Discounts / scholarships | ★ | Available — `/finance-reports` (RP-FI-07) |
| RP-FI-08 | Collection by fee category | ★ | Available — `/finance-reports` |

## 3. Attendance

| ID | Report | v1 | Status |
| --- | --- | --- | --- |
| RP-AT-01 | Student attendance % | ★ | Available — batch summary rows on `/reports/attendance` |
| RP-AT-02 | Batch attendance summary | ★ | Available — `/reports/attendance` + CSV |
| RP-AT-03 | Low attendance below threshold | ★ | Available — `/reports/attendance` + CSV |

## 4. Academic / exams

| ID | Report | v1 | Status |
| --- | --- | --- | --- |
| RP-EX-01 | Exam results by batch | ★ | Available — `/reports/exams` + CSV |
| RP-EX-02 | Pass/fail summary | ○ phase 2 polish | — |
| RP-EX-03 | Subject performance | ○ | — |

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
