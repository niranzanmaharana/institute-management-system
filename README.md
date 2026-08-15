# Institute Management System (IMS)

Multi-tenant institute management platform.

| Path | Description |
| --- | --- |
| [ims-ui](./ims-ui) | Angular SPA (Skydash-inspired admin UI) |
| [ims-api](./ims-api) | Spring Boot modular monolith + API Gateway + shared library |
| [infra](./infra) | Docker Compose (MySQL, MinIO, Jaeger) |
| [docs](./docs) | Architecture, standards, MVP roadmap |
| [postman](./postman) | Generated Postman collections |

## Prerequisites

- JDK **21**
- Maven 3.9+
- Node.js **22.19+** (Angular **21** until Node ≥ **22.22.3** for Angular 22 — see `ims-ui/README.md`)
- Docker Desktop (MinIO / Jaeger + Testcontainers; MySQL in Compose is optional if you use a local install)

## Quick start (Phase 0–1)

```bash
# Infrastructure (requires Docker Desktop)
cd infra
docker compose up -d

# Backend — install modules once, then run app + gateway
cd ../ims-api
mvn -pl ims-common,ims-application,api-gateway -am install -DskipTests
mvn -pl ims-application spring-boot:run
# separate terminal:
mvn -pl api-gateway spring-boot:run

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

With `ims-application` running:

```bash
cd ims-api
mvn -pl ims-application -DskipTests package
# or while app is up:
npm run generate:postman --prefix ../ims-ui
# alternatively from repo root scripts:
./scripts/generate-postman.ps1
```

Collection output: `postman/ims-api.postman_collection.json`

## Phase roadmap

See [docs/11-mvp-v1-step-by-step-development.md](./docs/11-mvp-v1-step-by-step-development.md).
#
