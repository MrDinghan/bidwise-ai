package com.bidwise.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bidwise.listing.AuctionDuration;
import com.bidwise.listing.Category;
import com.bidwise.listing.ItemCondition;
import com.bidwise.listing.Listing;
import com.bidwise.user.Role;
import com.bidwise.user.User;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private PaymentGateway gateway;
    @Mock
    private PaymentHoldRepository holdRepository;
    @Mock
    private ProcessedWebhookEventRepository processedRepository;

    private WebhookService service;

    @BeforeEach
    void setUp() {
        service = new WebhookService(gateway, holdRepository, processedRepository);
    }

    private PaymentHold authorizedHold() {
        User buyer = new User("Buyer", "buyer@x.com", "hash", Role.USER);
        Listing listing = new Listing(
                buyer, "Lamp", Category.FURNITURE, "A lamp", ItemCondition.GOOD,
                List.of(), new BigDecimal("10.00"), new BigDecimal("1.00"), "Library",
                AuctionDuration.THREE_DAYS);
        return new PaymentHold(buyer, listing, "pi_1", new BigDecimal("1.00"), "hold:2:100");
    }

    @Test
    void newSucceededEventCapturesTheHold() {
        PaymentHold hold = authorizedHold();
        when(gateway.verify("body", "sig")).thenReturn(
                new PaymentGateway.WebhookEvent("evt_1", "payment_intent.succeeded", "pi_1"));
        when(processedRepository.existsById("evt_1")).thenReturn(false);
        when(holdRepository.findByStripePaymentIntentId("pi_1")).thenReturn(Optional.of(hold));

        boolean processed = service.handle("body", "sig");

        assertThat(processed).isTrue();
        assertThat(hold.getStatus()).isEqualTo(PaymentHoldStatus.CAPTURED);
        verify(processedRepository).save(any(ProcessedWebhookEvent.class));
    }

    @Test
    void duplicateEventIsSkipped() {
        when(gateway.verify("body", "sig")).thenReturn(
                new PaymentGateway.WebhookEvent("evt_1", "payment_intent.succeeded", "pi_1"));
        when(processedRepository.existsById("evt_1")).thenReturn(true);

        boolean processed = service.handle("body", "sig");

        assertThat(processed).isFalse();
        verify(holdRepository, never()).findByStripePaymentIntentId(any());
        verify(processedRepository, never()).save(any());
    }

    @Test
    void redeliveredEventTakesEffectOnlyOnce() {
        PaymentHold hold = authorizedHold();
        when(gateway.verify("body", "sig")).thenReturn(
                new PaymentGateway.WebhookEvent("evt_1", "payment_intent.succeeded", "pi_1"));
        // First delivery: not yet seen. Second delivery: already recorded.
        when(processedRepository.existsById("evt_1")).thenReturn(false, true);
        when(holdRepository.findByStripePaymentIntentId("pi_1")).thenReturn(Optional.of(hold));

        boolean first = service.handle("body", "sig");
        boolean second = service.handle("body", "sig");

        assertThat(first).isTrue();
        assertThat(second).isFalse();
        // The side effect (capture + record) happened exactly once.
        verify(holdRepository, times(1)).findByStripePaymentIntentId("pi_1");
        verify(processedRepository, times(1)).save(any(ProcessedWebhookEvent.class));
    }

    @Test
    void invalidSignaturePropagates() {
        when(gateway.verify("body", "bad"))
                .thenThrow(new PaymentGatewayException("bad signature"));

        assertThatThrownBy(() -> service.handle("body", "bad"))
                .isInstanceOf(PaymentGatewayException.class);
    }
}
