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

/** Converts an ISO timestamp to the value a `datetime-local` input expects. */
export const toDateTimeLocal = (iso?: string): string => {
  if (!iso) {
    return '';
  }
  const date = new Date(iso);
  const offsetMs = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - offsetMs).toISOString().slice(0, 16);
};
