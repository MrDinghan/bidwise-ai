package com.bidwise.listing;

import java.time.Duration;

/**
 * How long an auction runs once published. The seller picks one of a fixed set of
 * options rather than an arbitrary end time: a 1-second or 1-year auction is
 * unreasonable for bidders, and the window must stay inside Stripe's ~7-day pre-auth
 * hold validity so capture-on-close cannot fail (see DESIGN §9). The cap is therefore
 * 5 days, leaving margin.
 *
 * <p>The server computes {@code endAt = startAt + duration} on {@link Listing#publish()};
 * the end time is never supplied by the client.
 */
public enum AuctionDuration {
    ONE_DAY(Duration.ofDays(1)),
    THREE_DAYS(Duration.ofDays(3)),
    FIVE_DAYS(Duration.ofDays(5));

    private final Duration duration;

    AuctionDuration(Duration duration) {
        this.duration = duration;
    }

    /** The wall-clock length of this auction window. */
    public Duration toDuration() {
        return duration;
    }
}
