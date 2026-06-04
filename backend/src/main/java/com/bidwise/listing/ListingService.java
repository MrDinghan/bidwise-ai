package com.bidwise.listing;

import com.bidwise.listing.dto.CreateListingRequest;
import com.bidwise.listing.dto.ListingPageResponse;
import com.bidwise.listing.dto.ListingResponse;
import com.bidwise.listing.dto.ListingSearchCriteria;
import com.bidwise.listing.dto.UpdateListingRequest;
import com.bidwise.user.User;
import com.bidwise.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listing lifecycle and search. Sellers own their listings; ownership is checked on
 * every mutating operation. The public search only returns {@code ACTIVE} listings.
 */
@Service
public class ListingService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    public ListingService(ListingRepository listingRepository, UserRepository userRepository) {
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ListingResponse create(String sellerEmail, CreateListingRequest request) {
        User seller = requireUser(sellerEmail);
        Listing listing = new Listing(
                seller,
                request.title(),
                request.category(),
                request.description(),
                request.condition(),
                request.photos(),
                request.startPrice(),
                request.bidIncrement(),
                request.pickupLocation(),
                request.endAt());
        return ListingResponse.from(listingRepository.save(listing));
    }

    @Transactional(readOnly = true)
    public ListingResponse get(Long id, String requesterEmail) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ListingNotFoundException(id));
        // Drafts are visible only to their owner; everyone else gets a 404.
        if (listing.getStatus() == ListingStatus.DRAFT && !ownedBy(listing, requesterEmail)) {
            throw new ListingNotFoundException(id);
        }
        return ListingResponse.from(listing);
    }

    @Transactional
    public ListingResponse update(Long id, String requesterEmail, UpdateListingRequest request) {
        Listing listing = requireOwned(id, requesterEmail);
        listing.applyEdits(
                request.title(),
                request.category(),
                request.description(),
                request.condition(),
                request.photos(),
                request.startPrice(),
                request.bidIncrement(),
                request.pickupLocation(),
                request.endAt());
        return ListingResponse.from(listing);
    }

    @Transactional
    public ListingResponse publish(Long id, String requesterEmail) {
        Listing listing = requireOwned(id, requesterEmail);
        listing.publish();
        return ListingResponse.from(listing);
    }

    @Transactional
    public void delete(Long id, String requesterEmail) {
        Listing listing = requireOwned(id, requesterEmail);
        if (listing.getStatus() == ListingStatus.DRAFT) {
            listingRepository.delete(listing);
        } else {
            listing.cancel();
        }
    }

    @Transactional(readOnly = true)
    public ListingPageResponse search(
            ListingSearchCriteria criteria, ListingSort sort, int page, int size) {
        Pageable pageable = PageRequest.of(clampPage(page), clampSize(size), sort.toSort());
        Page<Listing> result =
                listingRepository.findAll(ListingSpecifications.matching(criteria), pageable);
        return ListingPageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public ListingPageResponse mine(String sellerEmail, int page, int size) {
        User seller = requireUser(sellerEmail);
        Pageable pageable = PageRequest.of(
                clampPage(page), clampSize(size), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Listing> result = listingRepository.findAll(
                (root, query, cb) -> cb.equal(root.get("seller"), seller), pageable);
        return ListingPageResponse.from(result);
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(ListingAccessDeniedException::new);
    }

    private Listing requireOwned(Long id, String email) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ListingNotFoundException(id));
        if (!ownedBy(listing, email)) {
            throw new ListingAccessDeniedException();
        }
        return listing;
    }

    private boolean ownedBy(Listing listing, String email) {
        return email != null && listing.getSeller().getEmail().equals(email);
    }

    private int clampPage(int page) {
        return Math.max(page, 0);
    }

    private int clampSize(int size) {
        if (size < 1) {
            return 1;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
