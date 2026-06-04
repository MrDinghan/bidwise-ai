import type { FC } from 'react';
import { useNavigate } from 'react-router-dom';
import { LogOut } from 'lucide-react';
import { useMe } from '@/api/generated/auth/auth';
import { useAuthStore } from '@/auth/store';
import RequireAuth from '@/auth/RequireAuth';
import { Button } from '@/components/ui/button';

const ProfileContent: FC = () => {
  const navigate = useNavigate();
  const logout = useAuthStore((s) => s.logout);
  const { data: user, isLoading, isError } = useMe();

  const onLogout = (): void => {
    logout();
    navigate('/');
  };

  if (isLoading) {
    return (
      <div className="relative z-10 mx-auto max-w-md px-5 py-16">
        <div className="h-48 w-full animate-pulse bg-muted" />
      </div>
    );
  }
  if (isError || !user) {
    return (
      <div className="relative z-10 mx-auto max-w-md px-5 py-16">
        <p role="alert" className="font-mono text-sm text-destructive">
          Could not load your profile.
        </p>
      </div>
    );
  }

  return (
    <div className="relative z-10 mx-auto max-w-md px-5 py-16">
      <div className="border border-foreground/15 bg-card">
        <div className="border-b-2 border-foreground/80 px-6 py-5">
          <span className="label-mono">Member record</span>
        </div>
        <div className="space-y-6 p-6">
          <div className="flex items-center gap-4">
            <span className="flex size-14 items-center justify-center rounded-sm bg-foreground font-display text-2xl text-background">
              {(user.name ?? '?').charAt(0).toUpperCase()}
            </span>
            <div className="min-w-0">
              <h1 className="truncate font-display text-2xl font-medium tracking-tight">
                {user.name}
              </h1>
              <p className="truncate font-mono text-xs text-muted-foreground">{user.email}</p>
            </div>
          </div>

          <dl className="grid grid-cols-2 gap-px overflow-hidden border border-foreground/15 bg-foreground/15">
            <div className="bg-card p-3">
              <dt className="label-mono text-[0.6rem]">Role</dt>
              <dd className="mt-1 font-mono text-sm font-semibold">{user.role}</dd>
            </div>
            <div className="bg-card p-3">
              <dt className="label-mono text-[0.6rem]">Status</dt>
              <dd className="mt-1 font-mono text-sm font-semibold text-primary">Active</dd>
            </div>
          </dl>

          <Button
            variant="outline"
            onClick={onLogout}
            className="w-full rounded-sm border-foreground/30 font-mono text-xs uppercase tracking-widest"
          >
            <LogOut className="size-4" /> Log out
          </Button>
        </div>
      </div>
    </div>
  );
};

const ProfilePage: FC = () => (
  <RequireAuth>
    <ProfileContent />
  </RequireAuth>
);

export default ProfilePage;
