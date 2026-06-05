package com.bidwise.listing.dto;

import com.bidwise.listing.Category;
import com.bidwise.listing.ItemCondition;
import com.bidwise.listing.Listing;
import com.bidwise.listing.ListingStatus;
import com.bidwise.user.User;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Full listing representation returned by the API.
 */
public record ListingResponse(
        Long id,
        Long sellerId,
        String sellerName,
        String title,
        Category category,
        String description,
        ItemCondition condition,
        List<String> photos,
        BigDecimal startPrice,
        BigDecimal bidIncrement,
        BigDecimal currentPrice,
        Long currentBidderId,
        String currentBidderName,
        int bidCount,
        ListingStatus status,
        Instant startAt,
        Instant endAt,
        String pickupLocation,
        Instant createdAt) {

    /** Maps a persisted listing to its API representation (call within a transaction). */
    public static ListingResponse from(Listing listing) {
        User currentBidder = listing.getCurrentBidder();
        return new ListingResponse(
                listing.getId(),
                listing.getSeller().getId(),
                listing.getSeller().getName(),
                listing.getTitle(),
                listing.getCategory(),
                listing.getDescription(),
                listing.getCondition(),
                List.copyOf(listing.getPhotos()),
                listing.getStartPrice(),
                listing.getBidIncrement(),
                listing.getCurrentPrice(),
                currentBidder == null ? null : currentBidder.getId(),
                currentBidder == null ? null : currentBidder.getName(),
                listing.getBidCount(),
                listing.getStatus(),
                listing.getStartAt(),
                listing.getEndAt(),
                listing.getPickupLocation(),
                listing.getCreatedAt());
    }
}
