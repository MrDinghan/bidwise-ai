package com.bidwise.listing;

import org.springframework.data.domain.Sort;

/**
 * Sort options for the public listing search, decoupled from entity column names.
 */
public enum ListingSort {
    NEWEST(Sort.by(Sort.Direction.DESC, "createdAt")),
    PRICE_ASC(Sort.by(Sort.Direction.ASC, "currentPrice")),
    PRICE_DESC(Sort.by(Sort.Direction.DESC, "currentPrice")),
    ENDING_SOON(Sort.by(Sort.Direction.ASC, "endAt"));

    private final Sort sort;

    ListingSort(Sort sort) {
        this.sort = sort;
    }

    public Sort toSort() {
        return sort;
    }
}
