import type { FC, ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuthStore } from './store';

interface RequireAuthProps {
  children: ReactNode;
}

/** Renders children only when authenticated; otherwise redirects to /login. */
const RequireAuth: FC<RequireAuthProps> = ({ children }) => {
  const isAuthenticated = useAuthStore((s) => Boolean(s.token));
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />;
};

export default RequireAuth;
