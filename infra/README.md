# Local infrastructure

```bash
docker compose up -d
docker compose ps
```

| Service | URL / port |
| --- | --- |
| MySQL (Docker, optional) | `localhost:3307` — db `ims`, user/pass `ims`/`ims`, root/`root`. App defaults to **local** MySQL on `3306` with `root`/`root` — see `ims-application` `application.yml`. |
| MinIO API | http://localhost:9000 |
| MinIO Console | http://localhost:9001 — `minio` / `minio12345` |
| MinIO private bucket | App creates `ims-private` on first document upload; CORS allows `http://localhost:4200` for browser PUT |
| Jaeger UI | http://localhost:16686 — optional tracing; OTLP HTTP `:4318`. Off in app config until `MANAGEMENT_OTLP_TRACING_EXPORT_ENABLED=true`. |

Person documents and swapping MinIO for cloud S3: **[docs/13-object-storage.md](../docs/13-object-storage.md)**.

Docker Desktop must be running before Compose. Start **only MinIO** if MySQL is already local:

```bash
docker compose up -d minio
```

Stop:

```bash
docker compose down
```
