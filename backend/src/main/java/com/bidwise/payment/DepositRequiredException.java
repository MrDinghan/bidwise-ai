package com.bidwise.payment;

/**
 * Raised when a buyer tries to bid without an authorized deposit hold on the listing.
 * Surfaces as HTTP 402 Payment Required so the client can prompt the deposit step.
 */
public class DepositRequiredException extends RuntimeException {

    public DepositRequiredException() {
        super("A deposit is required before you can bid on this auction");
    }
}
