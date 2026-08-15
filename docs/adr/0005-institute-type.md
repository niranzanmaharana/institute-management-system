# ADR 0005 — Institute type (v1 product scope)

**Status:** Accepted  
**Date:** 2026-08-08  
**Context:** Schools, colleges, and coaching centers need different progression models. Ambiguity would force schema churn.

## Decision

v1 targets a **professional training / coaching institute**:

- Multiple **courses** with duration, fee plans, subjects
- **Batches/cohorts** with capacity
- Students may take **multiple courses** over time
- Progression = enrollment lifecycle toward **completion** (not semester promotion / GPA programs)
- Attendance + exams support training outcomes; not a full university examination ERP

**Out of v1 core:** hostel/transport as full modules (fee categories may still reserve codes), library, payroll, LMS content.

If the real customer is a **school/college**, revisit academic progression (terms, promotion, backlog) before implementing Operations DB deeply — see `04-business-rules.md`.

## Consequences

- Admission → Student → Enrollment → Fees is the spine.
- `academic_year` exists for planning; **financial_year** is separate for finance.
- Semester/promotion tables are deferred unless scope changes.
