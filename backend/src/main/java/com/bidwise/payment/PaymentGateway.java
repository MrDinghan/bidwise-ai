package com.bidwise.payment;

import java.math.BigDecimal;

/**
 * Port over a payment provider's auth-and-capture flow. A deposit is authorized (a
 * manual-capture hold, not a charge), then later captured (winner) or released
 * (losers). The gateway also verifies inbound webhook signatures.
 *
 * <p>Two adapters exist: {@link FakePaymentGateway} (default, an in-memory hold ledger
 * that needs no external service — used for local dev and tests) and
 * {@link StripePaymentGateway} (Stripe test mode). Business code depends only on this
 * interface so the provider is swappable and the flow stays testable.
 */
public interface PaymentGateway {

    /**
     * Authorizes a hold for {@code amount} against the buyer. The {@code idempotencyKey}
     * makes a retry return the same hold rather than creating a duplicate.
     *
     * @param customerRef provider-agnostic buyer reference (e.g. email / customer id)
     * @param amount the amount to hold, in major currency units
     * @param idempotencyKey stable key for safe retries
     * @return the created authorization
     */
    Authorization authorize(String customerRef, BigDecimal amount, String idempotencyKey);

    /** Captures a previously authorized hold (charges the buyer). */
    void capture(String paymentIntentId);

    /** Releases (voids) a previously authorized hold without charging. */
    void release(String paymentIntentId);

    /**
     * Verifies a webhook payload's signature and parses it into a provider-agnostic
     * event. Throws if the signature is invalid.
     */
    WebhookEvent verify(String payload, String signature);

    /** The result of an authorization: the provider's hold/intent identifier. */
    record Authorization(String paymentIntentId) {
    }

    /** A verified webhook event, reduced to what settlement/fulfillment needs. */
    record WebhookEvent(String eventId, String type, String paymentIntentId) {
    }
}
