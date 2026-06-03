import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface AuthState {
  token: string | null;
  login: (token: string) => void;
  logout: () => void;
}

/**
 * Auth state, persisted to localStorage by zustand's persist middleware.
 * Use selectors in components, e.g. `useAuthStore((s) => s.token)`.
 * Outside React (e.g. axios interceptors) use `useAuthStore.getState()`.
 */
export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      login: (token) => set({ token }),
      logout: () => set({ token: null }),
    }),
    { name: 'bidwise-auth' },
  ),
);
