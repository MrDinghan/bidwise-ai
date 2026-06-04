import { Suspense, useEffect, type FC } from "react";
import { useRoutes } from "react-router-dom";
import routes from "~react-pages";
import AppHeader from "@/components/AppHeader";
import { Toaster } from "@/components/ui/sonner";
import { useThemeStore } from "@/theme/store";

// Routes are generated from src/pages/** by vite-plugin-pages — no manual route
// tree. Per-route auth is declared via RequireAuth/RequireGuest inside each page.
const App: FC = () => {
  // Keep the <html> class in sync with the theme store (initial class is set by an
  // inline script in index.html to avoid a flash before React mounts).
  const theme = useThemeStore((s) => s.theme);
  useEffect(() => {
    document.documentElement.classList.toggle("dark", theme === "dark");
  }, [theme]);

  return (
    <div className="flex min-h-screen flex-col">
      <AppHeader />
      <main className="relative z-10 flex-1">
        <Suspense
          fallback={
            <p className="mx-auto max-w-6xl px-5 py-16 font-mono text-sm text-muted-foreground">
              Loading…
            </p>
          }
        >
          {useRoutes(routes)}
        </Suspense>
      </main>
      <footer className="relative z-10 border-t border-foreground/15">
        <div className="mx-auto flex max-w-6xl flex-col items-center justify-between gap-2 px-5 py-6 sm:flex-row">
          <span className="label-mono">
            BidWise — Student Auction Catalogue
          </span>
          <span className="label-mono">Going once · Going twice · Sold</span>
        </div>
      </footer>
      <Toaster
        richColors
        position="top-center"
        toastOptions={{ className: "font-mono text-sm" }}
      />
    </div>
  );
};

export default App;
