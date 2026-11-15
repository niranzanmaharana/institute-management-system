# 08 — Security & privacy

**Status:** Active for v1 — Step 7.1 checklist filed under [security/7.1-security-pass-checklist.md](./security/7.1-security-pass-checklist.md)

---

## 1. Authentication & session

- BCrypt password hashes  
- Short-lived access JWT; rotatable refresh tokens hashed at rest  
- JWT claims include **`institute_id`** (mandatory for institute users)  
- Lock account after N failed logins (configurable: `ims.security.max-failed-logins`, `ims.security.lock-minutes`)  
- Generic login error messages to clients; detailed login_audit internally  
- **Token transport (v1):** Bearer access JWT + refresh token in JSON body; UI stores tokens in `localStorage`. Spring CSRF is disabled because auth is not cookie-based. If refresh moves to HttpOnly cookies, add an explicit CSRF strategy before enabling.  

---

## 2. Authorization & tenant isolation

- Enforce permission matrix (`06-authorization-matrix.md`) on every use case  
- **TenantContext** from JWT; all repository access scoped by `institute_id`  
- Object-level checks: student self-access; faculty assigned batches — **within tenant**  
- Never trust client-sent `studentId` / `instituteId` for authorization  
- Cross-tenant IDOR tests required for every new resource API  
- `PLATFORM_ADMIN` cannot use normal institute APIs as a silent superuser over all data  

---

## 3. PII & student data

| Data | Handling |
| --- | --- |
| Phone, email, DOB, ID docs | Minimize in logs; mask in INFO; tenant-bound |
| Identity documents | Private object storage under `institutes/{id}/...`; signed URLs; retention policy |
| Exports | Role-gated; audit download; tenant-only |

Compliance posture: design for reasonable privacy (India DPDP-aware practices) — legal review before production. Multi-tenant isolation is a **security control**, not only a product feature.

---

## 4. Document storage architecture

```text
Angular → API (authz + tenant) → pre-signed upload URL → client PUT to MinIO/S3
path: institutes/{institute_id}/...
```

| Control | v1 rule |
| --- | --- |
| Types | pdf, jpeg, png |
| Max size | 5 MB (configurable) |
| Scan | Hook point; optional ClamAV later |
| Access | Private bucket; no public ACLs; tenant prefix mandatory |
| Metadata | content_type, size, checksum, uploaded_by, status, institute_id |
| Retention | Soft-delete metadata; lifecycle rules on bucket TBD |

Backend proxy upload allowed as fallback for local/dev.

**Local vs production:** Dev uses **MinIO** (Docker) as an S3-compatible store. The app talks S3 (`ims.storage.*`); swap MinIO for AWS S3 / R2 / hosted MinIO later without changing document APIs. See **[13-object-storage.md](./13-object-storage.md)**.

---

## 5. Audit requirements (business-critical)

Append-only audit for at least:

- Institute created/suspended (platform)  
- Student created/updated/deleted (soft)  
- Enrollment created/transferred/withdrawn/activated  
- Fee plan changed on account  
- Invoice cancelled  
- Payment created/reversed  
- Refund approved/processed  
- Attendance modified after submit / unlocked  
- Marks changed / result published/locked  
- User role changed / password reset  

Fields: **institute_id**, actor, action, entity, before, after, reason, trace/request id, timestamp, IP when available.

---

## 6. Application security baseline

- HTTPS only at edge  
- Secrets in env/secret manager — never git (see root `.env.example`; prod profile rejects default JWT secret)  
- Dependency scanning: Dependabot + Maven profile `-Pdependency-check` (`failBuildOnCVSS=9`); snapshot in [security/7.1-dependency-scan.md](./security/7.1-dependency-scan.md)  
- SQL via parameterized ORM/queries  
- CSRF: not applicable for Bearer/localStorage v1; document before any cookie-session change (see §1)  
- Automated test: two tenants, same numeric id patterns, zero leakage (`TenantIsolationIT`, `SliceIsolationIT`)  
- Gateway rate-limits `POST /auth/login` and `/auth/refresh`; prefer Redis in multi-instance prod  
- OpenAPI/Swagger public only when `ims.security.expose-openapi=true` (off in `prod` profile)  

**v1 gate artifact:** [security/7.1-security-pass-checklist.md](./security/7.1-security-pass-checklist.md)
