import type { FC } from 'react';
import { Link } from 'react-router-dom';
import { useAuthStore } from '@/auth/store';

const AppHeader: FC = () => {
  const isAuthenticated = useAuthStore((s) => Boolean(s.token));
  const logout = useAuthStore((s) => s.logout);

  return (
    <header className="app-header">
      <Link to="/" className="app-header__brand">
        BidWise
      </Link>
      <nav className="app-header__nav">
        <Link to="/">Browse</Link>
        {isAuthenticated ? (
          <>
            <Link to="/listings/new">Sell</Link>
            <Link to="/my-listings">My listings</Link>
            <Link to="/profile">Profile</Link>
            <button type="button" onClick={logout}>
              Log out
            </button>
          </>
        ) : (
          <>
            <Link to="/login">Log in</Link>
            <Link to="/register">Register</Link>
          </>
        )}
      </nav>
    </header>
  );
};

export default AppHeader;
