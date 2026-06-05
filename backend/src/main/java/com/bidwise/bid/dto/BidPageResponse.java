package com.bidwise.bid.dto;

import com.bidwise.bid.Bid;
import java.util.List;
import org.springframework.data.domain.Page;

/**
 * A page of bids. A concrete DTO (rather than Spring's {@code Page<T>}) so the
 * OpenAPI schema and the generated frontend types stay stable.
 */
public record BidPageResponse(
        List<BidResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    /** Wraps a Spring Data page of bids, mapping each to a {@link BidResponse}. */
    public static BidPageResponse from(Page<Bid> page) {
        return new BidPageResponse(
                page.getContent().stream().map(BidResponse::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
