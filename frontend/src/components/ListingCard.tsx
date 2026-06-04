import type { FC } from 'react';
import { Link } from 'react-router-dom';
import { ImageOff } from 'lucide-react';
import type { ListingResponse } from '@/api/generated/model';
import { formatPrice, formatTimeLeft } from '@/lib/format';

interface ListingCardProps {
  listing: ListingResponse;
}

const lotNumber = (id: number | undefined): string => `LOT ${String(id ?? 0).padStart(3, '0')}`;

const ListingCard: FC<ListingCardProps> = ({ listing }) => (
  <Link
    to={`/listings/${listing.id}`}
    className="group relative flex flex-col border border-foreground/15 bg-card transition-colors duration-300 hover:border-foreground/40"
  >
    <div className="relative aspect-[4/5] overflow-hidden bg-muted">
      {listing.photos && listing.photos.length > 0 ? (
        <img
          src={listing.photos[0]}
          alt={listing.title}
          className="size-full object-cover transition-transform duration-500 ease-out group-hover:scale-105"
        />
      ) : (
        <div className="flex size-full flex-col items-center justify-center gap-1.5 text-muted-foreground">
          <ImageOff className="size-6" />
          <span className="label-mono">No plate</span>
        </div>
      )}

      <span className="absolute left-0 top-0 bg-background/90 px-2 py-1 font-mono text-[0.65rem] font-semibold tracking-widest text-foreground backdrop-blur-sm">
        {lotNumber(listing.id)}
      </span>
      <span className="absolute right-0 top-0 bg-foreground/90 px-2 py-1 font-mono text-[0.65rem] font-medium tracking-wide text-background">
        {formatTimeLeft(listing.endAt)}
      </span>
    </div>

    <div className="flex flex-1 flex-col gap-3 p-4">
      <h3
        className="font-display text-lg leading-tight decoration-primary decoration-2 underline-offset-4 group-hover:underline"
        title={listing.title}
      >
        {listing.title}
      </h3>

      <p className="label-mono">
        {listing.category} · {listing.condition}
      </p>

      <div className="mt-auto flex items-baseline justify-between border-t border-dashed border-foreground/20 pt-3">
        <span className="label-mono text-[0.6rem]">Current</span>
        <span className="tabular font-mono text-xl font-semibold text-foreground">
          {formatPrice(listing.currentPrice)}
        </span>
      </div>
    </div>
  </Link>
);

export default ListingCard;
