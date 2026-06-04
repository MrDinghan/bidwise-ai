package com.bidwise.listing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Persistence for {@link Listing}. Extends {@link JpaSpecificationExecutor} so the
 * search endpoint can compose dynamic filters via {@link ListingSpecifications}.
 */
public interface ListingRepository
        extends JpaRepository<Listing, Long>, JpaSpecificationExecutor<Listing> {
}
