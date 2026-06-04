import type { FC } from 'react';
import { Link } from 'react-router-dom';
import { Plus } from 'lucide-react';
import { useGetMyListings } from '@/api/generated/listings/listings';
import RequireAuth from '@/auth/RequireAuth';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { formatPrice } from '@/lib/format';
import { statusBadgeVariant } from '@/lib/listing';

const MyListingsContent: FC = () => {
  const { data, isLoading, isError } = useGetMyListings({ size: 50 });
  const listings = data?.content ?? [];

  return (
    <div className="relative z-10 mx-auto max-w-4xl px-5 py-10">
      <div className="mb-6 flex items-end justify-between border-b-2 border-foreground/80 pb-4">
        <div>
          <span className="label-mono">Consignor ledger</span>
          <h1 className="mt-1 font-display text-3xl font-medium tracking-tight">My lots</h1>
        </div>
        <Button asChild size="sm" className="rounded-sm font-mono text-xs uppercase tracking-widest">
          <Link to="/listings/new">
            <Plus className="size-3.5" /> New lot
          </Link>
        </Button>
      </div>

      {isLoading && <div className="h-64 w-full animate-pulse bg-muted" />}
      {isError && (
        <p role="alert" className="font-mono text-sm text-destructive">
          Could not load your lots.
        </p>
      )}

      {data && listings.length === 0 && (
        <div className="border border-dashed border-foreground/30 py-16 text-center">
          <p className="font-display text-xl">Nothing consigned yet.</p>
          <p className="label-mono mt-2">List your first lot to get started</p>
        </div>
      )}

      {listings.length > 0 && (
        <div className="border border-foreground/15 bg-card">
          <div className="grid grid-cols-[auto_1fr_auto_auto] items-center gap-3 border-b sm:gap-4 border-foreground/20 px-4 py-2.5">
            <span className="label-mono text-[0.6rem]">Lot</span>
            <span className="label-mono text-[0.6rem]">Title</span>
            <span className="label-mono text-[0.6rem]">Price</span>
            <span className="label-mono text-[0.6rem] text-right">·</span>
          </div>
          {listings.map((listing) => (
            <div
              key={listing.id}
              className="grid grid-cols-[auto_1fr_auto_auto] items-center gap-3 border-b sm:gap-4 border-foreground/10 px-4 py-3 last:border-0 hover:bg-muted/40"
            >
              <span className="font-mono text-xs font-semibold tracking-wider text-muted-foreground">
                {String(listing.id ?? 0).padStart(3, '0')}
              </span>
              <div className="min-w-0">
                <Link
                  to={`/listings/${listing.id}`}
                  className="block truncate font-display text-base decoration-primary decoration-2 underline-offset-2 hover:underline"
                >
                  {listing.title}
                </Link>
                <Badge
                  variant={statusBadgeVariant(listing.status)}
                  className="mt-1 rounded-sm font-mono text-[0.6rem] uppercase tracking-widest"
                >
                  {listing.status}
                </Badge>
              </div>
              <span className="tabular font-mono text-sm font-semibold">
                {formatPrice(listing.currentPrice)}
              </span>
              <Button
                asChild
                variant="ghost"
                size="sm"
                className="font-mono text-xs uppercase tracking-widest"
              >
                <Link to={`/listings/${listing.id}/edit`}>Edit</Link>
              </Button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

const MyListingsPage: FC = () => (
  <RequireAuth>
    <MyListingsContent />
  </RequireAuth>
);

export default MyListingsPage;
