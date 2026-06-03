# CLAUDE.md

Guidance for working in this repository (read before making changes).

## What this is
BidWise AI — a real-time auction marketplace for international students selling
secondhand goods. Monorepo: Spring Boot backend + React/TS frontend. Current
milestone is **P0 (project foundation)**; the roadmap is in the README and design
doc, and is depth-first (get the bidding/payment spine to production grade before
layering in AI).

## Repository layout
```
bidwise-ai/
├── CLAUDE.md            # this file: repo conventions
├── README.md           # getting started & commands
├── docker-compose.yml  # engineering orchestration only (postgres/redis/backend/frontend)
├── Jenkinsfile         # CI pipeline
├── backend/            # Spring Boot (Java 21, Maven) — self-contained subproject
└── frontend/           # React + TS (Vite) — self-contained subproject
```

## Repository conventions (must follow)

### 1. The repo root holds engineering/project-level files only
Only files meaningful to the **whole repo** belong at the root: `README.md`,
`CLAUDE.md`, `docker-compose.yml`, `Jenkinsfile`, the root `.gitignore`, and future
repo-wide CI/tooling config. Do **not** put anything that belongs to a single
subproject at the root, and do not put non-engineering scratch files, secrets, or
notes there.

### 2. Layered .gitignore
- Root `.gitignore`: **common / language-agnostic** ignores only (OS files like
  `.DS_Store`, IDE dirs like `.idea/`, editor files, `*.log`).
- Language/tool-specific ignores live in the relevant subproject:
  - `backend/.gitignore`: `target/`, Maven artifacts, etc.
  - `frontend/.gitignore`: `node_modules/`, `dist/`, `coverage/`, etc.
- Do not put subproject-specific rules like `node_modules/` or `target/` in the
  root `.gitignore`.

### 3. Env / secrets live per subproject; no env at the root
- Backend and database variables → `backend/.env` (template `backend/.env.example`).
  The backend owns the database, so `POSTGRES_*` lives here too; `docker-compose.yml`
  injects it into both the postgres and backend services via `env_file: ./backend/.env`.
- Frontend variables (`VITE_*`) → `frontend/.env` (template `frontend/.env.example`).
- **No `.env` at the repo root.** Accordingly, `docker-compose.yml` does not rely on
  root-level `${VAR}` interpolation — it uses each service's `env_file`. Variables that
  must expand inside the container are written `$$VAR` (e.g. in healthcheck commands).
- `.env.example` files are committed (placeholder values); real `.env` files are never
  committed (already ignored per subproject).

### 4. Contract-first (zero manual API sync)
The backend is the single source of truth for the API: springdoc emits OpenAPI 3
from annotations/DTOs (`/swagger-ui.html`, `/v3/api-docs`); `OpenApiSpecExportTest`
exports `backend/target/openapi.json` during `mvn test/verify`. The frontend uses
**Orval** to generate TanStack Query hooks + types into `frontend/src/api/generated/`.
- Business code only calls generated hooks; **never** hand-write API requests/types.
- Do not hand-edit `frontend/src/api/generated/` (regenerate with `pnpm run gen:api`).
- Change a backend endpoint → regenerate → a frontend compile/CI failure surfaces drift.

## Common commands
```bash
# Backend: tests + spec + quality gates + export openapi.json
cd backend && ./mvnw verify

# Frontend: generate client from the spec, then lint/test/build (uses pnpm)
cd frontend && OPENAPI_SPEC=../backend/target/openapi.json pnpm run gen:api \
  && pnpm run lint && pnpm test && pnpm run build

# Whole stack (create both .env files first)
docker-compose up --build
```

## Quality gates (keep green for any change)
- Backend: Checkstyle (`backend/checkstyle.xml`) + SpotBugs
  (`backend/spotbugs-exclude.xml`) + JaCoCo coverage threshold. The core domain
  (bidding/settlement/payment) will be raised to 90%+ later.
- Frontend: ESLint + Vitest (the generated dir is excluded from lint/coverage).
- Before committing, make sure `./mvnw verify` and the frontend lint/test pass; CI
  runs them again.

## Language
All project content is in English — README, CLAUDE.md, code comments, commit
messages, identifiers. Do not introduce non-English text.
