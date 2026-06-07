package com.bidwise.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bidwise.bid.AuctionNotActiveException;
import com.bidwise.bid.InvalidBidException;
import com.bidwise.listing.AuctionDuration;
import com.bidwise.listing.Category;
import com.bidwise.listing.ItemCondition;
import com.bidwise.listing.Listing;
import com.bidwise.listing.ListingRepository;
import com.bidwise.payment.dto.PaymentHoldResponse;
import com.bidwise.user.Role;
import com.bidwise.user.User;
import com.bidwise.user.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DepositServiceTest {

    private static final String SELLER = "seller@example.com";
    private static final String BUYER = "buyer@example.com";

    @Mock
    private ListingRepository listingRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PaymentHoldRepository holdRepository;
    @Mock
    private PaymentGateway gateway;

    private DepositService service;

    private User seller;
    private User buyer;

    @BeforeEach
    void setUp() {
        service = new DepositService(
                listingRepository, userRepository, holdRepository, gateway, new BigDecimal("0.10"));
        seller = userWithId("Seller", SELLER, 1L);
        buyer = userWithId("Buyer", BUYER, 2L);
    }

    private User userWithId(String name, String email, long id) {
        User user = new User(name, email, "hash", Role.USER);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    /** An ACTIVE listing owned by {@link #seller}, start price 10.00. */
    private Listing activeListing() {
        Listing listing = new Listing(
                seller, "Lamp", Category.FURNITURE, "A lamp", ItemCondition.GOOD,
                List.of(), new BigDecimal("10.00"), new BigDecimal("1.00"), "Library",
                AuctionDuration.THREE_DAYS);
        listing.publish();
        ReflectionTestUtils.setField(listing, "id", 100L);
        return listing;
    }

    @Test
    void authorizeComputesDepositAsFractionOfStartPrice() {
        when(listingRepository.findById(100L)).thenReturn(Optional.of(activeListing()));
        when(userRepository.findByEmail(BUYER)).thenReturn(Optional.of(buyer));
        when(holdRepository.findByUserIdAndListingId(2L, 100L)).thenReturn(Optional.empty());
        when(gateway.authorize(eq(BUYER), any(), eq("hold:2:100")))
                .thenReturn(new PaymentGateway.Authorization("pi_x"));
        when(holdRepository.save(any(PaymentHold.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentHoldResponse response = service.authorize(100L, BUYER);

        // 10.00 * 0.10 = 1.00
        assertThat(response.amount()).isEqualByComparingTo("1.00");
        assertThat(response.status()).isEqualTo(PaymentHoldStatus.AUTHORIZED);
        verify(gateway).authorize(eq(BUYER), eq(new BigDecimal("1.00")), eq("hold:2:100"));
    }

    @Test
    void authorizeIsIdempotentWhenAHoldAlreadyExists() {
        Listing listing = activeListing();
        PaymentHold existing =
                new PaymentHold(buyer, listing, "pi_existing", new BigDecimal("1.00"), "hold:2:100");
        when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));
        when(userRepository.findByEmail(BUYER)).thenReturn(Optional.of(buyer));
        when(holdRepository.findByUserIdAndListingId(2L, 100L)).thenReturn(Optional.of(existing));

        PaymentHoldResponse response = service.authorize(100L, BUYER);

        assertThat(response.amount()).isEqualByComparingTo("1.00");
        verify(gateway, never()).authorize(any(), any(), any());
        verify(holdRepository, never()).save(any());
    }

    @Test
    void sellerCannotDepositOnOwnListing() {
        when(listingRepository.findById(100L)).thenReturn(Optional.of(activeListing()));
        when(userRepository.findByEmail(SELLER)).thenReturn(Optional.of(seller));

        assertThatThrownBy(() -> service.authorize(100L, SELLER))
                .isInstanceOf(InvalidBidException.class);
        verify(gateway, never()).authorize(any(), any(), any());
    }

    @Test
    void depositOnANonActiveListingIsRejected() {
        Listing draft = new Listing(
                seller, "Lamp", Category.FURNITURE, "A lamp", ItemCondition.GOOD,
                List.of(), new BigDecimal("10.00"), new BigDecimal("1.00"), "Library",
                AuctionDuration.THREE_DAYS); // still DRAFT (not published)
        ReflectionTestUtils.setField(draft, "id", 100L);
        when(listingRepository.findById(100L)).thenReturn(Optional.of(draft));
        when(userRepository.findByEmail(BUYER)).thenReturn(Optional.of(buyer));

        assertThatThrownBy(() -> service.authorize(100L, BUYER))
                .isInstanceOf(AuctionNotActiveException.class);
    }
}
