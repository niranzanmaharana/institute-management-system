# Operations responsive smoke (Phase 5.4)

Manual checks at **375 / 768 / 1440**. **Superseded for v1 sign-off by** [../responsive-qa.md](../responsive-qa.md) (Step 7.4).

| Screen | 375 | 768 | 1440 | Notes |
| --- | :---: | :---: | :---: | --- |
| Dashboard → Operations shortcuts | ✅ | ✅ | ✅ | Links visible by permission |
| Attendance list | ✅ | ✅ | ✅ | Cards ≤767; table ≥768; batch filter |
| Attendance session | ✅ | ✅ | ✅ | Action buttons wrap; table scrolls |
| Timetable | ✅ | ✅ | ✅ | Rooms cards ≤767; week→agenda ≤1023 |
| Exams list | ✅ | ✅ | ✅ | Cards ≤767; table ≥768; batch filter |
| Exam detail | ✅ | ✅ | ✅ | Marks table horizontal scroll; lifecycle actions wrap |

Touch targets ≥44px on primary buttons (shared `UiButton` / `UiInput`).

**Automation:** `ims-ui` → `npm run e2e:responsive`.
