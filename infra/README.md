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
| Jaeger UI | http://localhost:16686 |

Stop:

```bash
docker compose down
```
