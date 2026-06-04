package com.bidwise.listing;

import com.bidwise.listing.dto.ListingSearchCriteria;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Builds JPA {@link Specification}s for the public listing search. Always constrains
 * to {@link ListingStatus#ACTIVE}; other filters are applied when present.
 */
public final class ListingSpecifications {

    private ListingSpecifications() {
    }

    public static Specification<Listing> matching(ListingSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), ListingStatus.ACTIVE));

            if (StringUtils.hasText(criteria.q())) {
                String like = "%" + criteria.q().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("description")), like)));
            }
            if (criteria.category() != null) {
                predicates.add(cb.equal(root.get("category"), criteria.category()));
            }
            if (criteria.condition() != null) {
                predicates.add(cb.equal(root.get("condition"), criteria.condition()));
            }
            if (criteria.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("currentPrice"), criteria.minPrice()));
            }
            if (criteria.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("currentPrice"), criteria.maxPrice()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
