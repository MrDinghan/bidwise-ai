package com.bidwise.payment;

import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for auction settlements (one per listing). */
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    /** Whether the listing has already been settled — the idempotency guard. */
    boolean existsByListingId(Long listingId);
}
