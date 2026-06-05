import { useEffect } from 'react';
import { Client } from '@stomp/stompjs';
import { useQueryClient } from '@tanstack/react-query';
import { getGetListingQueryKey } from '@/api/generated/listings/listings';
import { getGetBidsQueryKey } from '@/api/generated/bids/bids';
import type { ListingResponse } from '@/api/generated/model';
import type { BidEvent } from './types';

/** Same-origin STOMP-over-WebSocket endpoint (proxied to the backend in dev). */
const brokerUrl = (): string => {
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
  return `${protocol}://${window.location.host}/ws`;
};

/**
 * Subscribes to a listing's real-time topic and folds each {@link BidEvent} into the
 * TanStack Query cache: the listing's price/bidder/countdown update in place, and the
 * bid-history query is invalidated so it refetches. One STOMP client per mounted
 * listing detail view; it reconnects automatically and tears down on unmount.
 */
export const useListingChannel = (listingId: number): void => {
  const queryClient = useQueryClient();

  useEffect(() => {
    if (!Number.isFinite(listingId)) {
      return;
    }

    const client = new Client({
      brokerURL: brokerUrl(),
      reconnectDelay: 4000,
      onConnect: () => {
        client.subscribe(`/topic/listings/${listingId}`, (message) => {
          const event = JSON.parse(message.body) as BidEvent;

          queryClient.setQueryData<ListingResponse>(
            getGetListingQueryKey(listingId),
            (prev) =>
              prev
                ? {
                    ...prev,
                    currentPrice: event.currentPrice,
                    bidCount: event.bidCount,
                    currentBidderId: event.highestBidderId ?? undefined,
                    currentBidderName: event.highestBidderName ?? undefined,
                    endAt: event.endAt ?? prev.endAt,
                    status: event.status as ListingResponse['status'],
                  }
                : prev,
          );

          void queryClient.invalidateQueries({ queryKey: getGetBidsQueryKey(listingId) });
        });
      },
    });

    client.activate();
    return () => {
      void client.deactivate();
    };
  }, [listingId, queryClient]);
};
