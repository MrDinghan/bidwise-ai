import { Suspense, type FC } from 'react';
import { useRoutes } from 'react-router-dom';
import routes from '~react-pages';
import AppHeader from '@/components/AppHeader';

// Routes are generated from src/pages/** by vite-plugin-pages — no manual route
// tree. Per-route auth is declared via RequireAuth/RequireGuest inside each page.
const App: FC = () => (
  <>
    <AppHeader />
    <Suspense fallback={<p>Loading…</p>}>{useRoutes(routes)}</Suspense>
  </>
);

export default App;
