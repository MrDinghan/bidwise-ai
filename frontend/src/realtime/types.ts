/**
 * Payload pushed over the listing's STOMP topic. This mirrors the backend
 * `com.bidwise.realtime.BidEvent`. It is the one hand-written API type in the app:
 * WebSocket messages are not part of the OpenAPI contract, so Orval cannot generate
 * it (all REST types/hooks still come from the generated client). Keep it in sync
 * with the backend record.
 */
export interface BidEvent {
  type: 'BID' | 'CLOSED';
  listingId: number;
  currentPrice: number;
  bidCount: number;
  highestBidderId: number | null;
  highestBidderName: string | null;
  endAt: string | null;
  status: string;
  at: string;
}
