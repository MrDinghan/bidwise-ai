import type { FC } from 'react';
import { useGetBids } from '@/api/generated/bids/bids';
import { formatPrice, formatTimeAgo } from '@/lib/format';

interface BidHistoryProps {
  listingId: number;
}

/** Public bid history for a listing, newest (highest) first. */
const BidHistory: FC<BidHistoryProps> = ({ listingId }) => {
  const { data, isLoading } = useGetBids(listingId, undefined, {
    query: { enabled: Number.isFinite(listingId) },
  });
  const bids = data?.content ?? [];

  return (
    <section className="mt-8 border-t border-foreground/15 pt-5">
      <h2 className="label-mono">Bid history</h2>

      {isLoading && <div className="mt-3 h-20 w-full animate-pulse bg-muted" />}

      {!isLoading && bids.length === 0 && (
        <p className="mt-3 font-mono text-xs text-muted-foreground">
          No bids yet — be the first to open this lot.
        </p>
      )}

      {bids.length > 0 && (
        <ol className="mt-3 divide-y divide-foreground/10">
          {bids.map((bid, index) => (
            <li
              key={bid.id}
              className="flex items-center justify-between gap-3 py-2 text-sm"
            >
              <span className="min-w-0 truncate">
                {index === 0 && (
                  <span className="mr-2 font-mono text-[0.6rem] uppercase tracking-widest text-primary">
                    Leading
                  </span>
                )}
                {bid.bidderName}
              </span>
              <span className="flex items-baseline gap-3">
                <span className="font-mono text-xs text-muted-foreground">
                  {formatTimeAgo(bid.createdAt)}
                </span>
                <span className="tabular font-mono font-semibold">{formatPrice(bid.amount)}</span>
              </span>
            </li>
          ))}
        </ol>
      )}
    </section>
  );
};

export default BidHistory;
