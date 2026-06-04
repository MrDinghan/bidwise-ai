package com.bidwise.listing;

/**
 * Raised when a listing cannot be found, or a draft is requested by someone other
 * than its owner (drafts are invisible to non-owners). Maps to HTTP 404.
 */
public class ListingNotFoundException extends RuntimeException {

    public ListingNotFoundException(Long id) {
        super("Listing not found: " + id);
    }
}
