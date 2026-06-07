package com.bidwise.payment;

/**
 * Outcome of settling a closed auction. {@code CAPTURED} when the winner's deposit was
 * charged; {@code FAILED} when capture errored (the listing stays {@code ENDED} for a
 * retry); {@code PENDING} is the transient initial state.
 */
public enum SettlementStatus {
    PENDING,
    CAPTURED,
    FAILED
}
