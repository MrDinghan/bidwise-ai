package com.bidwise.payment;

/**
 * Raised when a payment provider call fails (authorize/capture/release) or a webhook
 * signature cannot be verified. Capture failures during settlement are caught and
 * recorded as a {@link SettlementStatus#FAILED} settlement; a verification failure on
 * the webhook endpoint surfaces as a 400.
 */
public class PaymentGatewayException extends RuntimeException {

    public PaymentGatewayException(String message) {
        super(message);
    }

    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
