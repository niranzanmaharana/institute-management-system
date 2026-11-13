# Institute Management System (IMS)

Multi-tenant institute management platform.

| Path | Description |
| --- | --- |
| [ims-ui](./ims-ui) | Angular SPA (Skydash-inspired admin UI) |
| [ims-backend](./ims-backend) | Spring Boot modular monolith (`ims-api`) + gateway + shared library |
| [infra](./infra) | Docker Compose (MySQL, MinIO, Jaeger) |
| [docs](./docs) | Architecture, standards, MVP roadmap |
| [postman](./postman) | Generated Postman collections |

## Prerequisites

- JDK **21**
- Maven 3.9+
- Node.js **22.19+** (Angular **21** until Node ≥ **22.22.3** for Angular 22 — see `ims-ui/README.md`)
- Docker Desktop (MinIO / Jaeger + Testcontainers; MySQL in Compose is optional if you use a local install)

## Quick start (Phase 0–1)

One command (Windows). Starts MinIO, builds the backend, then opens three windows for the API (`:8080`), gateway (`:8088`), and UI (`:4200`):

```powershell
.\scripts\start-dev.ps1
.\scripts\stop-dev.ps1
```

Start flags: `-SkipInfra` (no Docker), `-SkipMavenInstall` (already built), `-FullInfra` (Compose MySQL + MinIO + Jaeger). Local MySQL on `3306` is assumed unless you use `-FullInfra` and point the datasource at `3307`.

Stop flags: `-SkipInfra` (leave Docker running), `-Down` (`docker compose down` instead of `stop`).

Manual steps:

```bash
# Infrastructure (requires Docker Desktop). MinIO only if you already have local MySQL:
cd infra
docker compose up -d minio

# Backend — install modules once, then run API + gateway
cd ../ims-backend
mvn -pl ims-common,ims-api,ims-gateway -am install -DskipTests
mvn -pl ims-api spring-boot:run
# separate terminal:
mvn -pl ims-gateway spring-boot:run

# Frontend (talks to gateway :8088)
cd ../ims-ui
npm start
```

### Demo logins

| User | Password | Institute code |
| --- | --- | --- |
| `admin` | `Password@123` | `DEMO_A` or `DEMO_B` |
| `platform` | `Password@123` | *(leave empty)* |

- UI: http://localhost:4200/login  
- API via **gateway**: http://localhost:8088  
- App direct (debug): http://localhost:8080/swagger-ui.html  
- MinIO console (person documents): http://localhost:9001 — `minio` / `minio12345`  
- Jaeger: http://localhost:16686  
- Postman: `postman/ims-api.postman_collection.json`  

## OpenAPI → Postman

With `ims-api` running:

```bash
cd ims-backend
mvn -pl ims-api -DskipTests package
# or while app is up:
npm run generate:postman --prefix ../ims-ui
# alternatively from repo root scripts:
./scripts/generate-postman.ps1
```

Collection output: `postman/ims-api.postman_collection.json`

## How to use the app (staff guide)

Step-by-step flows by role (admissions approve/reject, enroll, fees, refunds, etc.):  
**[docs/14-user-manual.md](./docs/14-user-manual.md)**

## Phase roadmap

See [docs/11-mvp-v1-step-by-step-development.md](./docs/11-mvp-v1-step-by-step-development.md).

