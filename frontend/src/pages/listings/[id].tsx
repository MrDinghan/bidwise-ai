import type { FC } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useMe } from '@/api/generated/auth/auth';
import {
  useDeleteListing,
  useGetListing,
  usePublishListing,
} from '@/api/generated/listings/listings';
import { useAuthStore } from '@/auth/store';
import { formatPrice, formatTimeLeft } from '@/lib/format';

const ListingDetailPage: FC = () => {
  const { id } = useParams();
  const listingId = Number(id);
  const navigate = useNavigate();
  const token = useAuthStore((s) => s.token);

  const { data: listing, isLoading, isError, refetch } = useGetListing(listingId, {
    query: { enabled: Number.isFinite(listingId) },
  });
  const { data: me } = useMe({ query: { enabled: Boolean(token) } });

  const publishMutation = usePublishListing();
  const deleteMutation = useDeleteListing();

  if (isLoading) {
    return <p>Loading…</p>;
  }
  if (isError || !listing) {
    return <p role="alert">Listing not found.</p>;
  }

  const isOwner = me?.id != null && me.id === listing.sellerId;

  const onPublish = (): void => {
    publishMutation.mutate({ id: listingId }, { onSuccess: () => refetch() });
  };
  const onDelete = (): void => {
    deleteMutation.mutate({ id: listingId }, { onSuccess: () => navigate('/my-listings') });
  };

  return (
    <main>
      <p>
        <Link to="/">← Back to listings</Link>
      </p>
      <h1>{listing.title}</h1>
      <p className="listing-detail__status">Status: {listing.status}</p>

      <div className="listing-detail__photos">
        {(listing.photos ?? []).map((url) => (
          <img key={url} src={url} alt={listing.title} width={320} />
        ))}
      </div>

      <p>{listing.description}</p>
      <ul>
        <li>Category: {listing.category}</li>
        <li>Condition: {listing.condition}</li>
        <li>Current price: {formatPrice(listing.currentPrice)}</li>
        <li>Start price: {formatPrice(listing.startPrice)}</li>
        <li>Bid increment: {formatPrice(listing.bidIncrement)}</li>
        <li>Pickup: {listing.pickupLocation ?? '—'}</li>
        <li>Seller: {listing.sellerName}</li>
        <li>Time left: {formatTimeLeft(listing.endAt)}</li>
      </ul>

      {isOwner && (
        <div className="listing-detail__actions">
          <Link to={`/listings/${listing.id}/edit`}>Edit</Link>
          {listing.status === 'DRAFT' && (
            <button type="button" onClick={onPublish} disabled={publishMutation.isPending}>
              Publish
            </button>
          )}
          <button type="button" onClick={onDelete} disabled={deleteMutation.isPending}>
            {listing.status === 'DRAFT' ? 'Delete' : 'Cancel listing'}
          </button>
        </div>
      )}
    </main>
  );
};

export default ListingDetailPage;
