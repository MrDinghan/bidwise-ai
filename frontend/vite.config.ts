/// <reference types="vitest/config" />
import { fileURLToPath, URL } from 'node:url';
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import Pages from 'vite-plugin-pages';

export default defineConfig({
  // File-based routing: routes are derived from src/pages/** (see ~react-pages).
  plugins: [
    react(),
    tailwindcss(),
    Pages({ resolver: 'react', dirs: 'src/pages', exclude: ['**/*.test.tsx'] }),
  ],
  resolve: {
    // `@/x` resolves to `src/x` — avoids deep ../../../ relative imports.
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    // Dev proxy so the frontend can call the backend at the same origin.
    proxy: {
      '/api': 'http://localhost:8080',
      '/v3': 'http://localhost:8080',
      // STOMP-over-WebSocket endpoint (ws: true upgrades the connection).
      '/ws': { target: 'http://localhost:8080', ws: true },
    },
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    coverage: {
      provider: 'v8',
      exclude: [
        'src/api/generated/**', // generated API client — not hand-written code
        '**/*.config.*',
        'src/main.tsx',
        'dist/**', // build artifacts, not source
        'coverage/**', // the coverage report itself
      ],
    },
  },
});
