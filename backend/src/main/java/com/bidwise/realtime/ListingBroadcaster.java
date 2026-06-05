package com.bidwise.realtime;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes {@link BidEvent}s to a listing's STOMP topic so every subscriber sees
 * price/lifecycle changes in real time.
 */
@Component
public class ListingBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public ListingBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /** Sends an event to {@code /topic/listings/{id}}. */
    public void broadcast(BidEvent event) {
        messagingTemplate.convertAndSend("/topic/listings/" + event.listingId(), event);
    }
}
