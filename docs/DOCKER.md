# AdminAI — Docker Guide

Everything the backend needs runs in containers. This guide covers the services, configuration, lifecycle, and troubleshooting.

## Services

| Service        | Image                    | Port  | Role |
|----------------|--------------------------|-------|------|
| `postgres`     | `pgvector/pgvector:pg16` | 5432  | Relational DB **and** vector store (pgvector). |
| `ollama`       | `ollama/ollama:latest`   | 11434 | Local inference server for the `bge-m3` embedding model. |
| `ollama-init`  | `ollama/ollama:latest`   | —     | One-shot job: pulls `bge-m3`, then exits. |
| `backend`      | built from `./backend`   | 8080  | Spring Boot API. |

All services share the `adminai-net` bridge network and resolve each other by service name (e.g. the backend reaches Postgres at `postgres:5432`).

## Volumes (persistent data)

| Volume                    | Contents |
|---------------------------|----------|
| `adminai-pgdata`          | PostgreSQL data (users, requests, embeddings…). |
| `adminai-ollama-data`     | Downloaded models (so `bge-m3` isn't re-downloaded). |
| `adminai-backend-storage` | Uploaded administrative documents. |

## Configuration

All configuration is passed via environment variables from `.env` (never hardcoded). See [`.env.example`](../.env.example) for every variable and inline documentation. Key groups:

- **Postgres:** `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_PORT`
- **Backend/security:** `BACKEND_PORT`, `JWT_SECRET`, `JWT_ACCESS_EXPIRATION_MS`, `JWT_REFRESH_EXPIRATION_MS`
- **LLM:** `LLM_BASE_URL`, `LLM_MODEL`, `LLM_API_KEY`, `LLM_TEMPERATURE`, `LLM_MAX_TOKENS`
- **Embeddings:** `EMBEDDING_BASE_URL`, `EMBEDDING_MODEL`, `EMBEDDING_DIMENSION`

> **Secrets:** `.env` is git-ignored. Generate a strong JWT secret with `openssl rand -base64 48`.

## Lifecycle

```bash
# Start everything (build backend image if needed)
docker compose up -d

# First-time / after backend code changes
docker compose up -d --build backend

# Watch logs
docker compose logs -f backend
docker compose logs -f postgres

# Status & health
docker compose ps

# Stop, keep data
docker compose down

# Stop and DELETE all data (fresh start)
docker compose down -v
```

## Database initialization

- On the **first** startup (empty `pgdata` volume), scripts in `infra/postgres/init/` run automatically and enable the `vector`, `pg_trgm`, and `pgcrypto` extensions.
- The backend's **Flyway** migrations (`backend/src/main/resources/db/migration/`) then create and version the application schema.
- To re-run initialization from scratch: `docker compose down -v && docker compose up -d`.

## GPU acceleration (optional)

Ollama runs on CPU by default (fine for embeddings). To use an NVIDIA GPU, install the NVIDIA Container Toolkit and uncomment the `deploy.resources.reservations.devices` block under the `ollama` service in `docker-compose.yml`.

## Troubleshooting

| Symptom | Cause / Fix |
|---------|-------------|
| `backend` restarts / unhealthy | Check logs: `docker compose logs backend`. Usually Postgres not ready yet (it waits) or a bad `LLM_API_KEY`. |
| Port already in use | Change `BACKEND_PORT` / `POSTGRES_PORT` / `OLLAMA_PORT` in `.env`. |
| Embeddings fail | Ensure `ollama-init` completed: `docker compose logs ollama-init`. Re-run: `docker compose run --rm ollama-init`. |
| `bge-m3` download slow | It's ~2 GB; it's cached in `adminai-ollama-data` after the first pull. |
| Schema validation error on boot | A migration is missing/out of sync. For dev, reset with `docker compose down -v`. |
| Need a DB shell | `docker compose exec postgres psql -U adminai -d adminai` |
| Verify pgvector | In psql: `SELECT * FROM pg_extension WHERE extname = 'vector';` |

## Health checks

Each service defines a healthcheck so `depends_on: condition: service_healthy` gates startup order:
- **postgres:** `pg_isready`
- **ollama:** `ollama list`
- **backend:** `GET /actuator/health` returns `UP`
