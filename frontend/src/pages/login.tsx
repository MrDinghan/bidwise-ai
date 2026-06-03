import { useState, type FC, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useLogin } from '@/api/generated/auth/auth';
import { useAuthStore } from '@/auth/store';
import RequireGuest from '@/auth/RequireGuest';

const LoginForm: FC = () => {
  const navigate = useNavigate();
  const login = useAuthStore((s) => s.login);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);

  const mutation = useLogin();

  const onSubmit = (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    mutation.mutate(
      { data: { email, password } },
      {
        onSuccess: (res) => {
          if (res.token) {
            login(res.token);
            navigate('/');
          }
        },
        onError: () => setError('Invalid email or password'),
      },
    );
  };

  return (
    <main>
      <h1>Log in</h1>
      <form onSubmit={onSubmit}>
        <label>
          Email
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </label>
        <label>
          Password
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </label>
        <button type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? 'Logging in…' : 'Log in'}
        </button>
      </form>
      {error && <p role="alert">{error}</p>}
      <p>
        No account? <Link to="/register">Register</Link>
      </p>
    </main>
  );
};

const LoginPage: FC = () => (
  <RequireGuest>
    <LoginForm />
  </RequireGuest>
);

export default LoginPage;
