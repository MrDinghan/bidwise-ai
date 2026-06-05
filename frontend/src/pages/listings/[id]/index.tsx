import type { FC } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { toast } from 'sonner';
import { ArrowLeft, Clock, MapPin, Pencil, Trash2, Upload } from 'lucide-react';
import { useMe } from '@/api/generated/auth/auth';
import {
  useDeleteListing,
  useGetListing,
  usePublishListing,
} from '@/api/generated/listings/listings';
import { useAuthStore } from '@/auth/store';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import BidPanel from '@/components/BidPanel';
import BidHistory from '@/components/BidHistory';
import { useListingChannel } from '@/realtime/useListingChannel';
import { formatPrice, formatTimeLeft } from '@/lib/format';
import { statusBadgeVariant } from '@/lib/listing';

const lotNumber = (id: number): string => `LOT ${String(id).padStart(3, '0')}`;

const ListingDetailPage: FC = () => {
  const { id } = useParams();
  const listingId = Number(id);
  const navigate = useNavigate();
  const token = useAuthStore((s) => s.token);

  const {
    data: listing,
    isLoading,
    isError,
    refetch,
  } = useGetListing(listingId, { query: { enabled: Number.isFinite(listingId) } });
  const { data: me } = useMe({ query: { enabled: Boolean(token) } });

  // Subscribe to live price/bid/close events for this listing.
  useListingChannel(listingId);

  const publishMutation = usePublishListing();
  const deleteMutation = useDeleteListing();

  if (isLoading) {
    return (
      <div className="relative z-10 mx-auto max-w-5xl px-5 py-12">
        <div className="aspect-[16/10] w-full animate-pulse bg-muted" />
      </div>
    );
  }
  if (isError || !listing) {
    return (
      <div className="relative z-10 mx-auto max-w-5xl px-5 py-24 text-center">
        <p role="alert" className="font-display text-3xl">
          This lot has left the room.
        </p>
        <Button asChild variant="link" className="mt-2 font-mono uppercase tracking-widest">
          <Link to="/">Back to the catalogue</Link>
        </Button>
      </div>
    );
  }

  const isOwner = me?.id != null && me.id === listing.sellerId;
  const isDraft = listing.status === 'DRAFT';
  const photos = listing.photos ?? [];
  const isAuthed = Boolean(token);
  const endsInFuture = listing.endAt != null && new Date(listing.endAt).getTime() > Date.now();
  const isBiddable = listing.status === 'ACTIVE' && endsInFuture;
  const isTopBidder = me?.id != null && me.id === listing.currentBidderId;
  const hasWinner = listing.currentBidderId != null;

  const onPublish = (): void => {
    publishMutation.mutate(
      { id: listingId },
      {
        onSuccess: () => {
          toast.success('Lot is live on the block.');
          refetch();
        },
        onError: () => toast.error('Could not publish the lot.'),
      },
    );
  };
  const onDelete = (): void => {
    deleteMutation.mutate(
      { id: listingId },
      {
        onSuccess: () => {
          toast.success(isDraft ? 'Draft discarded.' : 'Lot withdrawn.');
          navigate('/my-listings');
        },
        onError: () => toast.error('Could not remove the lot.'),
      },
    );
  };

  return (
    <div className="relative z-10 mx-auto max-w-5xl px-5 py-8">
      <div className="mb-6 flex items-center justify-between border-b-2 border-foreground/80 pb-3">
        <Link
          to="/"
          className="flex items-center gap-1.5 font-mono text-xs uppercase tracking-widest text-muted-foreground transition-colors hover:text-foreground"
        >
          <ArrowLeft className="size-3.5" /> Catalogue
        </Link>
        <span className="font-mono text-xs font-semibold uppercase tracking-[0.2em]">
          {lotNumber(listingId)}
        </span>
      </div>

      <div className="grid grid-cols-1 gap-10 lg:grid-cols-[1.1fr_1fr]">
        {/* Plates */}
        <div className="space-y-3">
          {photos.length > 0 ? (
            <>
              <div className="aspect-[4/3] overflow-hidden border border-foreground/15 bg-muted">
                <img src={photos[0]} alt={listing.title} className="size-full object-cover" />
              </div>
              {photos.length > 1 && (
                <div className="grid grid-cols-4 gap-3">
                  {photos.slice(1, 5).map((url) => (
                    <div
                      key={url}
                      className="aspect-square overflow-hidden border border-foreground/15 bg-muted"
                    >
                      <img src={url} alt={listing.title} className="size-full object-cover" />
                    </div>
                  ))}
                </div>
              )}
            </>
          ) : (
            <div className="flex aspect-[4/3] items-center justify-center border border-dashed border-foreground/30 bg-muted">
              <span className="label-mono">No plates on file</span>
            </div>
          )}
        </div>

        {/* Lot record */}
        <div>
          <div className="flex items-center gap-2">
            <Badge
              variant={statusBadgeVariant(listing.status)}
              className="rounded-sm font-mono text-[0.65rem] uppercase tracking-widest"
            >
              {listing.status}
            </Badge>
            <span className="label-mono">
              {listing.category} · {listing.condition}
            </span>
          </div>

          <h1 className="mt-3 font-display text-4xl font-medium leading-[1.05] tracking-tight">
            {listing.title}
          </h1>

          <div className="mt-6 flex flex-col gap-4 border-y border-foreground/20 py-4 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <p className="label-mono">
                {(listing.bidCount ?? 0) > 0 ? 'Current bid' : 'Starting price'}
              </p>
              <p className="tabular mt-1 font-mono text-3xl font-semibold text-foreground sm:text-4xl">
                {formatPrice(listing.currentPrice)}
              </p>
              {(listing.bidCount ?? 0) > 0 && (
                <p className="label-mono mt-1 text-muted-foreground">
                  {listing.bidCount} {listing.bidCount === 1 ? 'bid' : 'bids'}
                </p>
              )}
            </div>
            <div className="sm:text-right">
              <p className="label-mono flex items-center gap-1 sm:justify-end">
                <Clock className="size-3" /> {formatTimeLeft(listing.endAt)}
              </p>
              <p className="mt-1 font-mono text-xs text-muted-foreground">
                Opens at {formatPrice(listing.startPrice)} · +{formatPrice(listing.bidIncrement)}/bid
              </p>
            </div>
          </div>

          <p className="mt-5 whitespace-pre-line text-sm leading-relaxed text-foreground/90">
            {listing.description}
          </p>

          <dl className="mt-6 space-y-2 border-t border-dashed border-foreground/20 pt-4 text-sm">
            <div className="flex items-center gap-2 font-mono text-xs text-muted-foreground">
              <MapPin className="size-3.5" />
              {listing.pickupLocation ?? 'Pickup to be arranged'}
            </div>
            <div className="font-mono text-xs text-muted-foreground">
              Consigned by {listing.sellerName}
            </div>
          </dl>

          {/* Bidding */}
          {isBiddable && !isOwner && isAuthed && (
            <BidPanel listing={listing} isTopBidder={isTopBidder} />
          )}
          {isBiddable && !isAuthed && (
            <div className="mt-6 border-t border-foreground/15 pt-5">
              <p className="label-mono mb-2 text-muted-foreground">Want this lot?</p>
              <Button
                asChild
                className="rounded-sm font-mono text-xs uppercase tracking-widest"
              >
                <Link to="/login">Log in to bid</Link>
              </Button>
            </div>
          )}
          {isBiddable && isOwner && (
            <p className="label-mono mt-6 border-t border-foreground/15 pt-5 text-muted-foreground">
              This is your lot — bidding is open to buyers.
            </p>
          )}
          {listing.status === 'ENDED' && (
            <div className="mt-6 border-t border-foreground/15 pt-5">
              <p className="font-display text-xl">
                {isTopBidder ? 'You won this lot.' : `Won by ${listing.currentBidderName}`}
              </p>
              <p className="label-mono mt-1 text-muted-foreground">
                Hammer price {formatPrice(listing.currentPrice)}
              </p>
            </div>
          )}
          {listing.status === 'CLOSED' && (
            <p className="label-mono mt-6 border-t border-foreground/15 pt-5 text-muted-foreground">
              {hasWinner ? 'Auction closed.' : 'Auction closed — no winning bid.'}
            </p>
          )}

          {/* History (any published lot) */}
          {listing.status !== 'DRAFT' && <BidHistory listingId={listingId} />}

          {isOwner && (
            <div className="mt-7 flex flex-wrap gap-2 border-t border-foreground/15 pt-5">
              <Button
                asChild
                variant="outline"
                size="sm"
                className="rounded-sm border-foreground/30 font-mono text-xs uppercase tracking-widest"
              >
                <Link to={`/listings/${listing.id}/edit`}>
                  <Pencil className="size-3.5" /> Edit
                </Link>
              </Button>
              {isDraft && (
                <Button
                  size="sm"
                  onClick={onPublish}
                  disabled={publishMutation.isPending}
                  className="rounded-sm font-mono text-xs uppercase tracking-widest"
                >
                  <Upload className="size-3.5" /> Send to block
                </Button>
              )}
              <Button
                variant="destructive"
                size="sm"
                onClick={onDelete}
                disabled={deleteMutation.isPending}
                className="rounded-sm font-mono text-xs uppercase tracking-widest"
              >
                <Trash2 className="size-3.5" /> {isDraft ? 'Discard' : 'Withdraw'}
              </Button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ListingDetailPage;
