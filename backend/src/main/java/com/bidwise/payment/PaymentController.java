package com.bidwise.payment;

import com.bidwise.payment.dto.PaymentHoldResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Deposit endpoints: authorize a deposit hold for the current user on an auction (a
 * prerequisite for bidding) and read that hold's status. The amount is computed
 * server-side; clients never send it.
 */
@RestController
@RequestMapping("/api/listings/{listingId}/deposit")
@Tag(name = "deposits", description = "Authorize and read the bidding deposit hold")
public class PaymentController {

    private final DepositService depositService;

    public PaymentController(DepositService depositService) {
        this.depositService = depositService;
    }

    @Operation(operationId = "placeDeposit", summary = "Authorize a deposit hold to enable bidding")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<PaymentHoldResponse> placeDeposit(
            @PathVariable Long listingId, Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(depositService.authorize(listingId, principal.getName()));
    }

    @Operation(operationId = "getDeposit", summary = "Read the current user's deposit hold")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<PaymentHoldResponse> getDeposit(
            @PathVariable Long listingId, Principal principal) {
        return depositService.find(listingId, principal.getName())
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No deposit hold on this listing"));
    }
}
