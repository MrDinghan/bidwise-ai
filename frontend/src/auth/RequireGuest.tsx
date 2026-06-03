import type { FC, ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuthStore } from './store';

interface RequireGuestProps {
  children: ReactNode;
}

/** Renders children only when NOT authenticated; otherwise redirects home. */
const RequireGuest: FC<RequireGuestProps> = ({ children }) => {
  const isAuthenticated = useAuthStore((s) => Boolean(s.token));
  return isAuthenticated ? <Navigate to="/" replace /> : <>{children}</>;
};

export default RequireGuest;
