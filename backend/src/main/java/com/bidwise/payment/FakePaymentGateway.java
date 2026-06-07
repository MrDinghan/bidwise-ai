package com.bidwise.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * In-memory {@link PaymentGateway} used by default ({@code payment.gateway=fake}) so the
 * whole pre-auth → capture/release flow runs with no external service — local dev and
 * the test suites depend on it. Holds are tracked in a map keyed by a deterministic
 * intent id ({@code pi_fake_<n>}); the idempotency key returns the same intent on retry.
 *
 * <p>Webhook payloads use the same JSON shape as Stripe
 * ({@code {"id","type","data":{"object":{"id"}}}}) so the parsing/dedup path is exercised
 * identically; a blank signature is rejected to mirror signature verification.
 */
@Component
@ConditionalOnProperty(name = "payment.gateway", havingValue = "fake", matchIfMissing = true)
public class FakePaymentGateway implements PaymentGateway {

    private final AtomicLong sequence = new AtomicLong();
    private final ConcurrentHashMap<String, PaymentHoldStatus> holds = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> intentsByIdempotencyKey = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public FakePaymentGateway(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Authorization authorize(String customerRef, BigDecimal amount, String idempotencyKey) {
        String intentId = intentsByIdempotencyKey.computeIfAbsent(
                idempotencyKey, key -> "pi_fake_" + sequence.incrementAndGet());
        holds.putIfAbsent(intentId, PaymentHoldStatus.AUTHORIZED);
        return new Authorization(intentId);
    }

    @Override
    public void capture(String paymentIntentId) {
        holds.put(paymentIntentId, PaymentHoldStatus.CAPTURED);
    }

    @Override
    public void release(String paymentIntentId) {
        holds.put(paymentIntentId, PaymentHoldStatus.VOIDED);
    }

    @Override
    public WebhookEvent verify(String payload, String signature) {
        if (!StringUtils.hasText(signature)) {
            throw new PaymentGatewayException("Missing webhook signature");
        }
        try {
            JsonNode root = objectMapper.readTree(payload);
            String eventId = root.path("id").asText(null);
            String type = root.path("type").asText(null);
            String intentId = root.path("data").path("object").path("id").asText(null);
            if (eventId == null || type == null) {
                throw new PaymentGatewayException("Malformed webhook payload");
            }
            return new WebhookEvent(eventId, type, intentId);
        } catch (PaymentGatewayException e) {
            throw e;
        } catch (Exception e) {
            throw new PaymentGatewayException("Could not parse webhook payload", e);
        }
    }
}
