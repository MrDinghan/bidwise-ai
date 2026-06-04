import { useEffect, useState, type FC, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { Loader2 } from 'lucide-react';
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
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { toDateTimeLocal } from '@/lib/format';

type ListingFormProps = { mode: 'create' } | { mode: 'edit'; listingId: number };

const labelClass = 'label-mono text-foreground';
const fieldClass = 'rounded-sm border-foreground/20';

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
    const data = buildPayload();
    const onError = (): void => {
      toast.error('Could not save the listing. Check the fields and retry.');
    };

    if (props.mode === 'edit') {
      updateMutation.mutate(
        { id: props.listingId, data },
        {
          onSuccess: (listing) => {
            toast.success('Lot updated.');
            navigate(`/listings/${listing.id}`);
          },
          onError,
        },
      );
    } else {
      createMutation.mutate(
        { data },
        {
          onSuccess: (listing) => {
            toast.success('Draft saved to your lots.');
            navigate(`/listings/${listing.id}`);
          },
          onError,
        },
      );
    }
  };

  return (
    <div className="relative z-10 mx-auto max-w-2xl px-5 py-10">
      <div className="border border-foreground/15 bg-card">
        <div className="flex items-center justify-between border-b-2 border-foreground/80 px-6 py-4">
          <h1 className="font-display text-2xl font-medium tracking-tight">
            {props.mode === 'edit' ? 'Edit listing' : 'New listing'}
          </h1>
          <span className="label-mono hidden sm:block">Consignment form</span>
        </div>

        <form onSubmit={onSubmit} className="space-y-6 p-6">
          <div className="space-y-2">
            <Label htmlFor="title" className={labelClass}>
              Title
            </Label>
            <Input
              id="title"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              maxLength={140}
              required
              className={fieldClass}
            />
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="category" className={labelClass}>
                Category
              </Label>
              <Select value={category} onValueChange={setCategory}>
                <SelectTrigger id="category" className={`w-full ${fieldClass}`}>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {Object.values(CreateListingRequestCategory).map((value) => (
                    <SelectItem key={value} value={value}>
                      {value}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="condition" className={labelClass}>
                Condition
              </Label>
              <Select value={condition} onValueChange={setCondition}>
                <SelectTrigger id="condition" className={`w-full ${fieldClass}`}>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {Object.values(CreateListingRequestCondition).map((value) => (
                    <SelectItem key={value} value={value}>
                      {value}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="description" className={labelClass}>
              Description
            </Label>
            <Textarea
              id="description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              maxLength={4000}
              rows={5}
              required
              className={fieldClass}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="photos" className={labelClass}>
              Photo URLs (one per line)
            </Label>
            <Textarea
              id="photos"
              value={photos}
              onChange={(e) => setPhotos(e.target.value)}
              rows={3}
              placeholder="https://…"
              className={`${fieldClass} font-mono text-xs`}
            />
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="startPrice" className={labelClass}>
                Start price
              </Label>
              <Input
                id="startPrice"
                type="number"
                min="0.01"
                step="0.01"
                value={startPrice}
                onChange={(e) => setStartPrice(e.target.value)}
                required
                className={`${fieldClass} font-mono`}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="bidIncrement" className={labelClass}>
                Bid increment
              </Label>
              <Input
                id="bidIncrement"
                type="number"
                min="0.01"
                step="0.01"
                value={bidIncrement}
                onChange={(e) => setBidIncrement(e.target.value)}
                required
                className={`${fieldClass} font-mono`}
              />
            </div>
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="pickupLocation" className={labelClass}>
                Pickup location
              </Label>
              <Input
                id="pickupLocation"
                value={pickupLocation}
                onChange={(e) => setPickupLocation(e.target.value)}
                className={fieldClass}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="endAt" className={labelClass}>
                Ends at
              </Label>
              <Input
                id="endAt"
                type="datetime-local"
                value={endAt}
                onChange={(e) => setEndAt(e.target.value)}
                className={`${fieldClass} font-mono`}
              />
            </div>
          </div>

          <Button
            type="submit"
            disabled={isPending}
            className="w-full rounded-sm font-mono text-xs uppercase tracking-widest sm:w-auto"
          >
            {isPending && <Loader2 className="size-4 animate-spin" />}
            {isPending ? 'Saving…' : 'Save draft'}
          </Button>
        </form>
      </div>
    </div>
  );
};

export default ListingForm;
