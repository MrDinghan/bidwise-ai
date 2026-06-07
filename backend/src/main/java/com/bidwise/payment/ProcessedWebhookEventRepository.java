package com.bidwise.payment;

import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for the webhook dedup ledger. */
public interface ProcessedWebhookEventRepository
        extends JpaRepository<ProcessedWebhookEvent, String> {
}
