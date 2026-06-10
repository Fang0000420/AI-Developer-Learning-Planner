# Infrastructure

Docker Compose is the default MVP deployment path.

From the repository root:

```bash
docker compose up --build
```

The historical infra entrypoint is still supported:

```bash
docker compose -f infra/docker-compose.yml up --build
```

Services:

- `postgres`: PostgreSQL 16 with `postgres-data` volume.
- `redis`: Redis 7 with `redis-data` volume.
- `agent-service`: FastAPI app on port `8000`.
- `backend`: Spring Boot app on port `8080`, using `postgres` and `agent-service` service names.
- `frontend`: Next.js standalone server on port `3000`, proxying backend requests to `backend:8080`.

Useful checks:

```bash
docker compose ps
docker compose logs -f backend agent-service frontend
curl http://localhost:8080/api/health
curl http://localhost:8000/health
curl http://localhost:3000/api/backend-health
```
