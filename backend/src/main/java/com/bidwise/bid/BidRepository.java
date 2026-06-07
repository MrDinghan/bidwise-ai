package com.bidwise.bid;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence for {@link Bid}. Bids are append-only history for a listing.
 */
public interface BidRepository extends JpaRepository<Bid, Long> {

    /** A page of a listing's bids, newest (highest) first. */
    Page<Bid> findByListingIdOrderByAmountDesc(Long listingId, Pageable pageable);

    /** The highest bid on a listing — the winning bid once the auction has ended. */
    Optional<Bid> findFirstByListingIdOrderByAmountDesc(Long listingId);

    /** Total number of bids placed on a listing. */
    long countByListingId(Long listingId);
}
