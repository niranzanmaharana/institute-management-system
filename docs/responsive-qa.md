# Step 7.4 — Full responsive QA matrix

**Date:** 2026-11-15  
**Standard:** FR-UX-01; [01 §3.13](./01-modules-and-delivery-plan.md); [02 §6.6](./02-coding-standards-and-guidelines.md)  
**Environment:** `ims-ui` `:4200` → gateway `:8088`; DEMO_A as `admin-a` / `Password@123`  
**Automation:** `ims-ui` Playwright projects `mobile-375` / `tablet-768` / `desktop-1440` — `npm run e2e:responsive`

**Pass rules:** no page-level horizontal scroll (`document`/`body` overflow &lt; 8px); lists use cards ≤767 or contained table scroll; touch targets ≥44px on shared `UiButton` / `UiInput`; drawer shell ≤767.

---

## 1. Sign-off

| | |
| --- | --- |
| **Result** | **PASS** |
| **Playwright** | 59 passed, 2 skipped (drawer test mobile-only), EXIT 0 — 2026-11-15 |
| **P0/P1 fixes** | Table↔cards on high-traffic lists; `UiInput` min-height 44px; timetable agenda ≤1023; admin content `overflow-x: clip` |
| **Signed** | 2026-11-15 |

Re-run: from `ims-ui`, with UI + gateway up — `npm run e2e:responsive`.

---

## 2. Automated overflow smoke (lists + shell)

| Screen / route | 375 | 768 | 1440 | Notes |
| --- | :---: | :---: | :---: | --- |
| Login + shell / dashboard | ✅ | ✅ | ✅ | Drawer open checked at 375 |
| Students `/students` | ✅ | ✅ | ✅ | Cards ≤767 |
| Faculty `/faculty` | ✅ | ✅ | ✅ | Cards ≤767 |
| Staff `/staff` | ✅ | ✅ | ✅ | Cards ≤767 |
| Courses `/courses` | ✅ | ✅ | ✅ | Cards ≤767 |
| Batches `/batches` | ✅ | ✅ | ✅ | Cards ≤767 |
| Admissions `/admissions` | ✅ | ✅ | ✅ | Cards ≤767 |
| Enquiries `/enquiries` | ✅ | ✅ | ✅ | Cards ≤767 |
| Enrollments `/enrollments` | ✅ | ✅ | ✅ | Cards ≤767 |
| Outstanding `/outstanding` | ✅ | ✅ | ✅ | Cards ≤767 |
| Refunds `/refunds` | ✅ | ✅ | ✅ | Cards ≤767 |
| Finance reports `/finance-reports` | ✅ | ✅ | ✅ | Contained scroll OK |
| Student reports `/reports/students` | ✅ | ✅ | ✅ | Contained scroll OK |
| Attendance `/attendance` | ✅ | ✅ | ✅ | Cards ≤767 |
| Timetable `/timetable` | ✅ | ✅ | ✅ | Agenda ≤1023; week ≥1024 |
| Exams `/exams` | ✅ | ✅ | ✅ | Cards ≤767 |
| Users `/users` | ✅ | ✅ | ✅ | Cards ≤767 |
| Imports `/imports` | ✅ | ✅ | ✅ | Form layout |

---

## 3. Manual — timetable & exams (detail / dense UI)

| Screen | 375 | 768 | 1440 | Notes |
| --- | :---: | :---: | :---: | --- |
| Timetable rooms list | ✅ | ✅ | ✅ | Cards ≤767 |
| Timetable week / agenda | ✅ | ✅ | ✅ | Week grid only ≥1024; agenda on phone/tablet |
| Exams list | ✅ | ✅ | ✅ | Cards ≤767; batch filter |
| Exam detail (marks) | ✅ | ✅ | ✅ | Marks table uses contained horizontal scroll; lifecycle actions wrap |
| Attendance session | ✅ | ✅ | ✅ | Action wrap; contained table scroll |

---

## 4. P0 / P1 fixes applied this step

| Severity | Issue | Fix |
| --- | --- | --- |
| P0 | High-traffic lists table-only on phone | `ops-table` / `ops-cards` on students, faculty, staff, enrollments, outstanding, refunds, admissions, enquiries, users, courses, batches |
| P0 | Timetable week grid overflow on tablet/phone | Agenda view ≤1023px; week desktop-only |
| P1 | Page-level clip on shell content | `admin-layout` content `overflow-x: clip` |
| P1 | Input touch target | Shared `UiInput` `min-height: 44px` (buttons already 44px) |

---

## 5. Related checklists

| Doc | Role |
| --- | --- |
| [design/responsive-phase1-smoke.md](./design/responsive-phase1-smoke.md) | Phase 1 shell (superseded for sign-off by this matrix) |
| [design/responsive-ops-smoke.md](./design/responsive-ops-smoke.md) | Phase 5.4 ops subset |

---

## 6. How to re-verify

```powershell
cd ims-ui
npm run e2e:responsive
```

Optional env: `IMS_UI_BASE`, `IMS_API_BASE`, `IMS_USER`, `IMS_PASSWORD`, `IMS_INSTITUTE`. Auth uses API login + `storageState` (avoids gateway login rate-limit during parallel runs).
