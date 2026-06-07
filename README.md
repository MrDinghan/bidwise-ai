# BidWise AI

A real-time auction marketplace for international students buying and selling
secondhand goods. AI tackles the three big pain points: not wanting to write
descriptions, not knowing how to price, and fear of getting ripped off.

> Current stage: **P3 complete — settlement / payment**. Features are built
> depth-first across milestones P0–P6 (see the Roadmap below and [`DESIGN.md`](DESIGN.md)).

## Roadmap
Depth-first: get the bidding/payment spine to production grade before layering in AI.

- [x] **P0 — Project foundation**: skeleton, JWT auth, Docker, Jenkins CI,
  contract-first pipeline.
- [x] **P1 — Listings**: listing CRUD + search/filter/sort (full-stack).
- [x] **P2 — Real-time bidding**: Redis atomic price, anti-snipe, auto-close,
  WebSocket live updates, bidding UI.
- [x] **P3 — Settlement / payment**: deposit pre-auth (pluggable gateway — in-memory
  fake by default, Stripe test-mode adapter), automatic settlement on close (capture
  winner / release losers), idempotent webhooks, bounded auction duration.
- [ ] **P4 — AI smart listing**: vision LLM auto-fills title/category/description +
  RAG-based suggested price.
- [ ] **P5 — AI valuation & moderation**: fair-value/bidding assistant, AI content
  moderation + human review console.
- [ ] **P6 — Hardening**: test coverage, notifications, cloud deploy.

## Tech stack
- Backend: Spring Boot 3.x / Java 21 (Maven), Postgres, Redis, Flyway
- Frontend: React + TypeScript (Vite), TanStack Query
- Contract-first: backend springdoc emits OpenAPI 3 → frontend Orval generates
  hooks/types (zero manual sync)
- Containers: Docker / docker-compose
- CI: Jenkins (local)

## Quick start
```bash
cp backend/.env.example backend/.env     # backend + DB + JWT (also feeds postgres)
cp frontend/.env.example frontend/.env   # frontend
docker-compose up --build                # postgres + redis + backend + frontend
```
> Convention: env files live in each subproject; no env/secrets at the repo root.
> See `CLAUDE.md`.

- Backend: http://localhost:8080  (Swagger UI: http://localhost:8080/swagger-ui.html)
- Frontend: http://localhost:3000
- OpenAPI JSON: http://localhost:8080/v3/api-docs  (importable into Postman)

## Local development
```bash
# Backend
cd backend && ./mvnw spring-boot:run

# Frontend (after the backend is running) — uses pnpm
cd frontend && corepack enable && pnpm install && pnpm run gen:api && pnpm run dev
```

## Contract pipeline
The backend is the single source of truth for the API. After changing a backend
endpoint:
```bash
cd frontend && pnpm run gen:api   # regenerate src/api/generated/
```
Frontend business code only calls generated hooks (`useLogin`, `useRegister`,
`useMe`, …) — never hand-written request/response types.

## Quality
```bash
cd backend && ./mvnw verify      # checkstyle + spotbugs + tests + jacoco + export openapi.json
cd frontend && pnpm run lint && pnpm test
```
