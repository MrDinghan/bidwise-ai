import { useState, type FC, type FormEvent } from 'react';
import { toast } from 'sonner';
import { Gavel, Loader2 } from 'lucide-react';
import { useQueryClient } from '@tanstack/react-query';
import { getGetBidsQueryKey, usePlaceBid } from '@/api/generated/bids/bids';
import { getGetListingQueryKey } from '@/api/generated/listings/listings';
import type { ListingResponse } from '@/api/generated/model';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { formatPrice } from '@/lib/format';

interface BidPanelProps {
  listing: ListingResponse;
  isTopBidder: boolean;
}

/** Bid input for an active auction. The amount is prefilled to the minimum next bid. */
const BidPanel: FC<BidPanelProps> = ({ listing, isTopBidder }) => {
  const queryClient = useQueryClient();
  const listingId = listing.id as number;
  const currentPrice = listing.currentPrice ?? 0;
  const increment = listing.bidIncrement ?? 0;
  const hasBids = (listing.bidCount ?? 0) > 0;
  // The opening bid clears at the start price; later bids must add one increment.
  const minBid = hasBids ? currentPrice + increment : currentPrice;

  const [amount, setAmount] = useState<string>(minBid.toFixed(2));
  const mutation = usePlaceBid();

  const onSubmit = (e: FormEvent): void => {
    e.preventDefault();
    mutation.mutate(
      { listingId, data: { amount: Number(amount) } },
      {
        onSuccess: () => {
          toast.success('Bid placed — you lead the lot.');
          void queryClient.invalidateQueries({ queryKey: getGetListingQueryKey(listingId) });
          void queryClient.invalidateQueries({ queryKey: getGetBidsQueryKey(listingId) });
        },
        onError: (error) => {
          const message = (error as { response?: { data?: { message?: string } } }).response?.data
            ?.message;
          toast.error(message ?? 'Could not place your bid.');
        },
      },
    );
  };

  return (
    <form onSubmit={onSubmit} className="mt-6 border-t border-foreground/15 pt-5">
      <div className="flex items-center justify-between">
        <span className="label-mono">Place a bid</span>
        {isTopBidder && (
          <span className="font-mono text-xs uppercase tracking-widest text-primary">
            You lead
          </span>
        )}
      </div>
      <p className="label-mono mt-1 text-muted-foreground">
        Minimum bid {formatPrice(minBid)}
      </p>
      <div className="mt-3 flex flex-col gap-2 sm:flex-row">
        <Input
          type="number"
          inputMode="decimal"
          min={minBid}
          step={increment || 0.01}
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          aria-label="Bid amount"
          required
          className="rounded-sm border-foreground/20 font-mono sm:flex-1"
        />
        <Button
          type="submit"
          disabled={mutation.isPending}
          className="rounded-sm font-mono text-xs uppercase tracking-widest"
        >
          {mutation.isPending ? <Loader2 className="size-4 animate-spin" /> : <Gavel className="size-3.5" />}
          {mutation.isPending ? 'Placing…' : 'Place bid'}
        </Button>
      </div>
    </form>
  );
};

export default BidPanel;
