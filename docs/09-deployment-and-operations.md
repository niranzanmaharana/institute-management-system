# 09 — Deployment & operations

**Status:** Draft after architecture review

---

## 1. Environments

```text
Developer laptop (docker-compose)
    → CI (build, lint, test, migrate-check)
    → Development
    → QA / Test
    → Staging
    → Production
```

| Env | Data | Deploy |
| --- | --- | --- |
| Local | Docker MySQL, MinIO, Jaeger | compose up |
| Dev | Shared, resettable | auto on main/develop |
| QA | Seeded scenarios | manual/auto |
| Staging | Prod-like | gated |
| Prod | Live | gated + approval |

---

## 2. CI/CD expectations

- Build backend + frontend  
- Unit/integration tests  
- Lint/format  
- Flyway validate  
- OpenAPI diff / breaking check  
- Optional Playwright smoke on compose  
- Container image tagged by git SHA  
- Migrations run as release step **before** switching traffic  
- Rollback: previous image + **forward-fix** migrations (no reliance on down scripts for prod)  

**Who deploys prod:** designated releaser; secrets via vault/CI secrets.

---

## 3. Runtime topology (v1)

```text
[Angular static] → [API Gateway] → [ims-application]
                         ↓
                 [MySQL] [MinIO] [Jaeger]
```

Local MinIO is an **S3-compatible** object store for person documents. Production can use AWS S3 or another S3 API without changing document endpoints — [13-object-storage.md](./13-object-storage.md).

Optional: Redis for rate-limit/session later.

---

## 4. Health & resilience

- Liveness / readiness on app and gateway  
- Graceful shutdown (finish in-flight, refuse new)  
- DB down → readiness fail; clear API errors  
- Timeouts on any future external HTTP  

---

## 5. Backup & disaster recovery

| Asset | Backup | Retention (initial proposal) |
| --- | --- | --- |
| MySQL | Daily full + binlog/PITR if hosted | 30 days |
| Object storage | Versioning + cross-region/copy | 30+ days |
| Secrets | Vault backup procedure | — |

| Target | Initial |
| --- | --- |
| RPO | ≤ 24h (improve with PITR) |
| RTO | ≤ 8h for v1 multi-tenant deployment |

- Restore procedure documented and **tested quarterly**  
- Backups encrypted at rest  
- Financial + document restores verified in drill  

---

## 6. Observability ops

### Local (developer laptop)

| Piece | Detail |
| --- | --- |
| Jaeger | Optional. `docker compose up -d jaeger` → UI http://localhost:16686 ; OTLP HTTP `:4318`. Apps export only when `MANAGEMENT_OTLP_TRACING_EXPORT_ENABLED=true`. |
| Services | `api-gateway` (`spring.application.name=api-gateway`) and `ims-application` both export OTLP traces |
| Sampling | `management.tracing.sampling.probability=1.0` locally |
| Propagation | W3C `traceparent` + `X-Request-Id` |
| Console logs | Pattern includes `[traceId,spanId] requestId=… userId=…` on both processes |
| Access logs | Every non-health HTTP call logged by gateway and application filters |

**How to correlate one login:**

1. Call via gateway (`:8088`).
2. Copy `traceId` from either console line (or Jaeger).
3. Grep both consoles for that `traceId` / `requestId` — gateway and app share them.
4. Open Jaeger → search service `api-gateway` or `ims-application` → same trace shows both spans.

### Production expectations

- Structured logs shipped centrally (JSON encoder can replace plain console pattern later)  
- Traces sampled in prod (lower probability)  
- Alerts: error rate, payment failures, disk, backup job failure  
