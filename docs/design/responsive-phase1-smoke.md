# Phase 1 responsive smoke (shell)

Manual smoke for authenticated shell + login. Target: **no page-level horizontal scroll**.

| Viewport | Width | Login | Shell drawer/sidebar | Dashboard | Design system |
| --- | ---: | :---: | :---: | :---: | :---: |
| Mobile | 375 | ☐ | ☐ | ☐ | ☐ |
| Tablet | 768 | ☐ | ☐ | ☐ | ☐ |
| Desktop | 1440 | ☐ | ☐ | ☐ | ☐ |

**How:** DevTools device mode → widths above → `/login` then login as `admin` / `Password@123` / `DEMO_A` (API via gateway `8088`).

For a **real phone** on Wi‑Fi, see [mobile-localhost-access.md](./mobile-localhost-access.md).

**Result (2026-08-09):** Layout breakpoints implemented (`admin-layout` drawer ≤767px). Mark boxes during local QA before Phase 2 demos.
