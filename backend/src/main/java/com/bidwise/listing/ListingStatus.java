package com.bidwise.listing;

/**
 * Lifecycle of a {@link Listing}.
 *
 * <p>P1 only transitions through {@code DRAFT → ACTIVE → CLOSED}. The remaining
 * states are reserved for later milestones: {@code SOLD} is set by settlement (P3),
 * and {@code PENDING_REVIEW}/{@code REJECTED} by AI moderation (P5).
 */
public enum ListingStatus {
    DRAFT,
    PENDING_REVIEW,
    ACTIVE,
    CLOSED,
    SOLD,
    REJECTED
}
