package com.bidwise.listing;

import com.bidwise.listing.dto.CreateListingRequest;
import com.bidwise.listing.dto.ListingPageResponse;
import com.bidwise.listing.dto.ListingResponse;
import com.bidwise.listing.dto.ListingSearchCriteria;
import com.bidwise.listing.dto.UpdateListingRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Listing endpoints: create/edit/publish/delete (seller-owned) plus public
 * browse/search and detail.
 */
@RestController
@RequestMapping("/api/listings")
@Tag(name = "listings", description = "Create, browse, search, and manage auction listings")
public class ListingController {

    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @Operation(operationId = "createListing", summary = "Create a draft listing")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<ListingResponse> create(
            @Valid @RequestBody CreateListingRequest request, Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(listingService.create(principal.getName(), request));
    }

    @Operation(operationId = "searchListings", summary = "Browse and search active listings")
    @GetMapping
    public ResponseEntity<ListingPageResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) ItemCondition condition,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "NEWEST") ListingSort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var criteria = new ListingSearchCriteria(q, category, condition, minPrice, maxPrice);
        return ResponseEntity.ok(listingService.search(criteria, sort, page, size));
    }

    @Operation(operationId = "getMyListings", summary = "List the caller's own listings")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/mine")
    public ResponseEntity<ListingPageResponse> mine(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(listingService.mine(principal.getName(), page, size));
    }

    @Operation(operationId = "getListing", summary = "Get a single listing by id")
    @GetMapping("/{id}")
    public ResponseEntity<ListingResponse> get(@PathVariable Long id, Principal principal) {
        String email = principal == null ? null : principal.getName();
        return ResponseEntity.ok(listingService.get(id, email));
    }

    @Operation(operationId = "updateListing", summary = "Edit a listing you own")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}")
    public ResponseEntity<ListingResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateListingRequest request,
            Principal principal) {
        return ResponseEntity.ok(listingService.update(id, principal.getName(), request));
    }

    @Operation(operationId = "publishListing", summary = "Publish a draft listing (DRAFT -> ACTIVE)")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{id}/publish")
    public ResponseEntity<ListingResponse> publish(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(listingService.publish(id, principal.getName()));
    }

    @Operation(operationId = "deleteListing", summary = "Delete a draft or cancel an active listing")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
        listingService.delete(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
