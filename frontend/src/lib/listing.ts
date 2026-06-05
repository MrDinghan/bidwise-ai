import type { ListingResponseStatus } from '@/api/generated/model';

type BadgeVariant = 'default' | 'secondary' | 'destructive' | 'outline' | 'gold';

/** Map a listing status to a Badge variant for consistent status chips. */
export const statusBadgeVariant = (status: ListingResponseStatus | undefined): BadgeVariant => {
  switch (status) {
    case 'ACTIVE':
      return 'default';
    case 'ENDED':
    case 'SOLD':
      return 'gold';
    case 'DRAFT':
    case 'PENDING_REVIEW':
      return 'secondary';
    case 'CLOSED':
    case 'REJECTED':
      return 'destructive';
    default:
      return 'outline';
  }
};
