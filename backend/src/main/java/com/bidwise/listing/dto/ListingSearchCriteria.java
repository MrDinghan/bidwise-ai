package com.bidwise.listing.dto;

import com.bidwise.listing.Category;
import com.bidwise.listing.ItemCondition;
import java.math.BigDecimal;

/**
 * Filters for the public listing search. All fields are optional; any combination
 * may be supplied. Only {@code ACTIVE} listings are ever returned.
 */
public record ListingSearchCriteria(
        String q,
        Category category,
        ItemCondition condition,
        BigDecimal minPrice,
        BigDecimal maxPrice) {
}
