# AdminAI — Architecture

This document explains the system design and the key engineering decisions.

## 1. Design principles

- **AI is the primary interface.** Employees never fill forms; they talk to an agent. Forms and dashboards are supporting tools for approvers.
- **Human-in-the-loop.** The AI analyzes, explains, and recommends. It **never** approves or rejects. Every request ends on a human approver's desk.
- **Grounded, explainable AI.** Answers and recommendations must be backed by retrieved internal documents (RAG). Every recommendation cites its sources.
- **Clean, layered backend.** Controllers → services → repositories, DTOs at the boundary, typed configuration, centralized error handling. SOLID throughout.
- **Everything reproducible via Docker.**

## 2. Component overview

| Component            | Responsibility |
|----------------------|----------------|
| **Employee app** (Expo) | Conversational UI; sends messages to the AI; tracks generated requests. |
| **Approver dashboard** (Angular) | Review queue, AI analysis panel (summary, confidence, risk, citations, reasoning), approve/reject/ask-AI. |
| **Backend** (Spring Boot) | REST API, auth, business domain, and the AI orchestration layer. |
| **PostgreSQL + pgvector** | Relational data **and** vector embeddings in one database. |
| **Ollama** | Local inference server hosting the `bge-m3` embedding model. |
| **Qwen3** (cloud) | LLM for understanding, extraction, generation, and reasoning. |

## 3. Backend module map

The backend is organized by feature (package-by-feature), each with its own `domain`, `repository`, `service`, and `web` layers:

```
com.pfe.adminagent
├── config            # Security, OpenAPI, typed properties, seeding
├── security          # JWT service, auth filter, principal, userdetails
├── common            # Cross-cutting: ApiError, exceptions, handlers
├── user              # Users & roles
├── auth              # Register / login / refresh
├── conversation      # (Phase 3) chat threads & messages
├── request           # (Phase 3) leave / mission / expense requests
├── document          # (Phase 2) uploaded regulation documents
├── rag               # (Phase 2) chunking, embedding, retrieval
├── llm               # (Phase 4) LLM client & prompt templates
├── orchestrator      # (Phase 4) AI decision flow
├── recommendation    # (Phase 4) confidence/risk scoring, recommendations
├── notification      # (Phase 3) notifications
└── audit             # (Phase 3) audit log
```

**Why package-by-feature?** It keeps related code together, makes modules independently understandable, and scales better than package-by-layer for a system this size.

## 4. Authentication & authorization

- **Stateless JWT.** Access token (short-lived) + refresh token (long-lived), both HMAC-SHA256 signed.
- `JwtAuthenticationFilter` populates the security context from the `Authorization: Bearer` header.
- Roles: `EMPLOYEE`, `MANAGER`, `DIRECTOR`, `ADMIN`. Method-level security (`@EnableMethodSecurity`) guards approver-only endpoints.
- Passwords hashed with BCrypt.

## 5. The AI decision flow (target)

For every employee message the orchestrator will:

1. Detect the administrative **intent** (leave / mission order / expense / Q&A).
2. **Extract** structured entities (dates, destination, amount, purpose…).
3. Identify **missing information** and ask clarifying questions.
4. **Retrieve** relevant regulations from pgvector (RAG).
5. **Validate compliance** against retrieved policy excerpts.
6. Detect violations, missing documents, duplicates, scheduling conflicts.
7. **Generate** the official request document.
8. Produce a **summary**, a **confidence score**, and a **risk score**.
9. **Explain** the reasoning and produce a **recommendation**.
10. Route the request to the approver dashboard.

Each step is a distinct, testable service coordinated by the orchestrator — not one giant prompt.

## 6. RAG pipeline (target)

```
Upload → Parse (PDF/DOCX/TXT) → Clean → Chunk (800–1000 tokens, 15–20% overlap)
      → Embed (bge-m3, 1024-dim) → Store in pgvector (+ metadata)
Query → Embed → Cosine similarity search → Top-k chunks → Prompt augmentation
      → Grounded LLM response with citations
```

**Why pgvector (not Pinecone/Weaviate)?** Keeping vectors in PostgreSQL means one datastore, transactional consistency between business data and embeddings, and no external vector-DB dependency — simpler ops and a cleaner demo.

## 7. Key decisions log

| Decision | Choice | Rationale |
|----------|--------|-----------|
| LLM hosting | Qwen3 via OpenAI-compatible cloud API | Fast, reliable demo; no GPU needed on the host. |
| Embeddings | bge-m3 via local Ollama | Self-hosted RAG, multilingual (FR/EN), free at inference. |
| Vector store | pgvector | Single database, transactional, easy to operate. |
| AI framework | LangChain4j | First-class Java abstractions for LLM, embeddings, RAG. |
| Schema management | Flyway (`ddl-auto=validate`) | Versioned, reviewable migrations; Hibernate only validates. |
| Backend structure | Package-by-feature | Cohesion and modularity at scale. |
| KB language | French | Authentic to the Moroccan public-administration context. |
