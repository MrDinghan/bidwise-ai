package com.bidwise.listing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bidwise.listing.dto.CreateListingRequest;
import com.bidwise.listing.dto.ListingResponse;
import com.bidwise.listing.dto.UpdateListingRequest;
import com.bidwise.user.Role;
import com.bidwise.user.User;
import com.bidwise.user.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListingServiceTest {

    private static final String OWNER = "alice@example.com";
    private static final String OTHER = "bob@example.com";

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ListingService service;

    private final User seller = new User("Alice", OWNER, "hash", Role.USER);

    private CreateListingRequest createRequest() {
        return new CreateListingRequest(
                "Desk lamp",
                Category.FURNITURE,
                "A barely used desk lamp",
                ItemCondition.GOOD,
                List.of("https://img.example/1.jpg"),
                new BigDecimal("10.00"),
                new BigDecimal("1.00"),
                "Campus library",
                AuctionDuration.THREE_DAYS);
    }

    private Listing draftListing() {
        var req = createRequest();
        return new Listing(
                seller, req.title(), req.category(), req.description(), req.condition(),
                req.photos(), req.startPrice(), req.bidIncrement(), req.pickupLocation(),
                req.duration());
    }

    @Test
    void createStartsAsDraftWithCurrentPriceEqualToStartPrice() {
        when(userRepository.findByEmail(OWNER)).thenReturn(Optional.of(seller));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));

        ListingResponse response = service.create(OWNER, createRequest());

        assertThat(response.status()).isEqualTo(ListingStatus.DRAFT);
        assertThat(response.currentPrice()).isEqualByComparingTo("10.00");
        assertThat(response.title()).isEqualTo("Desk lamp");
        assertThat(response.startAt()).isNull();
    }

    @Test
    void publishTransitionsDraftToActiveAndOpensWindow() {
        when(listingRepository.findById(1L)).thenReturn(Optional.of(draftListing()));

        ListingResponse response = service.publish(1L, OWNER);

        assertThat(response.status()).isEqualTo(ListingStatus.ACTIVE);
        assertThat(response.startAt()).isNotNull();
    }

    @Test
    void publishByNonOwnerIsForbidden() {
        when(listingRepository.findById(1L)).thenReturn(Optional.of(draftListing()));

        assertThatThrownBy(() -> service.publish(1L, OTHER))
                .isInstanceOf(ListingAccessDeniedException.class);
    }

    @Test
    void publishingAnAlreadyActiveListingIsRejected() {
        Listing listing = draftListing();
        listing.publish(); // now ACTIVE
        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> service.publish(1L, OWNER))
                .isInstanceOf(IllegalListingStateException.class);
    }

    @Test
    void updateByNonOwnerIsForbidden() {
        when(listingRepository.findById(1L)).thenReturn(Optional.of(draftListing()));
        var update = new UpdateListingRequest(
                "Hacked", Category.OTHER, "x", ItemCondition.POOR, List.of(),
                new BigDecimal("1.00"), new BigDecimal("1.00"), null, AuctionDuration.ONE_DAY);

        assertThatThrownBy(() -> service.update(1L, OTHER, update))
                .isInstanceOf(ListingAccessDeniedException.class);
    }

    @Test
    void draftIsNotVisibleToNonOwner() {
        when(listingRepository.findById(1L)).thenReturn(Optional.of(draftListing()));

        assertThatThrownBy(() -> service.get(1L, OTHER))
                .isInstanceOf(ListingNotFoundException.class);
    }

    @Test
    void draftIsVisibleToItsOwner() {
        when(listingRepository.findById(1L)).thenReturn(Optional.of(draftListing()));

        ListingResponse response = service.get(1L, OWNER);

        assertThat(response.status()).isEqualTo(ListingStatus.DRAFT);
    }

    @Test
    void deletingADraftRemovesItRatherThanCancelling() {
        Listing draft = draftListing();
        when(listingRepository.findById(1L)).thenReturn(Optional.of(draft));

        service.delete(1L, OWNER);

        verify(listingRepository).delete(draft);
    }

    @Test
    void deletingAnActiveListingCancelsItWithoutDeleting() {
        Listing listing = draftListing();
        listing.publish();
        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));

        service.delete(1L, OWNER);

        assertThat(listing.getStatus()).isEqualTo(ListingStatus.CLOSED);
        verify(listingRepository, never()).delete(any(Listing.class));
    }
}
