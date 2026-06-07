package com.bidwise.payment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Receives payment provider webhooks. The endpoint is public (no JWT) — the signature is
 * the authentication — so it is permitted in {@code SecurityConfig}. The raw body is
 * required for signature verification, so it is read as a {@code String}. Handling is
 * idempotent: a redelivered event is deduped (see {@link WebhookService}).
 */
@RestController
@RequestMapping("/api/webhooks/stripe")
@Tag(name = "webhooks", description = "Payment provider webhook receiver")
public class StripeWebhookController {

    private final WebhookService webhookService;

    public StripeWebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @Operation(operationId = "stripeWebhook", summary = "Receive a Stripe payment webhook")
    @PostMapping
    public ResponseEntity<Void> handle(
            @RequestBody String payload,
            @RequestHeader(name = "Stripe-Signature", required = false) String signature) {
        try {
            webhookService.handle(payload, signature);
            return ResponseEntity.ok().build();
        } catch (PaymentGatewayException e) {
            // A failed signature/parse is a client error; do not let Stripe retry forever.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
