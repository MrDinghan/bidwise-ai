package com.bidwise.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Stripe (test-mode) {@link PaymentGateway}, active when {@code payment.gateway=stripe}.
 * Deposits are manual-capture PaymentIntents confirmed with a test payment method, so a
 * successful authorize leaves the intent in {@code requires_capture}. The idempotency
 * key is forwarded to Stripe so retries don't double-create.
 *
 * <p>The signature is verified with {@link Webhook#constructEvent}; the payment-intent id
 * is then read from the raw payload (avoids coupling to a specific API-version model
 * shape). The default gateway is {@link FakePaymentGateway}; this adapter is only wired
 * when test-mode keys are configured.
 */
@Component
@ConditionalOnProperty(name = "payment.gateway", havingValue = "stripe")
public class StripePaymentGateway implements PaymentGateway {

    private final String secretKey;
    private final String webhookSecret;
    private final String currency;
    private final ObjectMapper objectMapper;

    public StripePaymentGateway(
            @Value("${payment.stripe.secret-key}") String secretKey,
            @Value("${payment.stripe.webhook-secret}") String webhookSecret,
            @Value("${payment.currency:eur}") String currency,
            ObjectMapper objectMapper) {
        this.secretKey = secretKey;
        this.webhookSecret = webhookSecret;
        this.currency = currency;
        this.objectMapper = objectMapper;
    }

    @Override
    public Authorization authorize(String customerRef, BigDecimal amount, String idempotencyKey) {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(toMinorUnits(amount))
                .setCurrency(currency)
                .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL)
                .setConfirm(true)
                .setOffSession(true)
                .setPaymentMethod("pm_card_visa") // Stripe test payment method
                .addPaymentMethodType("card")
                .putMetadata("customerRef", customerRef)
                .build();
        // Pass the key per request (RequestOptions) rather than mutating the global
        // Stripe.apiKey static — thread-safe and avoids shared mutable state.
        RequestOptions options = requestOptions().toBuilder()
                .setIdempotencyKey(idempotencyKey)
                .build();
        try {
            PaymentIntent intent = PaymentIntent.create(params, options);
            return new Authorization(intent.getId());
        } catch (StripeException e) {
            throw new PaymentGatewayException("Stripe authorize failed", e);
        }
    }

    @Override
    public void capture(String paymentIntentId) {
        try {
            PaymentIntent.retrieve(paymentIntentId, requestOptions()).capture(requestOptions());
        } catch (StripeException e) {
            throw new PaymentGatewayException("Stripe capture failed", e);
        }
    }

    @Override
    public void release(String paymentIntentId) {
        try {
            PaymentIntent.retrieve(paymentIntentId, requestOptions()).cancel(requestOptions());
        } catch (StripeException e) {
            throw new PaymentGatewayException("Stripe release failed", e);
        }
    }

    private RequestOptions requestOptions() {
        return RequestOptions.builder().setApiKey(secretKey).build();
    }

    @Override
    public WebhookEvent verify(String payload, String signature) {
        try {
            Event event = Webhook.constructEvent(payload, signature, webhookSecret);
            JsonNode object = objectMapper.readTree(payload).path("data").path("object");
            return new WebhookEvent(event.getId(), event.getType(), object.path("id").asText(null));
        } catch (Exception e) {
            throw new PaymentGatewayException("Stripe webhook verification failed", e);
        }
    }

    private static long toMinorUnits(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
