import { useState, type FC, type FormEvent } from 'react';
import { toast } from 'sonner';
import { Gavel, Loader2, ShieldCheck } from 'lucide-react';
import { useQueryClient } from '@tanstack/react-query';
import { getGetBidsQueryKey, usePlaceBid } from '@/api/generated/bids/bids';
import {
  getGetDepositQueryKey,
  useGetDeposit,
  usePlaceDeposit,
} from '@/api/generated/deposits/deposits';
import { getGetListingQueryKey } from '@/api/generated/listings/listings';
import { PaymentHoldResponseStatus, type ListingResponse } from '@/api/generated/model';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { formatPrice } from '@/lib/format';

interface BidPanelProps {
  listing: ListingResponse;
  isTopBidder: boolean;
}

/**
 * Bid input for an active auction. Bidding requires an authorized deposit hold first
 * (the pre-auth gate), so until the buyer has one we show a "place deposit" step.
 */
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

  // A 404 means "no deposit yet" — don't retry it, just treat the hold as absent.
  const deposit = useGetDeposit(listingId, { query: { retry: false } });
  const depositMutation = usePlaceDeposit();
  const hasAuthorizedDeposit =
    deposit.data?.status === PaymentHoldResponseStatus.AUTHORIZED;

  const onPlaceDeposit = (): void => {
    depositMutation.mutate(
      { listingId },
      {
        onSuccess: () => {
          toast.success('Deposit authorized — you can bid now.');
          void queryClient.invalidateQueries({ queryKey: getGetDepositQueryKey(listingId) });
        },
        onError: () => {
          toast.error('Could not authorize your deposit. Please try again.');
        },
      },
    );
  };

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

  if (deposit.isLoading) {
    return (
      <div className="mt-6 flex items-center gap-2 border-t border-foreground/15 pt-5">
        <Loader2 className="size-4 animate-spin" />
        <span className="label-mono text-muted-foreground">Checking your deposit…</span>
      </div>
    );
  }

  if (!hasAuthorizedDeposit) {
    return (
      <div className="mt-6 border-t border-foreground/15 pt-5">
        <span className="label-mono">Deposit required</span>
        <p className="label-mono mt-1 text-muted-foreground">
          A refundable deposit is held to bid. It is released automatically if you don’t win.
        </p>
        <Button
          type="button"
          onClick={onPlaceDeposit}
          disabled={depositMutation.isPending}
          className="mt-3 rounded-sm font-mono text-xs uppercase tracking-widest"
        >
          {depositMutation.isPending ? (
            <Loader2 className="size-4 animate-spin" />
          ) : (
            <ShieldCheck className="size-3.5" />
          )}
          {depositMutation.isPending ? 'Authorizing…' : 'Place deposit to bid'}
        </Button>
      </div>
    );
  }

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
