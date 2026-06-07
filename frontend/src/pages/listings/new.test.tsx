import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import NewListingPage from './new';
import { useAuthStore } from '@/auth/store';

// Mock the generated listing hooks; the form is the unit under test.
const createMutate = vi.fn();
vi.mock('../../api/generated/listings/listings', () => ({
  useCreateListing: () => ({ mutate: createMutate, isPending: false }),
  useUpdateListing: () => ({ mutate: vi.fn(), isPending: false }),
  useGetListing: () => ({ data: undefined }),
}));

describe('New listing form', () => {
  beforeEach(() => {
    createMutate.mockReset();
    useAuthStore.setState({ token: 'test-token' }); // satisfy RequireAuth
  });

  it('submits a draft through the generated create mutation', async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter>
        <NewListingPage />
      </MemoryRouter>,
    );

    await user.type(screen.getByLabelText('Title'), 'Desk lamp');
    await user.type(screen.getByLabelText('Description'), 'A nice lamp');
    await user.type(screen.getByLabelText('Start price'), '10');
    await user.type(screen.getByLabelText('Bid increment'), '1');
    await user.click(screen.getByRole('button', { name: /save draft/i }));

    expect(createMutate).toHaveBeenCalledTimes(1);
    const [payload] = createMutate.mock.calls[0];
    expect(payload.data.title).toBe('Desk lamp');
    expect(payload.data.startPrice).toBe(10);
    expect(payload.data.bidIncrement).toBe(1);
    // Duration defaults to the 3-day option; endAt is no longer sent.
    expect(payload.data.duration).toBe('THREE_DAYS');
    expect(payload.data.endAt).toBeUndefined();
  });
});
