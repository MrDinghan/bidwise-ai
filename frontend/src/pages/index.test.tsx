import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import MarketplacePage from './index';

// Mock the generated search hook so the smoke test never hits the network.
vi.mock('../api/generated/listings/listings', () => ({
  useSearchListings: () => ({
    data: {
      content: [
        {
          id: 1,
          title: 'Vintage camera',
          category: 'ELECTRONICS',
          condition: 'LIKE_NEW',
          currentPrice: 50,
          photos: [],
        },
      ],
      page: 0,
      size: 12,
      totalElements: 1,
      totalPages: 1,
    },
    isLoading: false,
    isError: false,
  }),
}));

describe('Marketplace page', () => {
  it('renders listing cards from the search results', () => {
    render(
      <MemoryRouter>
        <MarketplacePage />
      </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { name: /browse listings/i })).toBeInTheDocument();
    expect(screen.getByText('Vintage camera')).toBeInTheDocument();
  });
});
