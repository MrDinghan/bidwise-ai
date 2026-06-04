import type { FC } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useSearchListings } from '@/api/generated/listings/listings';
import type { SearchListingsParams } from '@/api/generated/model';
import ListingCard from '@/components/ListingCard';
import SearchFilters from '@/components/SearchFilters';

const PAGE_SIZE = 12;

const MarketplacePage: FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();

  const q = searchParams.get('q') ?? '';
  const category = searchParams.get('category') ?? '';
  const condition = searchParams.get('condition') ?? '';
  const minPrice = searchParams.get('minPrice') ?? '';
  const maxPrice = searchParams.get('maxPrice') ?? '';
  const sort = searchParams.get('sort') ?? 'NEWEST';
  const page = Number(searchParams.get('page') ?? '0');

  const params: SearchListingsParams = {
    q: q || undefined,
    category: (category || undefined) as SearchListingsParams['category'],
    condition: (condition || undefined) as SearchListingsParams['condition'],
    minPrice: minPrice ? Number(minPrice) : undefined,
    maxPrice: maxPrice ? Number(maxPrice) : undefined,
    sort: (sort || undefined) as SearchListingsParams['sort'],
    page,
    size: PAGE_SIZE,
  };

  const { data, isLoading, isError } = useSearchListings(params);
  const listings = data?.content ?? [];

  // Changing a filter resets paging; changing the page keeps the filters.
  const onChange = (key: string, value: string): void => {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      if (value) {
        next.set(key, value);
      } else {
        next.delete(key);
      }
      if (key !== 'page') {
        next.delete('page');
      }
      return next;
    });
  };

  const totalPages = data?.totalPages ?? 0;

  return (
    <main>
      <h1>Browse listings</h1>
      <SearchFilters
        q={q}
        category={category}
        condition={condition}
        minPrice={minPrice}
        maxPrice={maxPrice}
        sort={sort}
        onChange={onChange}
      />

      {isLoading && <p>Loading…</p>}
      {isError && <p role="alert">Could not load listings.</p>}

      {data && listings.length === 0 && <p>No listings match your search.</p>}

      <section className="listing-grid">
        {listings.map((listing) => (
          <ListingCard key={listing.id} listing={listing} />
        ))}
      </section>

      {totalPages > 1 && (
        <nav className="pagination" aria-label="Pagination">
          <button
            type="button"
            disabled={page <= 0}
            onClick={() => onChange('page', String(page - 1))}
          >
            Previous
          </button>
          <span>
            Page {page + 1} of {totalPages}
          </span>
          <button
            type="button"
            disabled={page >= totalPages - 1}
            onClick={() => onChange('page', String(page + 1))}
          >
            Next
          </button>
        </nav>
      )}
    </main>
  );
};

export default MarketplacePage;
