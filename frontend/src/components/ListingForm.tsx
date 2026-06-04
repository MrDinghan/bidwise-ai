import { useEffect, useState, type FC, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  useCreateListing,
  useGetListing,
  useUpdateListing,
} from '@/api/generated/listings/listings';
import {
  CreateListingRequestCategory,
  CreateListingRequestCondition,
  type CreateListingRequest,
} from '@/api/generated/model';
import { toDateTimeLocal } from '@/lib/format';

type ListingFormProps = { mode: 'create' } | { mode: 'edit'; listingId: number };

const ListingForm: FC<ListingFormProps> = (props) => {
  const navigate = useNavigate();

  const [title, setTitle] = useState('');
  const [category, setCategory] = useState<string>(CreateListingRequestCategory.ELECTRONICS);
  const [condition, setCondition] = useState<string>(CreateListingRequestCondition.GOOD);
  const [description, setDescription] = useState('');
  const [photos, setPhotos] = useState('');
  const [startPrice, setStartPrice] = useState('');
  const [bidIncrement, setBidIncrement] = useState('');
  const [pickupLocation, setPickupLocation] = useState('');
  const [endAt, setEndAt] = useState('');
  const [error, setError] = useState<string | null>(null);

  const editId = props.mode === 'edit' ? props.listingId : undefined;
  const existing = useGetListing(editId as number, {
    query: { enabled: editId != null && Number.isFinite(editId) },
  });

  // Prefill the form once the listing being edited has loaded.
  useEffect(() => {
    const listing = existing.data;
    if (!listing) {
      return;
    }
    setTitle(listing.title ?? '');
    setCategory(listing.category ?? CreateListingRequestCategory.ELECTRONICS);
    setCondition(listing.condition ?? CreateListingRequestCondition.GOOD);
    setDescription(listing.description ?? '');
    setPhotos((listing.photos ?? []).join('\n'));
    setStartPrice(listing.startPrice != null ? String(listing.startPrice) : '');
    setBidIncrement(listing.bidIncrement != null ? String(listing.bidIncrement) : '');
    setPickupLocation(listing.pickupLocation ?? '');
    setEndAt(toDateTimeLocal(listing.endAt));
  }, [existing.data]);

  const createMutation = useCreateListing();
  const updateMutation = useUpdateListing();
  const isPending = createMutation.isPending || updateMutation.isPending;

  const buildPayload = (): CreateListingRequest => ({
    title,
    category: category as CreateListingRequest['category'],
    description,
    condition: condition as CreateListingRequest['condition'],
    photos: photos
      .split('\n')
      .map((line) => line.trim())
      .filter(Boolean),
    startPrice: Number(startPrice),
    bidIncrement: Number(bidIncrement),
    pickupLocation: pickupLocation || undefined,
    endAt: endAt ? new Date(endAt).toISOString() : undefined,
  });

  const onSubmit = (e: FormEvent): void => {
    e.preventDefault();
    setError(null);
    const data = buildPayload();
    const onError = (): void => setError('Could not save the listing. Check the fields and retry.');

    if (props.mode === 'edit') {
      updateMutation.mutate(
        { id: props.listingId, data },
        {
          onSuccess: (listing) => navigate(`/listings/${listing.id}`),
          onError,
        },
      );
    } else {
      createMutation.mutate(
        { data },
        {
          onSuccess: (listing) => navigate(`/listings/${listing.id}`),
          onError,
        },
      );
    }
  };

  return (
    <main>
      <h1>{props.mode === 'edit' ? 'Edit listing' : 'New listing'}</h1>
      <form onSubmit={onSubmit}>
        <label>
          Title
          <input value={title} onChange={(e) => setTitle(e.target.value)} maxLength={140} required />
        </label>
        <label>
          Category
          <select value={category} onChange={(e) => setCategory(e.target.value)}>
            {Object.values(CreateListingRequestCategory).map((value) => (
              <option key={value} value={value}>
                {value}
              </option>
            ))}
          </select>
        </label>
        <label>
          Condition
          <select value={condition} onChange={(e) => setCondition(e.target.value)}>
            {Object.values(CreateListingRequestCondition).map((value) => (
              <option key={value} value={value}>
                {value}
              </option>
            ))}
          </select>
        </label>
        <label>
          Description
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            maxLength={4000}
            required
          />
        </label>
        <label>
          Photo URLs (one per line)
          <textarea value={photos} onChange={(e) => setPhotos(e.target.value)} />
        </label>
        <label>
          Start price
          <input
            type="number"
            min="0.01"
            step="0.01"
            value={startPrice}
            onChange={(e) => setStartPrice(e.target.value)}
            required
          />
        </label>
        <label>
          Bid increment
          <input
            type="number"
            min="0.01"
            step="0.01"
            value={bidIncrement}
            onChange={(e) => setBidIncrement(e.target.value)}
            required
          />
        </label>
        <label>
          Pickup location
          <input value={pickupLocation} onChange={(e) => setPickupLocation(e.target.value)} />
        </label>
        <label>
          Ends at
          <input type="datetime-local" value={endAt} onChange={(e) => setEndAt(e.target.value)} />
        </label>
        <button type="submit" disabled={isPending}>
          {isPending ? 'Saving…' : 'Save draft'}
        </button>
      </form>
      {error && <p role="alert">{error}</p>}
    </main>
  );
};

export default ListingForm;
