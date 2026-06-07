package com.bidwise.payment;

import com.bidwise.bid.AuctionNotActiveException;
import com.bidwise.bid.InvalidBidException;
import com.bidwise.listing.Listing;
import com.bidwise.listing.ListingAccessDeniedException;
import com.bidwise.listing.ListingNotFoundException;
import com.bidwise.listing.ListingRepository;
import com.bidwise.listing.ListingStatus;
import com.bidwise.payment.dto.PaymentHoldResponse;
import com.bidwise.user.User;
import com.bidwise.user.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authorizes and reads a buyer's deposit hold for an auction. A hold must exist before
 * the buyer may bid (enforced in {@code BidService}). The deposit amount is computed
 * server-side as a fraction of the listing start price — the client never supplies it.
 *
 * <p>Authorization is idempotent: a stable key per (buyer, listing) plus the unique
 * constraint means a repeated call returns the existing {@code AUTHORIZED} hold rather
 * than creating or charging a second one.
 */
@Service
public class DepositService {

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final PaymentHoldRepository holdRepository;
    private final PaymentGateway gateway;
    private final BigDecimal depositFraction;

    public DepositService(
            ListingRepository listingRepository,
            UserRepository userRepository,
            PaymentHoldRepository holdRepository,
            PaymentGateway gateway,
            @Value("${payment.deposit.fraction:0.10}") BigDecimal depositFraction) {
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
        this.holdRepository = holdRepository;
        this.gateway = gateway;
        this.depositFraction = depositFraction;
    }

    /**
     * Authorizes (or returns the existing) deposit hold for the buyer on the listing.
     * The auction must be {@code ACTIVE} and the buyer may not be the seller.
     */
    @Transactional
    public PaymentHoldResponse authorize(Long listingId, String buyerEmail) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ListingNotFoundException(listingId));
        User buyer = userRepository.findByEmail(buyerEmail)
                .orElseThrow(ListingAccessDeniedException::new);

        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new AuctionNotActiveException("This auction is not open for deposits");
        }
        if (listing.isOwnedBy(buyer.getId())) {
            throw new InvalidBidException("You cannot place a deposit on your own listing");
        }

        // Idempotent: reuse an existing hold for this (buyer, listing) if present.
        Optional<PaymentHold> existing =
                holdRepository.findByUserIdAndListingId(buyer.getId(), listingId);
        if (existing.isPresent()) {
            return PaymentHoldResponse.from(existing.get());
        }

        BigDecimal amount = depositFor(listing);
        String idempotencyKey = "hold:" + buyer.getId() + ":" + listingId;
        PaymentGateway.Authorization auth =
                gateway.authorize(buyer.getEmail(), amount, idempotencyKey);
        PaymentHold hold = holdRepository.save(
                new PaymentHold(buyer, listing, auth.paymentIntentId(), amount, idempotencyKey));
        return PaymentHoldResponse.from(hold);
    }

    /** Returns the buyer's current hold on the listing, or empty if none exists. */
    @Transactional(readOnly = true)
    public Optional<PaymentHoldResponse> find(Long listingId, String buyerEmail) {
        User buyer = userRepository.findByEmail(buyerEmail)
                .orElseThrow(ListingAccessDeniedException::new);
        return holdRepository.findByUserIdAndListingId(buyer.getId(), listingId)
                .map(PaymentHoldResponse::from);
    }

    /** Server-authoritative deposit: a fraction of the start price, rounded to cents. */
    private BigDecimal depositFor(Listing listing) {
        return listing.getStartPrice()
                .multiply(depositFraction)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
