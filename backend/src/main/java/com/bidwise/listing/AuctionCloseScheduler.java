package com.bidwise.listing;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically triggers {@link AuctionCloseService} to end auctions whose time is up.
 * Disabled in the test profile ({@code auction.close.enabled=false}) so the H2 test
 * suites never reach Redis; the close logic is exercised directly instead.
 *
 * <p>Single-instance assumption for now; horizontal scaling would need a distributed
 * lock (e.g. ShedLock) so only one node runs the sweep.
 */
@Component
@ConditionalOnProperty(name = "auction.close.enabled", havingValue = "true", matchIfMissing = true)
public class AuctionCloseScheduler {

    private final AuctionCloseService auctionCloseService;

    public AuctionCloseScheduler(AuctionCloseService auctionCloseService) {
        this.auctionCloseService = auctionCloseService;
    }

    @Scheduled(fixedDelayString = "${auction.close.poll-ms:15000}")
    public void sweep() {
        auctionCloseService.closeDueAuctions();
    }
}
