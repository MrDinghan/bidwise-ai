import { defineConfig } from 'orval';

/**
 * Generates the API client (TanStack Query hooks + TS types) from the backend's
 * OpenAPI spec. The spec is the single source of truth — never hand-edit
 * src/api/generated/.
 *
 * Spec source resolution order:
 *   1. OPENAPI_SPEC env var (CI points this at backend/target/openapi.json)
 *   2. the live running backend at http://localhost:8080/v3/api-docs (dev default)
 */
const SPEC =
  process.env.OPENAPI_SPEC ?? 'http://localhost:8080/v3/api-docs';

export default defineConfig({
  bidwise: {
    input: SPEC,
    output: {
      mode: 'tags-split',
      target: 'src/api/generated',
      schemas: 'src/api/generated/model',
      client: 'react-query',
      clean: true,
      prettier: false,
      override: {
        mutator: {
          path: 'src/api/mutator.ts',
          name: 'customInstance',
        },
      },
    },
  },
});
