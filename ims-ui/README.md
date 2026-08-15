# ims-ui

Angular admin SPA for IMS. Visual style follows BootstrapDash **Skydash** (see `docs/design/`).

## Note on Angular version

Uses **Angular 21** while local Node is below Angular 22’s requirement (`>=22.22.3`). Current environment: Node `v22.19.0`. After upgrading Node, migrate with `ng update` to match doc 11’s Angular 22 wording.

## Run

```bash
npm install
npm start
```

App: http://localhost:4200 — API base URL is the **gateway** (`http://localhost:8088`).

Login: `admin` / `Password@123` / `DEMO_A` (or `DEMO_B`).

Design system demo (authenticated): `/design-system`
