import type { FC } from 'react';
import { useMe } from '@/api/generated/auth/auth';
import { useAuthStore } from '@/auth/store';
import RequireAuth from '@/auth/RequireAuth';

const ProfileContent: FC = () => {
  const logout = useAuthStore((s) => s.logout);
  const { data: user, isLoading, isError } = useMe();

  if (isLoading) {
    return <p>Loading…</p>;
  }
  if (isError || !user) {
    return <p role="alert">Could not load your profile.</p>;
  }

  return (
    <main>
      <h1>Welcome, {user.name}</h1>
      <p>Email: {user.email}</p>
      <p>Role: {user.role}</p>
      <button type="button" onClick={logout}>
        Log out
      </button>
    </main>
  );
};

const ProfilePage: FC = () => (
  <RequireAuth>
    <ProfileContent />
  </RequireAuth>
);

export default ProfilePage;
