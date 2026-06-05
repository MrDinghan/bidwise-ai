import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import BidPanel from './BidPanel';
import type { ListingResponse } from '@/api/generated/model';

// Mock the generated bid hook; the panel is the unit under test.
const placeMutate = vi.fn();
vi.mock('@/api/generated/bids/bids', () => ({
  usePlaceBid: () => ({ mutate: placeMutate, isPending: false }),
  getGetBidsQueryKey: () => ['bids'],
}));
vi.mock('@/api/generated/listings/listings', () => ({
  getGetListingQueryKey: () => ['listing'],
}));

const listing = {
  id: 1,
  currentPrice: 10,
  bidIncrement: 1,
  bidCount: 1,
} as ListingResponse;

const renderPanel = () =>
  render(
    <QueryClientProvider client={new QueryClient()}>
      <BidPanel listing={listing} isTopBidder={false} />
    </QueryClientProvider>,
  );

describe('BidPanel', () => {
  beforeEach(() => placeMutate.mockReset());

  it('shows the minimum next bid and places a bid through the generated mutation', async () => {
    const user = userEvent.setup();
    renderPanel();

    // currentPrice 10 + increment 1 = 11 (bids already exist).
    expect(screen.getByText(/minimum bid/i)).toBeInTheDocument();
    expect(screen.getByLabelText('Bid amount')).toHaveValue(11);

    await user.click(screen.getByRole('button', { name: /place bid/i }));

    expect(placeMutate).toHaveBeenCalledTimes(1);
    const [payload] = placeMutate.mock.calls[0];
    expect(payload.listingId).toBe(1);
    expect(payload.data.amount).toBe(11);
  });
});
