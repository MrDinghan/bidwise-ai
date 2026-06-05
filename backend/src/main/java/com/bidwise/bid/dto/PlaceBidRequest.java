package com.bidwise.bid.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Request to place a bid. The amount must clear the listing's current bar; that
 * check is enforced atomically server-side, not by this annotation alone.
 */
public record PlaceBidRequest(
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount) {
}
