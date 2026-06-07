package com.bidwise.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FakePaymentGatewayTest {

    private FakePaymentGateway gateway;

    @BeforeEach
    void setUp() {
        gateway = new FakePaymentGateway(new ObjectMapper());
    }

    @Test
    void authorizeReturnsADeterministicIntentId() {
        PaymentGateway.Authorization auth =
                gateway.authorize("buyer@example.com", new BigDecimal("5.00"), "hold:2:100");

        assertThat(auth.paymentIntentId()).startsWith("pi_fake_");
    }

    @Test
    void sameIdempotencyKeyReturnsTheSameIntent() {
        PaymentGateway.Authorization first =
                gateway.authorize("buyer@example.com", new BigDecimal("5.00"), "hold:2:100");
        PaymentGateway.Authorization retry =
                gateway.authorize("buyer@example.com", new BigDecimal("5.00"), "hold:2:100");

        assertThat(retry.paymentIntentId()).isEqualTo(first.paymentIntentId());
    }

    @Test
    void differentKeysGetDistinctIntents() {
        String a = gateway.authorize("x", new BigDecimal("1.00"), "hold:1:1").paymentIntentId();
        String b = gateway.authorize("y", new BigDecimal("1.00"), "hold:2:2").paymentIntentId();

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void verifyParsesAStripeShapedPayload() {
        String payload = """
                {"id":"evt_1","type":"payment_intent.succeeded",
                 "data":{"object":{"id":"pi_fake_1"}}}""";

        PaymentGateway.WebhookEvent event = gateway.verify(payload, "sig");

        assertThat(event.eventId()).isEqualTo("evt_1");
        assertThat(event.type()).isEqualTo("payment_intent.succeeded");
        assertThat(event.paymentIntentId()).isEqualTo("pi_fake_1");
    }

    @Test
    void verifyRejectsABlankSignature() {
        assertThatThrownBy(() -> gateway.verify("{\"id\":\"e\",\"type\":\"t\"}", "  "))
                .isInstanceOf(PaymentGatewayException.class);
    }

    @Test
    void verifyRejectsAMalformedPayload() {
        assertThatThrownBy(() -> gateway.verify("not json", "sig"))
                .isInstanceOf(PaymentGatewayException.class);
    }
}
