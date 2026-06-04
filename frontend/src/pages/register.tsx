import { useState, type FC, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useRegister } from '@/api/generated/auth/auth';
import { useAuthStore } from '@/auth/store';
import RequireGuest from '@/auth/RequireGuest';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

const labelClass = 'label-mono text-foreground';
const fieldClass = 'rounded-sm border-foreground/20';

const RegisterForm: FC = () => {
  const navigate = useNavigate();
  const login = useAuthStore((s) => s.login);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);

  const mutation = useRegister();

  const onSubmit = (e: FormEvent): void => {
    e.preventDefault();
    setError(null);
    mutation.mutate(
      { data: { name, email, password } },
      {
        onSuccess: (res) => {
          if (res.token) {
            login(res.token);
            navigate('/');
          }
        },
        onError: () => setError('Could not register (email may already be in use)'),
      },
    );
  };

  return (
    <div className="relative z-10 mx-auto max-w-md px-5 py-16">
      <div className="border border-foreground/15 bg-card">
        <div className="border-b-2 border-foreground/80 px-6 py-5">
          <span className="label-mono">Claim your paddle</span>
          <h1 className="mt-1 font-display text-3xl font-medium tracking-tight">Create account</h1>
        </div>
        <form onSubmit={onSubmit} className="space-y-5 p-6">
          <div className="space-y-2">
            <Label htmlFor="name" className={labelClass}>
              Name
            </Label>
            <Input
              id="name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              className={fieldClass}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="email" className={labelClass}>
              Email
            </Label>
            <Input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              className={fieldClass}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="password" className={labelClass}>
              Password
            </Label>
            <Input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              minLength={8}
              required
              className={fieldClass}
            />
          </div>
          <Button
            type="submit"
            disabled={mutation.isPending}
            className="w-full rounded-sm font-mono text-xs uppercase tracking-widest"
          >
            {mutation.isPending ? 'Creating…' : 'Register'}
          </Button>
          {error && (
            <p role="alert" className="font-mono text-xs text-destructive">
              {error}
            </p>
          )}
          <p className="border-t border-dashed border-foreground/20 pt-4 text-center font-mono text-xs text-muted-foreground">
            Already have a paddle?{' '}
            <Link to="/login" className="text-primary underline-offset-2 hover:underline">
              Log in
            </Link>
          </p>
        </form>
      </div>
    </div>
  );
};

const RegisterPage: FC = () => (
  <RequireGuest>
    <RegisterForm />
  </RequireGuest>
);

export default RegisterPage;
