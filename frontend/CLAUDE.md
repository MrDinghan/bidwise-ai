# CLAUDE.md — frontend

Frontend-specific conventions. Inherits everything in the root `../CLAUDE.md`
(repo layout, env-per-subproject, contract-first, English-only). Rules here add to
or refine those.

## Stack
React 18 + TypeScript (Vite), TanStack Query (server state), Zustand (client state),
React Router, axios.

## Package manager: pnpm
This subproject uses **pnpm** (pinned via `packageManager` in `package.json`;
enable with `corepack enable`). Use `pnpm`, not `npm`/`yarn` — only `pnpm-lock.yaml`
is committed. Native build scripts must be allowlisted under `pnpm.onlyBuiltDependencies`
in `package.json` (currently `esbuild`).

## Conventions (must follow)

### 1. Use arrow functions
Unless there is a real reason not to, **always use arrow functions** — for
components, hooks, handlers, and helpers. Prefer:
```ts
const Login = () => { /* ... */ };
export default Login;
```
over `function Login() { ... }`.

Legitimate exceptions (where a `function` declaration is acceptable): cases that
genuinely require hoisting, `this` binding semantics, or generator functions
(`function*`). These should be rare — justify them in a comment.

### 2. Components must be explicitly typed
Every component must carry an explicit type. Use `FC` (or `FC<Props>` when it takes
props):
```ts
import type { FC } from 'react';

const App: FC = () => { /* ... */ };

interface ButtonProps { label: string; }
const Button: FC<ButtonProps> = ({ label }) => <button>{label}</button>;
```
Do not leave a component as a bare untyped `const X = () => ...`.

### 3. Use the `@/` import alias for cross-directory imports
`@/` resolves to `src/` (configured in `tsconfig.json` paths + `vite.config.ts`
resolve.alias). Import modules in another directory via the alias instead of deep
relative paths:
```ts
import { useAuthStore } from '@/auth/store';   // not ../../../auth/store
```
Same-directory imports may stay relative (`./store`). Generated files under
`src/api/generated/` keep their own relative imports (Orval-owned) — don't touch them.

### 4. Never hand-write the API client
The generated client in `src/api/generated/` is owned by Orval (`pnpm run gen:api`).
Do not edit it by hand and do not hand-roll fetch/axios calls in feature code —
call the generated hooks (`useLogin`, `useRegister`, `useMe`, …). See root
`../CLAUDE.md` §4.

### 5. Routing is file-based (do not hand-write a route tree)
Routes are generated from `src/pages/**` by **vite-plugin-pages** and consumed via
`useRoutes(routes)` in `App.tsx` (`import routes from '~react-pages'`). To add a
route, add a file under `src/pages/`:
- `src/pages/index.tsx` → `/`
- `src/pages/login.tsx` → `/login`
- `src/pages/[...all].tsx` → catch-all (redirects home)

Do not edit `App.tsx` to register routes manually. Per-route auth is declared by
wrapping a page's content in `RequireAuth` / `RequireGuest` (in `src/auth/`), not by
branching in a central router. `*.test.tsx` files under `src/pages/` are excluded
from route generation.

### 6. Client state: use Zustand (no hand-rolled stores)
Client/UI state lives in **Zustand** stores under `src/`, not in bespoke
pub/sub, contexts, or manual `useSyncExternalStore`. Persist with the `persist`
middleware instead of touching `localStorage` directly. Read state via selectors
(`useStore((s) => s.x)`); outside React use `useStore.getState()`.
- **Server state** (anything fetched from the API) stays in TanStack Query via the
  generated hooks — do not mirror it into Zustand.

### 7. Auth + HTTP
Auth state is the Zustand store `src/auth/store.ts` (`useAuthStore`), persisted to
localStorage. All requests go through the axios instance in `src/api/mutator.ts`,
which reads the token from `useAuthStore.getState()` and calls `logout()` on 401.
Don't read/write the token from `localStorage` directly anywhere.

## Commands
```bash
pnpm run gen:api   # regenerate API client from the backend OpenAPI spec
pnpm run lint      # ESLint (generated dir excluded)
pnpm test          # Vitest
pnpm run build     # type-check + production build
```
