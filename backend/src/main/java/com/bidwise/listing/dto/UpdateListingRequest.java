package com.bidwise.listing.dto;

import com.bidwise.listing.Category;
import com.bidwise.listing.ItemCondition;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Payload to edit the mutable fields of a listing. Status is changed through the
 * dedicated publish/delete endpoints, not here.
 */
public record UpdateListingRequest(
        @NotBlank @Size(max = 140) String title,
        @NotNull Category category,
        @NotBlank @Size(max = 4000) String description,
        @NotNull ItemCondition condition,
        @Size(max = 20) List<@NotBlank @Size(max = 1000) String> photos,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal startPrice,
        @NotNull @DecimalMin(value = "0.01") BigDecimal bidIncrement,
        @Size(max = 255) String pickupLocation,
        @Future Instant endAt) {
}
