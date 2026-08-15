# 07 — API guidelines

**Status:** Draft after architecture review  
**Companion:** Coding standards §5.4; modular monolith still exposes versioned HTTP APIs

---

## 1. OpenAPI-first

```text
Design OpenAPI contract → review → implement → CI fails on undocumented/breaking drift
```

- Each module publishes paths under `/api/v1/...`
- springdoc generates/validates against committed OpenAPI (or commit YAML as source of truth — pick one in Phase 0; **recommendation: code annotations → generate artifact, plus breaking-change check**)
- Angular clients: **prefer generated** TypeScript client from OpenAPI for module APIs

---

## 2. Conventions

| Topic | Rule |
| --- | --- |
| Versioning | `/api/v1`; breaking → `/api/v2` |
| Tenancy | Tenant from JWT `institute_id` only; ignore/forbid body/query `instituteId` on normal APIs |
| Errors | Unified problem JSON (`traceId`, `error`, `message`, `details`) |
| Cross-tenant | Missing/other-tenant resource → `404` (preferred) or `403` — never leak existence across tenants inconsistently; pick **404** project-wide |
| Pagination | `page`, `size`, `sort`; max size enforced |
| Dates | ISO-8601; dates as `YYYY-MM-DD`; timestamps UTC |
| Money | Decimal numbers with 2-scale — document in OpenAPI |
| IDs | Numeric path IDs; public codes in body/filters where useful |

---

## 3. Idempotency

Required headers on:

- `POST` collect payment  
- `POST` activate enrollment / generate invoices  
- `POST` process refund  
- Other money-moving POSTs  

### Storage (`idempotency_records`)

| Column | Purpose |
| --- | --- |
| `institute_id` | Tenant scope |
| `idempotency_key` | Client key (unique per institute + user + operation) |
| `user_id` | Caller |
| `operation` | e.g. `PAYMENT_COLLECT` |
| `request_hash` | Hash of canonical body |
| `response_status` | HTTP status to replay |
| `response_body` | Snapshot to replay |
| `expires_at` | Retention window (e.g. 48h) |

Same key + same hash → replay response. Same key + different hash → `409 CONFLICT`. Keys are **per institute**.

---

## 4. Concurrency

- Fee accounts use `@Version` / `version` column  
- On conflict: `409` with clear message; client refreshes and retries  
- Integration test: concurrent payments on same account  
- Isolation test: two institutes cannot observe each other’s payments/invoices by id

---

## 5. Health

| Endpoint | Purpose |
| --- | --- |
| `/actuator/health/liveness` | Process up |
| `/actuator/health/readiness` | DB reachable (and critical deps) |
| Gateway health | Aggregated or separate |

---

## 6. Gateway

- Strip client-supplied identity / tenant headers; set from JWT (`X-Institute-Id` only if re-asserted from trusted claims)  
- Rate-limit login and payment endpoints  
- Propagate W3C `traceparent` (Micrometer Tracing) and `X-Request-Id`  
- Emit access logs with `traceId`, `spanId`, `requestId` (same fields as `ims-application`)
