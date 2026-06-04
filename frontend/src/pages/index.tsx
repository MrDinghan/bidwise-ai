import type { CSSProperties, FC } from 'react';
import { useSearchParams } from 'react-router-dom';
import { ArrowLeft, ArrowRight } from 'lucide-react';
import { useSearchListings } from '@/api/generated/listings/listings';
import type { SearchListingsParams } from '@/api/generated/model';
import ListingCard from '@/components/ListingCard';
import SearchFilters from '@/components/SearchFilters';
import { Button } from '@/components/ui/button';

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
  const totalPages = data?.totalPages ?? 0;
  const totalElements = data?.totalElements ?? 0;

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

  return (
    <div className="relative z-10 mx-auto max-w-6xl px-5 py-10">
      {/* Catalogue cover */}
      <section className="mb-10 border-b-2 border-foreground/80 pb-8">
        <div className="flex items-center justify-between">
          <span className="label-mono">The Student Auction Catalogue</span>
          <span className="label-mono hidden sm:block">Edition · MMXXVI</span>
        </div>
        <h1 className="mt-4 max-w-3xl font-display text-4xl font-medium leading-[1] tracking-tight sm:text-6xl lg:text-7xl">
          Browse <em className="font-normal italic text-primary">listings</em> from
          fellow students.
        </h1>
        <p className="mt-5 max-w-xl text-base text-muted-foreground">
          Furniture, electronics, books and more — each one a lot on the block, going to the
          highest bidder before the term ends.
        </p>
      </section>

      <div className="mb-6">
        <SearchFilters
          q={q}
          category={category}
          condition={condition}
          minPrice={minPrice}
          maxPrice={maxPrice}
          sort={sort}
          onChange={onChange}
        />
      </div>

      <div className="mb-5 flex items-center justify-between border-b border-foreground/15 pb-2">
        <span className="label-mono">
          {isLoading ? 'Loading lots…' : `${totalElements} ${totalElements === 1 ? 'lot' : 'lots'} on the block`}
        </span>
        <span className="label-mono hidden sm:block">Sorted · {sort}</span>
      </div>

      {isError && (
        <p role="alert" className="py-8 text-center font-mono text-sm text-destructive">
          The catalogue could not be loaded.
        </p>
      )}

      {isLoading ? (
        <div className="grid grid-cols-2 gap-px bg-foreground/10 sm:grid-cols-3 lg:grid-cols-4">
          {Array.from({ length: 8 }).map((_, i) => (
            <div key={i} className="aspect-[3/4] animate-pulse bg-muted" />
          ))}
        </div>
      ) : listings.length === 0 ? (
        <div className="border border-dashed border-foreground/30 py-20 text-center">
          <p className="font-display text-2xl">No lots match your search.</p>
          <p className="label-mono mt-2">Try widening the filters</p>
        </div>
      ) : (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 sm:gap-5 lg:grid-cols-4">
          {listings.map((listing, i) => (
            <div
              key={listing.id}
              className="animate-lot-rise"
              style={{ animationDelay: `${Math.min(i, 11) * 55}ms` } as CSSProperties}
            >
              <ListingCard listing={listing} />
            </div>
          ))}
        </div>
      )}

      {totalPages > 1 && (
        <nav
          className="mt-12 flex items-center justify-center gap-6 border-t border-foreground/15 pt-6"
          aria-label="Pagination"
        >
          <Button
            variant="ghost"
            size="sm"
            className="font-mono text-xs uppercase tracking-widest"
            disabled={page <= 0}
            onClick={() => onChange('page', String(page - 1))}
          >
            <ArrowLeft className="size-4" /> Prev
          </Button>
          <span className="label-mono">
            {String(page + 1).padStart(2, '0')} / {String(totalPages).padStart(2, '0')}
          </span>
          <Button
            variant="ghost"
            size="sm"
            className="font-mono text-xs uppercase tracking-widest"
            disabled={page >= totalPages - 1}
            onClick={() => onChange('page', String(page + 1))}
          >
            Next <ArrowRight className="size-4" />
          </Button>
        </nav>
      )}
    </div>
  );
};

export default MarketplacePage;
