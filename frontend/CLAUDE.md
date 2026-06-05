# CLAUDE.md — frontend

Frontend-specific conventions. Inherits everything in the root `../CLAUDE.md`
(repo layout, env-per-subproject, contract-first, English-only). Rules here add to
or refine those.

## Stack
React 18 + TypeScript (Vite), TanStack Query (server state), Zustand (client state),
React Router, axios. **UI: Tailwind CSS v4 + shadcn/ui** (Radix primitives), `sonner`
for toasts, `lucide-react` for icons.

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

### 8. Styling: Tailwind v4 + shadcn/ui ("The Auction Catalogue")
Style with **Tailwind utility classes**; build UI from the **shadcn/ui** primitives in
`src/components/ui/` (Button, Badge, Input, Label, Select, Textarea, Sonner). Compose
classes with `cn()` from `@/lib/utils`.

> **Three non-negotiables for any UI work:**
> 1. **Responsive by default.** Every component/page must work on mobile, not just
>    desktop. Design mobile-first and verify at ~360px: navigation stays reachable
>    (no destinations hidden with `hidden sm:*` and no replacement), content stacks,
>    font sizes scale, and tap targets are adequate. Use Tailwind breakpoints
>    (`sm:`/`md:`/`lg:`) rather than fixed widths.
> 2. **Dark theme always.** Both light and dark are first-class (see the theming bullet
>    below). Build with semantic tokens only — never hardcode `bg-white`/`bg-black`/hex —
>    and **sanity-check every new screen in dark mode**, not just light.
> 3. **Use the `frontend-design` skill for visual changes.** When creating or
>    restyling the look-and-feel (new components/pages, redesigns), drive it through
>    the `frontend-design` skill — don't hand-roll ad-hoc styling. Routine feature
>    scaffolding/wiring still goes through the `feature-dev` workflow.
- **Design language** is an editorial *auction-catalogue* look: warm paper background,
  near-black ink, a single sharp **persimmon `primary`** for live/CTA/price moments, and
  an amber **`gold`** reserved for winning-bid highlights. Items read as numbered "lots".
  Keep new UI consistent with this — hairline borders (`border-foreground/15`), sharp
  corners (small `--radius`), editorial serif headings, and the mono "ticker" layer for
  numbers/metadata. Subtle film-grain + paper wash live on `body` in `src/index.css`.
- **Typography:** `font-display` = Fraunces (headings — base rule already applies it to
  `h1–h4`), `font-sans` = Hanken Grotesk (body), `font-mono` = JetBrains Mono (lot
  numbers, prices, timers, labels). Fonts are loaded in `index.html`. Helper classes:
  `.label-mono` (uppercase mono meta label), `.tabular` (tabular figures),
  `.animate-lot-rise` (staggered page-load reveal — set `animationDelay` inline).
- **Design tokens** (colors, radius, fonts) live in `src/index.css` as CSS variables
  mapped through `@theme` / `@theme inline`. Use the semantic token classes
  (`bg-background`, `text-muted-foreground`, `border-border`, `bg-primary`,
  `text-gold-foreground`, …) — **don't hardcode raw colors** like `bg-[#10b981]`.
- **`src/components/ui/` are shadcn-owned primitives.** Add new ones with the shadcn CLI
  (`pnpm dlx shadcn@latest add <name>`) rather than hand-rolling; editing an existing
  primitive is allowed (unlike the Orval-generated client). They are exempt from the
  `react-refresh/only-export-components` lint rule (see `eslint.config.js`).
- Feature components/pages compose primitives + Tailwind — don't reach for a different
  component library or a bespoke CSS file. Tailwind v4 is wired via `@tailwindcss/vite`
  (no `tailwind.config.js`); the single global stylesheet is `src/index.css`.
- Toasts: `import { toast } from 'sonner'` (the `<Toaster/>` is mounted in `App.tsx`).
  Icons: `lucide-react`.
- **Light + dark mode.** Both palettes live in `src/index.css` (`:root` and `.dark`).
  Theme state is the Zustand store `src/theme/store.ts` (`useThemeStore`, persisted,
  defaults to the OS preference); `App.tsx` toggles the `dark` class on
  `document.documentElement`, and an inline script in `index.html` applies it before
  paint to avoid a flash. Because everything uses semantic tokens, new UI works in both
  themes automatically — **always sanity-check new screens in dark too**. For inverted
  chips use `bg-foreground text-background` (they invert correctly); never hardcode
  `bg-white`/`bg-black`/hex.

### 9. Real-time updates (WebSocket / STOMP)
Live auction updates use **STOMP over WebSocket** via `@stomp/stompjs`. Commands
(placing a bid) still go through the **generated REST hooks**; the socket is
broadcast-only (server → client).
- `src/realtime/useListingChannel.ts` subscribes to `/topic/listings/{id}` and folds
  each event into the TanStack Query cache (`setQueryData` on the listing,
  `invalidateQueries` on its bids) — UI reads stay in TanStack Query, no socket state
  in components. Mount it once per listing detail view.
- The dev server proxies `/ws` to the backend (`vite.config.ts`, `ws: true`).
- **One sanctioned hand-written API type:** `src/realtime/types.ts` (`BidEvent`).
  WebSocket payloads are **not** part of the OpenAPI contract, so Orval cannot
  generate them. This is the *only* place a request/response-ish type is hand-written;
  it mirrors the backend `com.bidwise.realtime.BidEvent` and must be kept in sync.
  Everything REST still comes from `src/api/generated/` (see §4) — do not hand-roll
  other API types.

## Commands
```bash
pnpm run gen:api   # regenerate API client from the backend OpenAPI spec
pnpm run lint      # ESLint (generated dir excluded)
pnpm test          # Vitest
pnpm run build     # type-check + production build
```
