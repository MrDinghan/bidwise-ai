package com.bidwise.listing.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * A page of listings. A concrete DTO (rather than Spring's {@code Page<T>}) so the
 * OpenAPI schema and the generated frontend types stay stable.
 */
public record ListingPageResponse(
        List<ListingResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    /** Wraps a Spring Data page of entities, mapping each to a {@link ListingResponse}. */
    public static ListingPageResponse from(Page<com.bidwise.listing.Listing> page) {
        return new ListingPageResponse(
                page.getContent().stream().map(ListingResponse::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
