package com.bidwise.bid;

import com.bidwise.bid.dto.BidPageResponse;
import com.bidwise.bid.dto.BidResponse;
import com.bidwise.bid.dto.PlaceBidRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bidding endpoints: place a bid (authenticated) and read a listing's public bid
 * history. Live updates are pushed separately over the STOMP topic for the listing.
 */
@RestController
@RequestMapping("/api/listings/{listingId}/bids")
@Tag(name = "bids", description = "Place bids and read a listing's bid history")
public class BidController {

    private final BidService bidService;

    public BidController(BidService bidService) {
        this.bidService = bidService;
    }

    @Operation(operationId = "placeBid", summary = "Place a bid on an active auction")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<BidResponse> placeBid(
            @PathVariable Long listingId,
            @Valid @RequestBody PlaceBidRequest request,
            Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bidService.placeBid(listingId, principal.getName(), request.amount()));
    }

    @Operation(operationId = "getBids", summary = "Read a listing's bid history (newest first)")
    @GetMapping
    public ResponseEntity<BidPageResponse> getBids(
            @PathVariable Long listingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(bidService.history(listingId, page, size));
    }
}
