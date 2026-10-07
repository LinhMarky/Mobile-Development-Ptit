package com.homely.rental.catalog.controller;

import com.homely.rental.catalog.dto.request.FeeInput;
import com.homely.rental.catalog.dto.request.ListingWriteRequest;
import com.homely.rental.catalog.dto.response.ListingDTO;
import com.homely.rental.catalog.service.ListingService;
import io.swagger.v3.oas.annotations.Operation;
import com.homely.rental.common.dto.PageResponse;
import com.homely.rental.common.exception.IdInvalidException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Listing controller (CAT01–CAT10).
 * Public endpoints for discovery and authenticated endpoints for host management.
 */
@RestController
@RequestMapping(path = "${apiPrefix}/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;

    // ===== Public endpoints (Discovery) =====

    // CAT05: Search listings
    @GetMapping
    @Operation(summary = "Search listings")
    public ResponseEntity<PageResponse<ListingDTO>> searchListings(
            @Valid @ModelAttribute com.homely.rental.catalog.dto.request.ListingSearchQuery query) {
        return ResponseEntity.ok(listingService.searchListings(query));
    }

    // CAT07: Get listing detail
    @GetMapping("/{id}")
    @Operation(summary = "Get listing detail")
    public ResponseEntity<ListingDTO> getListingDetail(@PathVariable Long id) {
        return ResponseEntity.ok(listingService.getListingDetail(id));
    }

    // ===== Host endpoints =====

    // CAT01: Create listing for a room
    @PostMapping("/room/{roomId}")
    @PreAuthorize("hasRole('ROLE_HOST')")
    @Operation(summary = "Create listing")
    public ResponseEntity<ListingDTO> createListing(@PathVariable Long roomId,
                                                     @Valid @RequestBody ListingWriteRequest dto) throws IdInvalidException {
        return ResponseEntity.status(HttpStatus.CREATED).body(listingService.createListing(roomId, dto));
    }

    // CAT02: Update listing
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_HOST')")
    @Operation(summary = "Update listing")
    public ResponseEntity<ListingDTO> updateListing(@PathVariable Long id,
                                                     @Valid @RequestBody ListingWriteRequest dto) throws IdInvalidException {
        return ResponseEntity.ok(listingService.updateListing(id, dto));
    }

    // CAT03: Submit listing for review
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('ROLE_HOST')")
    @Operation(summary = "Submit listing for review")
    public ResponseEntity<ListingDTO> submitForReview(@PathVariable Long id) throws IdInvalidException {
        return ResponseEntity.ok(listingService.submitForReview(id));
    }

    // CAT04: Hide listing
    @PostMapping("/{id}/hide")
    @PreAuthorize("hasRole('ROLE_HOST')")
    @Operation(summary = "Hide listing")
    public ResponseEntity<ListingDTO> hideListing(@PathVariable Long id) throws IdInvalidException {
        return ResponseEntity.ok(listingService.hideListing(id));
    }

    // Host: List own listings
    @GetMapping("/my")
    @PreAuthorize("hasRole('ROLE_HOST')")
    @Operation(summary = "List host listings")
    public ResponseEntity<PageResponse<ListingDTO>> getHostListings(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable)
            throws IdInvalidException {
        return ResponseEntity.ok(listingService.getHostListings(pageable));
    }

    // ===== Fee management (CAT08–10) =====

    // CAT08: Set fees for listing
    @PutMapping("/{id}/fees")
    @PreAuthorize("hasRole('ROLE_HOST')")
    @Operation(summary = "Set listing fees")
    public ResponseEntity<List<ListingDTO.FeeDTO>> setFees(@PathVariable Long id,
                                                            @Valid @RequestBody List<FeeInput> fees) throws IdInvalidException {
        return ResponseEntity.ok(listingService.setFees(id, fees));
    }

    // CAT09-10: Get fees for listing
    @GetMapping("/{id}/fees")
    @Operation(summary = "Get listing fees")
    public ResponseEntity<List<ListingDTO.FeeDTO>> getFees(@PathVariable Long id) {
        return ResponseEntity.ok(listingService.getFees(id));
    }
}
