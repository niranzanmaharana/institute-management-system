# Phase 1 accessibility baseline — signed

Signed for shell + login (WCAG 2.2 AA start).

- [x] Login fields have visible `<label>` / `for` association (`app-ui-input`)
- [x] Login error uses `role="alert"`
- [x] Primary button min height 44px; focus-visible outline
- [x] Sidebar toggle and close have `aria-label`
- [x] Mobile drawer closes on backdrop / close / navigation
- [x] Keyboard: Tab through login form; Enter submits
- [x] Modal focus trap + Escape close pattern (`app-ui-modal`)
- [x] Contrast tokens documented in `docs/design/README.md`

**Sign-off:** Phase 1 shell/login a11y baseline accepted for MVP gate (2026-08-09).

Manual check: open `/login`, complete form with keyboard only; open Design system → Modal and Tab-cycle within dialog.
