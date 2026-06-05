package com.bidwise.listing;

import com.bidwise.user.User;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence for {@link Listing}. Extends {@link JpaSpecificationExecutor} so the
 * search endpoint can compose dynamic filters via {@link ListingSpecifications}.
 */
public interface ListingRepository
        extends JpaRepository<Listing, Long>, JpaSpecificationExecutor<Listing> {

    /** Active auctions whose time is up — fed to the scheduled auto-close job. */
    List<Listing> findByStatusAndEndAtLessThanEqual(ListingStatus status, Instant cutoff);

    /**
     * Atomically records a higher bid: advances the price, highest bidder, and bid
     * count in a single statement. The guard keeps the update monotonic so out-of-order
     * writes can never lower the price — while still admitting the opening bid, which
     * equals the start price ({@code currentPrice}) when no bids exist yet.
     * Returns the number of rows updated (1 when applied, 0 when the guard rejected).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Listing l
               SET l.currentPrice = :amount,
                   l.currentBidder = :bidder,
                   l.bidCount = l.bidCount + 1,
                   l.updatedAt = :now
             WHERE l.id = :id
               AND (l.bidCount = 0 OR l.currentPrice < :amount)""")
    int recordHigherBid(
            @Param("id") Long id,
            @Param("amount") BigDecimal amount,
            @Param("bidder") User bidder,
            @Param("now") Instant now);

    /** Extends the auction end time (anti-snipe). Monotonic: only ever pushes it later. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Listing l SET l.endAt = :newEnd WHERE l.id = :id AND l.endAt < :newEnd")
    int extendEndAt(@Param("id") Long id, @Param("newEnd") Instant newEnd);
}
