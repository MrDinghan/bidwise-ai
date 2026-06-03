# BidWise AI

A real-time auction marketplace for international students buying and selling
secondhand goods. AI tackles the three big pain points: not wanting to write
descriptions, not knowing how to price, and fear of getting ripped off.

> Current stage: **P0 — project foundation** (skeleton + JWT auth + Docker +
> Jenkins + contract pipeline). Business features (bidding / payment / AI) are
> built depth-first across milestones P1–P6; see the design doc.

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
