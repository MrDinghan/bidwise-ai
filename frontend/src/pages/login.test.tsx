import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import LoginPage from './login';
import { useAuthStore } from '@/auth/store';

// Mock the generated hook so the smoke test never hits a real network.
const mutate = vi.fn();
vi.mock('../api/generated/auth/auth', () => ({
  useLogin: () => ({ mutate, isPending: false }),
}));

const renderLogin = () =>
  render(
    <MemoryRouter>
      <LoginPage />
    </MemoryRouter>,
  );

describe('Login page', () => {
  beforeEach(() => {
    mutate.mockReset();
    useAuthStore.setState({ token: null });
  });

  it('renders the login form', () => {
    renderLogin();
    expect(screen.getByRole('heading', { name: /log in/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
  });

  it('submits credentials through the generated mutation', async () => {
    const user = userEvent.setup();
    renderLogin();

    await user.type(screen.getByLabelText(/email/i), 'alice@example.com');
    await user.type(screen.getByLabelText(/password/i), 'password123');
    await user.click(screen.getByRole('button', { name: /log in/i }));

    expect(mutate).toHaveBeenCalledTimes(1);
    expect(mutate.mock.calls[0][0]).toEqual({
      data: { email: 'alice@example.com', password: 'password123' },
    });
  });
});
