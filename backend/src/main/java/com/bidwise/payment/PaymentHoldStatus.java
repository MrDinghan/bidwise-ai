package com.bidwise.payment;

/**
 * Lifecycle of a deposit hold. {@code AUTHORIZED} on creation (a manual-capture hold);
 * settlement moves the winner's hold to {@code CAPTURED} and every other hold to
 * {@code VOIDED}.
 */
public enum PaymentHoldStatus {
    AUTHORIZED,
    CAPTURED,
    VOIDED
}
