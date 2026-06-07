package com.bidwise.payment.dto;

import com.bidwise.payment.PaymentHold;
import com.bidwise.payment.PaymentHoldStatus;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * A buyer's deposit hold on a listing, as returned by the deposit endpoints. The amount
 * is the server-computed deposit; the client never sets it.
 */
public record PaymentHoldResponse(
        Long id,
        Long listingId,
        BigDecimal amount,
        PaymentHoldStatus status,
        Instant createdAt) {

    /** Maps a persisted hold to its API representation (call within a transaction). */
    public static PaymentHoldResponse from(PaymentHold hold) {
        return new PaymentHoldResponse(
                hold.getId(),
                hold.getListing().getId(),
                hold.getAmount(),
                hold.getStatus(),
                hold.getCreatedAt());
    }
}
