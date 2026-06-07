/** Formatting helpers shared across listing views. */

export const formatPrice = (value?: number): string =>
  value == null
    ? '—'
    : new Intl.NumberFormat(undefined, { style: 'currency', currency: 'USD' }).format(value);

export const formatTimeLeft = (endAt?: string): string => {
  if (!endAt) {
    return '—';
  }
  const remainingMs = new Date(endAt).getTime() - Date.now();
  if (remainingMs <= 0) {
    return 'Ended';
  }
  const totalHours = Math.floor(remainingMs / 3_600_000);
  const days = Math.floor(totalHours / 24);
  if (days > 0) {
    return `${days}d ${totalHours % 24}h left`;
  }
  const minutes = Math.floor((remainingMs % 3_600_000) / 60_000);
  return `${totalHours}h ${minutes}m left`;
};

/** Compact "time since" label for bid timestamps, e.g. "just now", "3m", "2h", "5d". */
export const formatTimeAgo = (iso?: string): string => {
  if (!iso) {
    return '';
  }
  const elapsedMs = Date.now() - new Date(iso).getTime();
  if (elapsedMs < 60_000) {
    return 'just now';
  }
  const minutes = Math.floor(elapsedMs / 60_000);
  if (minutes < 60) {
    return `${minutes}m ago`;
  }
  const hours = Math.floor(minutes / 60);
  if (hours < 24) {
    return `${hours}h ago`;
  }
  return `${Math.floor(hours / 24)}d ago`;
};
