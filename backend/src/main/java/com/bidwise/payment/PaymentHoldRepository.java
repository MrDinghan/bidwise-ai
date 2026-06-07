package com.bidwise.payment;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for deposit holds. */
public interface PaymentHoldRepository extends JpaRepository<PaymentHold, Long> {

    /** The (at most one) hold a user holds on a listing. */
    Optional<PaymentHold> findByUserIdAndListingId(Long userId, Long listingId);

    /** Whether the user holds a deposit in the given status on the listing. */
    boolean existsByUserIdAndListingIdAndStatus(
            Long userId, Long listingId, PaymentHoldStatus status);

    /** All holds on a listing in the given status (e.g. AUTHORIZED holds to settle). */
    List<PaymentHold> findByListingIdAndStatus(Long listingId, PaymentHoldStatus status);

    /** Lookup by the provider's intent id, used when fulfilling webhook events. */
    Optional<PaymentHold> findByStripePaymentIntentId(String stripePaymentIntentId);
}
