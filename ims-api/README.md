# ims-api

Maven multi-module backend.

| Module | Port | Role |
| --- | --- | --- |
| `ims-common` | — | Errors, paging, TenantContext, money helpers, `CorrelationIds` |
| `ims-application` | 8080 | Modular monolith + Flyway + Swagger |
| `api-gateway` | 8088 | Single entry (routes to application) |

## Run

```bash
# From repo: infra/docker compose up -d   (MinIO + Jaeger; MySQL optional — app uses local MySQL)
# First time / after dependency changes: install modules into local .m2
mvn -pl ims-common,ims-application,api-gateway -am install -DskipTests
mvn -pl ims-application spring-boot:run
mvn -pl api-gateway spring-boot:run
```

Local MySQL (default in `application.yml`): `localhost:3306`, database `ims`, user/pass `root`/`root`.
Flyway creates schema objects on first start (`createDatabaseIfNotExist=true`).

Students API (Phase 2.1): `/api/v1/students` — requires JWT with `student:read` / `student:write`.
Academic (2.2): `/api/v1/courses`, `/fee-categories`, `/fee-plans`, `/batches`, `/academic-years`.
Admissions (2.3 / 3.3): `/api/v1/admissions`, `/api/v1/enquiries`.
Enrollments (2.4): `/api/v1/enrollments` (+ activate / lifecycle).
Finance (2.5): `/api/v1/fee-accounts`, `/outstanding`, payments.
Users & roles: `/api/v1/users`, `/api/v1/roles` — requires `user:manage` (institute ADMIN).
- App Swagger: http://localhost:8080/swagger-ui.html
- **Preferred entry:** Gateway http://localhost:8088 (UI uses this)
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Jaeger (OTLP traces): http://localhost:16686 — look up services `api-gateway` and `ims-application`
- Postman: import `../postman/ims-api.postman_collection.json` or regenerate via `../scripts/generate-postman.ps1`

## Logging & tracing

Both processes log every API call (except `/actuator/health`) with:

`[traceId,spanId] requestId=… userId=…` plus an HTTP access line that repeats those ids.

- Propagation: W3C `traceparent` (automatic) and `X-Request-Id` (gateway generates if missing).
- Same `traceId` / `requestId` appear in **gateway** and **application** consoles for one user action.
- OTLP endpoint: `http://localhost:4318/v1/traces` (requires Jaeger from `infra/docker-compose`).
- Sampling: 100% locally (`management.tracing.sampling.probability=1.0`).

## Phase 0 tenant header

Send `X-Institute-Id: 1` (DEMO_A) or `2` (DEMO_B) on `/api/v1/tenant-probes/**`.
