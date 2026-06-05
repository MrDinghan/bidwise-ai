import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import BidHistory from './BidHistory';

// Mock the generated bid-history query.
vi.mock('@/api/generated/bids/bids', () => ({
  useGetBids: () => ({
    data: {
      content: [
        { id: 2, bidderName: 'Bob', amount: 11, createdAt: new Date().toISOString() },
        { id: 1, bidderName: 'Alice', amount: 10, createdAt: new Date().toISOString() },
      ],
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
    },
    isLoading: false,
  }),
}));

describe('BidHistory', () => {
  it('lists bids newest first with bidder and amount', () => {
    render(<BidHistory listingId={1} />);

    expect(screen.getByText('Bid history')).toBeInTheDocument();
    expect(screen.getByText('Bob')).toBeInTheDocument();
    expect(screen.getByText('Alice')).toBeInTheDocument();
    expect(screen.getByText('Leading')).toBeInTheDocument();
  });
});
