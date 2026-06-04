import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export type Theme = 'light' | 'dark';

interface ThemeState {
  theme: Theme;
  setTheme: (theme: Theme) => void;
  toggleTheme: () => void;
}

/** OS preference, used as the default until the user makes an explicit choice. */
const systemTheme = (): Theme =>
  typeof window !== 'undefined' &&
  window.matchMedia?.('(prefers-color-scheme: dark)').matches
    ? 'dark'
    : 'light';

/**
 * Theme state, persisted to localStorage by zustand's persist middleware.
 * First visit (nothing stored) falls back to the OS preference; an inline script
 * in index.html applies the matching class before paint to avoid a flash.
 * Use selectors in components, e.g. `useThemeStore((s) => s.theme)`.
 */
export const useThemeStore = create<ThemeState>()(
  persist(
    (set, get) => ({
      theme: systemTheme(),
      setTheme: (theme) => set({ theme }),
      toggleTheme: () => set({ theme: get().theme === 'dark' ? 'light' : 'dark' }),
    }),
    { name: 'bidwise-theme' },
  ),
);
