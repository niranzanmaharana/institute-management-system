# 13 — Object storage (MinIO / S3)

**Document purpose:** What MinIO is in this project, how IMS uses it for person documents, and how to point the app at another S3-compatible store later.  
**Companions:** [08-security-and-privacy.md](./08-security-and-privacy.md) §4, [04-business-rules.md](./04-business-rules.md) BR-DC-*, [infra/README.md](../infra/README.md)  
**Status:** Phase 3.4 — person documents (pre-signed upload)

---

## 1. What MinIO is

**MinIO** is a self-hosted object store that speaks the **Amazon S3 API**. Files are stored as objects in a **bucket**, addressed by a **key** (a path-like string), not as rows in MySQL.

In IMS, MinIO is the **local stand-in for Amazon S3** (or any S3-compatible cloud). The application does **not** import MinIO-specific libraries. It uses the AWS SDK v2 S3 client against a configurable endpoint. That is why swapping MinIO for real S3 later is a configuration change, not a rewrite.

IMS does **not** put identity-document bytes in MySQL. The database holds only **metadata** (`person_documents`). The file lives in the bucket.

| Role | Local (dev) | Production later |
| --- | --- | --- |
| Object store | MinIO in Docker (`infra/docker-compose.yml`) | AWS S3, Cloudflare R2, GCS S3 interop, or a hosted MinIO |
| API | http://localhost:9000 | Provider endpoint (HTTPS) |
| Console | http://localhost:9001 — `minio` / `minio12345` | Provider console / IAM |
| Bucket | `ims-private` (created on first upload) | Same idea; private, no public ACL |

Docker Compose is **only** required locally if you want document upload/download. The rest of IMS (MySQL on `localhost:3306`, Angular, API) runs without MinIO.

---

## 2. How IMS uses it

### 2.1 Flow (pre-signed, not proxy)

Bytes never go through the API Gateway on the happy path:

```text
Browser (logged-in Angular)
    │  1. POST /api/v1/people/{ownerType}/{ownerId}/documents/uploads
    │     (JWT → authz + tenant; validate type/size)
    ▼
ims-application
    │  2. Insert person_documents row (status PENDING)
    │     storage_key = institutes/{instituteId}/{ownerType}/{ownerId}/{uuid}/{fileName}
    │     Return time-limited PUT URL
    ▼
Browser
    │  3. PUT file directly to MinIO/S3 (signed URL; no JWT)
    │  4. POST .../documents/{id}/complete  (HEAD object; status ACTIVE; checksum)
    ▼
Download: GET .../documents/{id}/download → short-lived GET URL → browser opens it
```

Rules (BR-DC-01–04): PDF / JPEG / PNG; max **5 MB**; private bucket; time-limited URLs; metadata stores content type, size, checksum, `uploaded_by`, status.

### 2.2 What lives where

| Store | Contents |
| --- | --- |
| MySQL `person_documents` | Tenant, owner (student/faculty/staff), doc type, file name, **storage_key**, content type, size, checksum, status (`PENDING` / `ACTIVE` / `DELETED`), uploader |
| MinIO/S3 object | The file bytes at `storage_key` |
| JWT / RBAC | `document:read` / `document:write`; institute **ADMIN** may use the APIs by role |

Cross-tenant: lookup is always `institute_id` from the JWT. The object key **must** start with `institutes/{thatId}/`. Another tenant’s document id returns **404**. Soft-delete marks metadata `DELETED`; the object is not purged yet (lifecycle TBD).

### 2.3 CORS (why the browser can PUT)

The Angular origin (`http://localhost:4200`) is **not** the MinIO origin (`http://localhost:9000`). On first use, the app applies bucket CORS so the browser may `PUT`/`GET`/`HEAD` with `Content-Type`. Cloud S3 needs the equivalent CORS on the bucket if the browser still uploads directly.

### 2.4 Code map

| Piece | Location |
| --- | --- |
| Port | `com.ims.platform.storage.ObjectStorage` |
| MinIO/S3 adapter | `S3ObjectStorage` (`ims.storage.type=s3`, default) |
| In-memory adapter | `InMemoryObjectStorage` (`ims.storage.type=memory` — tests only) |
| Settings | `ims.storage.*` in `ims-application` `application.yml` |
| APIs | `/api/v1/people/{STUDENT\|FACULTY\|STAFF}/{id}/documents` |
| UI | Student / faculty / staff detail → Documents |

Compose service: `infra/docker-compose.yml` → `minio` (ports **9000** API, **9001** console). Start just that service:

```bash
cd infra
docker compose up -d minio
```

Docker Desktop must be **running** (Windows: whale icon idle). If Compose prints `dockerDesktopLinuxEngine` / pipe not found, start Docker Desktop first.

---

## 3. Local configuration (current)

`ims-application/src/main/resources/application.yml`:

```yaml
ims:
  storage:
    type: s3
    endpoint: http://localhost:9000
    access-key: minio
    secret-key: minio12345
    bucket: ims-private
    region: us-east-1
    path-style: true          # required for MinIO
    presign-expiry-minutes: 10
    max-size-bytes: 5242880
    cors-origins:
      - http://localhost:4200
      - http://127.0.0.1:4200
```

These values are **dev defaults** (same as Compose `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD`). Do not reuse them in production.

---

## 4. Using an alternate store later

Any product that implements **S3** (path-style or virtual-hosted) can replace MinIO. Keep `type: s3`. Change endpoint, credentials, bucket, region, and path-style.

Prefer **environment variables** or a secrets manager over committing production keys. Spring maps `IMS_STORAGE_ENDPOINT` → `ims.storage.endpoint` (and the same pattern for the other fields).

### 4.1 Amazon S3

```yaml
ims:
  storage:
    type: s3
    endpoint: https://s3.ap-south-1.amazonaws.com   # or omit a custom endpoint if you add region-only wiring
    access-key: ${IMS_STORAGE_ACCESS_KEY}
    secret-key: ${IMS_STORAGE_SECRET_KEY}
    bucket: ims-prod-private
    region: ap-south-1
    path-style: false
    cors-origins:
      - https://app.example.com
```

Checklist:

- Bucket **private**; block public access.
- IAM user/role: `s3:PutObject`, `GetObject`, `HeadObject`, `CreateBucket` (if the app still creates the bucket), `PutBucketCors` (or set CORS once in the console and drop create-on-startup).
- Presigned URLs must be reachable **from the user’s browser** (public S3 endpoint or a CloudFront origin that supports signed S3).
- Set CORS on the bucket to the real UI origin if the browser still PUTs directly.
- `path-style: false` is normal for AWS. MinIO local stays `true`.

If the SDK needs “no custom endpoint” for AWS, a small adapter tweak may be needed so `endpoint` is optional when empty. Until then, set the regional S3 endpoint explicitly as above.

### 4.2 Cloudflare R2

R2 is S3-compatible. Use the account endpoint, R2 access key/secret, `region` as `auto` (or whatever R2 documents), and typically `path-style: false`. Same `ims.storage.*` keys.

### 4.3 Another MinIO (on-prem / VM)

Same as local: set `endpoint` to `https://minio.internal.example`, keys, bucket, `path-style: true`. Put TLS on the reverse proxy. Keep the bucket private.

### 4.4 Google Cloud Storage

Use GCS **S3 interoperability** keys and the GCS S3-compatible endpoint, or add a new `ObjectStorage` implementation. Prefer interoperability first so `type: s3` still works.

### 4.5 New adapter (non-S3)

If a provider is not S3-compatible:

1. Implement `ObjectStorage` (`presignPut`, `presignGet`, `head`).
2. Register it with `@ConditionalOnProperty(name = "ims.storage.type", havingValue = "…")`.
3. Leave `S3ObjectStorage` as the default.
4. Do not change `person_documents.storage_key` format (`institutes/{instituteId}/…`) unless you migrate existing objects.

`ims.storage.type=memory` is **not** a production alternative; it only exists so tests can run without MinIO.

### 4.6 Cutover notes

- Existing objects stay in the old bucket until copied (`mc mirror`, `aws s3 sync`, or vendor replication).
- After cutover, old pre-signed URLs (already expired in minutes) do not matter; metadata rows keep the same keys if you **preserve key layout** in the new bucket.
- Update `cors-origins` to production UI origins.
- Rotate keys; never commit production `access-key` / `secret-key`.
- Gateway CORS is **not** the same as bucket CORS. Gateway covers `/api/**`. Direct PUT/GET to the bucket needs **bucket** CORS (or a future backend-proxy upload).

---

## 5. Troubleshooting (local)

| Symptom | Likely cause |
| --- | --- |
| Compose: `dockerDesktopLinuxEngine` / pipe not found | Docker Desktop not running |
| UI: cannot reach object storage | MinIO container down; start `docker compose up -d minio` |
| Upload PUT fails in the browser (CORS) | Bucket CORS origins must include the UI origin; app applies this on first upload to MinIO |
| API 403 on document APIs | JWT lacks `document:read`/`document:write` (re-login after Flyway V13); institute ADMIN is also allowed |
| Empty Documents section (older UI) | Hard-refresh the SPA after pulling the documents UI |

---

## 6. Related

- Flyway: `V13__person_documents.sql`
- Step: [11](./11-mvp-v1-step-by-step-development.md) Phase 3.4
