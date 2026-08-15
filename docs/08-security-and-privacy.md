# 08 — Security & privacy

**Status:** Draft after architecture review (multi-tenant Option B)

---

## 1. Authentication & session

- BCrypt/Argon2 password hashes  
- Short-lived access JWT; rotatable refresh tokens hashed at rest  
- JWT claims include **`institute_id`** (mandatory for institute users)  
- Lock account after N failed logins (configurable)  
- Generic login error messages to clients; detailed login_audit internally  

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
- Secrets in env/secret manager — never git  
- Dependency scanning in CI  
- SQL via parameterized ORM/queries  
- CSRF strategy documented for cookie refresh choice  
- Automated test: two tenants, same numeric id patterns, zero leakage  
