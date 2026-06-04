import type { FC } from 'react';
import { Link } from 'react-router-dom';
import type { ListingResponse } from '@/api/generated/model';
import { formatPrice, formatTimeLeft } from '@/lib/format';

interface ListingCardProps {
  listing: ListingResponse;
}

const ListingCard: FC<ListingCardProps> = ({ listing }) => (
  <Link to={`/listings/${listing.id}`} className="listing-card">
    {listing.photos && listing.photos.length > 0 ? (
      <img src={listing.photos[0]} alt={listing.title} width={220} height={150} />
    ) : (
      <div className="listing-card__placeholder">No photo</div>
    )}
    <h3>{listing.title}</h3>
    <p className="listing-card__price">{formatPrice(listing.currentPrice)}</p>
    <p className="listing-card__meta">
      {listing.category} · {listing.condition}
    </p>
    <p className="listing-card__time">{formatTimeLeft(listing.endAt)}</p>
  </Link>
);

export default ListingCard;
