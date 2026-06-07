package com.bidwise.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies and applies inbound payment webhook events, idempotently. The provider's
 * event id is recorded in {@link ProcessedWebhookEvent}; a redelivery of the same event
 * is detected and skipped, so reprocessing has no effect (no double-fulfillment).
 *
 * <p>Events reflect the provider's view of a hold back into our records — settlement
 * itself drives capture/release; the webhook is the async confirmation channel.
 */
@Service
public class WebhookService {

    private static final Logger LOG = LoggerFactory.getLogger(WebhookService.class);

    private final PaymentGateway gateway;
    private final PaymentHoldRepository holdRepository;
    private final ProcessedWebhookEventRepository processedRepository;

    public WebhookService(
            PaymentGateway gateway,
            PaymentHoldRepository holdRepository,
            ProcessedWebhookEventRepository processedRepository) {
        this.gateway = gateway;
        this.holdRepository = holdRepository;
        this.processedRepository = processedRepository;
    }

    /**
     * Verifies the signature, dedupes on the event id, then applies the event. Returns
     * {@code true} when the event was newly processed, {@code false} when it was a
     * duplicate (already seen) and therefore skipped.
     *
     * @throws PaymentGatewayException if the signature cannot be verified
     */
    @Transactional
    public boolean handle(String payload, String signature) {
        PaymentGateway.WebhookEvent event = gateway.verify(payload, signature);
        if (processedRepository.existsById(event.eventId())) {
            LOG.debug("Skipping duplicate webhook event {}", event.eventId());
            return false;
        }
        apply(event);
        processedRepository.save(new ProcessedWebhookEvent(event.eventId(), event.type()));
        return true;
    }

    private void apply(PaymentGateway.WebhookEvent event) {
        if (event.paymentIntentId() == null) {
            return;
        }
        holdRepository.findByStripePaymentIntentId(event.paymentIntentId()).ifPresent(hold -> {
            switch (event.type()) {
                case "payment_intent.succeeded" -> hold.capture();
                case "payment_intent.canceled" -> hold.voidHold();
                default -> LOG.debug("Ignoring webhook event type {}", event.type());
            }
        });
    }
}
