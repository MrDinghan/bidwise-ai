package com.bidwise.listing;

/**
 * Lifecycle of a {@link Listing}.
 *
 * <p>P1 transitions through {@code DRAFT → ACTIVE → CLOSED}. P2 (bidding) adds
 * {@code ENDED}: an auction that ran its course with a winning bidder and is now
 * awaiting payment/settlement. The remaining states are reserved for later
 * milestones: {@code SOLD} is set by settlement (P3) once an {@code ENDED} auction
 * is paid, and {@code PENDING_REVIEW}/{@code REJECTED} by AI moderation (P5).
 */
public enum ListingStatus {
    DRAFT,
    PENDING_REVIEW,
    ACTIVE,
    ENDED,
    CLOSED,
    SOLD,
    REJECTED
}
