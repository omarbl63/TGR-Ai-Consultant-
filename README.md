# AdminAI — AI Administrative Agent

> An enterprise platform where employees interact with an **AI agent** in natural language instead of filling administrative forms. The AI understands the request, retrieves internal regulations via **Retrieval-Augmented Generation (RAG)**, validates compliance, drafts the official document, explains its reasoning, and produces a **recommendation for a human approver**. The AI never decides — a manager or director always makes the final call.

The knowledge base simulates a **Moroccan public administration** (documents in French). It is a simulated dataset created for this project, not official documents of any institution.

---

## Architecture at a glance

```
                        ┌─────────────────────────────┐
   Employee (mobile)    │  Expo / React Native app     │
   ────────────────────▶│  ChatGPT-style AI chat        │
                        └──────────────┬──────────────┘
                                       │ REST + JWT
                        ┌──────────────▼──────────────┐        ┌──────────────────┐
   Manager (web)        │      Spring Boot backend      │───────▶│  Qwen3 (LLM)     │
   ────────────────────▶│  Auth · Requests · AI         │  cloud │  OpenAI-compatible│
   Angular dashboard    │  Orchestrator · RAG · LLM     │        └──────────────────┘
                        └───────┬───────────────┬──────┘
                                │               │ embeddings
                    ┌───────────▼──┐     ┌──────▼───────────┐
                    │ PostgreSQL   │     │ Ollama (bge-m3)  │
                    │ + pgvector   │     │  local, Docker   │
                    └──────────────┘     └──────────────────┘
```

| Layer            | Technology                                             |
|------------------|--------------------------------------------------------|
| Employee app     | React Native · Expo · TypeScript · NativeWind          |
| Approver dashboard | Angular · TypeScript · Angular Material · Tailwind    |
| Backend          | Spring Boot · Java 21 · Spring Security · JPA · JWT     |
| AI framework     | LangChain4j                                             |
| LLM              | **Qwen3** via an OpenAI-compatible cloud endpoint       |
| Embeddings       | **BAAI/bge-m3** served locally by Ollama (1024-dim)     |
| Database + vectors | PostgreSQL 16 + **pgvector**                          |
| Infrastructure   | Docker · Docker Compose                                 |

---

## Repository layout

```
Project/
├── docker-compose.yml         # Orchestrates postgres, ollama, backend
├── .env / .env.example        # Environment configuration
├── infra/
│   ├── postgres/init/         # Enables vector/pgcrypto/pg_trgm on first boot
│   └── ollama/                # Embedding model bootstrap
├── backend/                   # Spring Boot API (Java 21, Maven, Dockerized)
├── dashboard/                 # Angular 19 approver dashboard (Tailwind, nginx)
├── mobile/                    # Expo employee app (React Native, NativeWind)
├── knowledge-base/            # French administrative corpus for RAG
└── docs/                      # Architecture & design docs
```

---

## Quick start

### 1. Prerequisites
- Docker + Docker Compose
- (Later) Node.js 20+ for the Angular dashboard and Expo app

### 2. Configure the environment
```bash
cp .env.example .env
# Edit .env and set LLM_API_KEY (OpenRouter or DashScope) and a strong JWT_SECRET.
```

### 3. Start everything
```bash
docker compose up -d
```
This starts:
- **postgres** with pgvector (port 5432)
- **ollama** and pulls the **bge-m3** embedding model (first run downloads ~2 GB)
- **backend** Spring Boot API (port 8080)
- **dashboard** Angular approver UI via nginx (port 4200) → open **http://localhost:4200**

First boot takes a few minutes while images build and the embedding model downloads.

### 4. Verify
```bash
docker compose ps
curl http://localhost:8080/actuator/health          # -> {"status":"UP"}
```
Open the API docs: **http://localhost:8080/swagger-ui.html**

### 5. Demo accounts (seeded automatically)
| Role      | Email                   | Password       |
|-----------|-------------------------|----------------|
| Employee  | employe@adminai.ma      | `Password123!` |
| Manager   | manager@adminai.ma      | `Password123!` |
| Director  | directeur@adminai.ma    | `Password123!` |
| Admin     | admin@adminai.ma        | `Password123!` |

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"manager@adminai.ma","password":"Password123!"}'
```

---

## RAG: index the knowledge base and search it

Once the stack is up and the **bge-m3** model has been pulled (the `ollama-init` service does this automatically on first `up`), index the French corpus and query it. Sign in as `admin@adminai.ma` to get a token, then:

```bash
# Index every document under knowledge-base/ (parse → chunk → embed → pgvector)
curl -X POST http://localhost:8080/api/v1/rag/reindex \
  -H "Authorization: Bearer $TOKEN"

# Semantic search — returns the most relevant chunks with citations (title, docRef, score)
curl -X POST http://localhost:8080/api/v1/rag/search \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"query":"Combien de jours à l’avance demander un ordre de mission international ?","topK":3}'

# Upload a new regulation (PDF / DOCX / TXT / MD)
curl -X POST http://localhost:8080/api/v1/rag/documents \
  -H "Authorization: Bearer $TOKEN" -F "file=@my-policy.pdf" -F "category=03-finance"
```

Endpoints: `POST /api/v1/rag/reindex` and `POST /api/v1/rag/documents` require the `ADMIN` role; `POST /api/v1/rag/search` and `GET /api/v1/rag/documents` are available to any authenticated user.

## Talk to the AI agent

With the stack up and the corpus indexed, sign in and chat (the agent detects intent, asks for missing info, creates & submits the request, and analyzes it):

```bash
# Ask an administrative question (RAG-grounded, cited)
curl -X POST http://localhost:8080/api/v1/ai/ask -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"question":"Quel est le délai pour un ordre de mission international et qui le valide ?"}'

# Converse — the agent creates the request from natural language
curl -X POST http://localhost:8080/api/v1/ai/chat -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"message":"Je dois aller à Rabat le 20 juillet 2026, retour le 21, pour une formation. Je prends le train."}'

# Approver: (re)run analysis / ask about a specific request
curl -X POST http://localhost:8080/api/v1/ai/requests/$REQUEST_ID/analyze -H "Authorization: Bearer $TOKEN"
curl -X POST http://localhost:8080/api/v1/ai/requests/$REQUEST_ID/ask -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"question":"Pourquoi cette recommandation ?"}'
```

> Requires a valid `LLM_API_KEY` in `.env` (OpenRouter/DashScope). `/ai/analyze` and `/ai/requests/*/ask` are approver-only; `/ai/chat` and `/ai/ask` are for any authenticated user.

## Dashboard (development with hot reload)

The dashboard runs in Docker via `docker compose up -d` (nginx on port 4200). For live development with hot reload, run Angular directly and proxy to the backend:

```bash
cd dashboard
npm install
npm start                                   # http://localhost:4200, proxies /api → :8080
# if your backend is on another port, use the provided local proxy:
npm start -- --proxy-config proxy.local.json   # proxies /api → :18080
```

Sign in with any seeded approver account (e.g. `manager@adminai.ma` / `Password123!`).

## Employee app (Expo)

```bash
cd mobile
npm install
npm start        # press w (web), i (iOS), or a (Android)
```

Sign in as `employe@adminai.ma` / `Password123!` and talk to the agent. See [mobile/README.md](mobile/README.md) for device/emulator API-URL configuration.

## Common commands

| Action                         | Command                                  |
|--------------------------------|------------------------------------------|
| Start everything               | `docker compose up -d`                   |
| Rebuild the backend            | `docker compose up -d --build backend`   |
| Follow backend logs            | `docker compose logs -f backend`         |
| Stop (keep data)               | `docker compose down`                    |
| Stop and **wipe all data**     | `docker compose down -v`                 |
| Open a psql shell              | `docker compose exec postgres psql -U adminai -d adminai` |
| Re-pull embedding model        | `docker compose run --rm ollama-init`    |

See [docs/DOCKER.md](docs/DOCKER.md) for the full Docker guide and troubleshooting.

---

## Roadmap

- [x] **Phase 0 — Foundation:** monorepo, Docker infra (Postgres/pgvector + Ollama), Spring Boot skeleton, JWT auth, seeded users, OpenAPI.
- [x] **Phase 1 — Knowledge base:** French administrative corpus — handbook, leave/mission/expense/travel policies, budget rules, supporting-docs guide, validation rules, org chart, FAQ, templates & a sample decision (12 documents across 6 categories).
- [x] **Phase 2 — RAG pipeline:** document ingestion (PDF/DOCX/TXT/MD), recursive chunking (~800–1000 tokens, ~18% overlap), bge-m3 embeddings, pgvector store with ivfflat cosine index, semantic search returning cited chunks. *Verified end-to-end: 13 docs → 18 chunks, French queries retrieve at 0.74–0.84 similarity.*
- [x] **Phase 3 — Domain:** conversations & messages, administrative requests (leave / mission order / expense) with JSONB structured data, attachments, approval workflow (submit → decision) with a timeline, AI-analysis persistence (+ citations), notifications, and an append-only audit log. *Verified end-to-end: create → submit → approve, role-based access (403), KPIs, notifications, audit.*
- [x] **Phase 4 — AI Orchestrator:** Qwen3 (via OpenRouter) + RAG. Intent detection, entity extraction, clarification questions, request creation & submission from natural language, RAG-grounded compliance analysis (summary, confidence/risk scores, missing info/documents, anomalies, recommendation) with citations, plus grounded Q&A and per-request "ask the AI". *Verified live end-to-end: a French mission-order conversation produced a submitted request with a persisted, cited recommendation.*
- [x] **Phase 5 — Approver dashboard:** Angular 19 + Tailwind SaaS UI (French) — JWT auth, KPI cards, pending queue, request detail with the **AI analysis panel** (recommendation, confidence/risk, missing info/docs, anomalies, reasoning, citations), approval timeline, approve/reject/request-changes, ask-the-AI, notifications. Served by nginx in Docker. *Verified live: login → dashboard → open request → approve updates status & timeline.*
- [x] **Phase 6 — Employee app:** Expo (SDK 57) + NativeWind mobile app (French) — login, ChatGPT-style **AI chat** (suggested prompts, typing indicator, in-chat request cards), "Mes demandes" tracking, and request detail with the AI analysis. *Verified live via Expo web: a French mission conversation created & submitted a request with a cited recommendation.*

---

## Documentation
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — system design and decisions
- [docs/DOCKER.md](docs/DOCKER.md) — containerization guide

---

## Disclaimer
This project is an academic simulation. The administrative knowledge base is **inspired by** common Moroccan public-administration practices and is **not** an official publication of any institution.
