package com.homely.rental.admin.controller;

import com.homely.rental.admin.service.AdminService;
import com.homely.rental.booking.entity.BookingCaseDecision;
import io.swagger.v3.oas.annotations.Operation;
import com.homely.rental.common.exception.IdInvalidException;
import lombok.RequiredArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/**
 * Admin controller (ADMIN01–ADMIN04).
 * All endpoints require ROLE_ADMIN.
 */
@RestController
@RequestMapping(path = "${apiPrefix}/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/listings")
    public com.homely.rental.common.dto.PageResponse<com.homely.rental.catalog.dto.response.ListingDTO> pendingListings(org.springframework.data.domain.Pageable pageable) {
        return adminService.pendingListings(pageable);
    }

    @GetMapping("/cases")
    public com.homely.rental.common.dto.PageResponse<com.homely.rental.booking.dto.BookingCaseDTO> openCases(org.springframework.data.domain.Pageable pageable) {
        return adminService.openCases(pageable);
    }

    // ADMIN01: Approve listing
    @PostMapping("/listings/{id}/approve")
    @Operation(summary = "Listing approved")
    public ResponseEntity<Void> approveListing(@PathVariable @Positive Long id) throws IdInvalidException {
        adminService.approveListing(id);
        return ResponseEntity.ok().build();
    }

    // ADMIN02: Reject listing
    @PostMapping("/listings/{id}/reject")
    @Operation(summary = "Listing rejected")
    public ResponseEntity<Void> rejectListing(
            @PathVariable @Positive Long id,
            @RequestParam @NotBlank @Size(max = 500) String reason) throws IdInvalidException {
        adminService.rejectListing(id, reason);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/listings/{id}/suspend")
    public ResponseEntity<Void> suspendListing(@PathVariable @Positive Long id,
            @RequestParam @NotBlank @Size(max = 500) String reason) {
        adminService.suspendListing(id, reason);
        return ResponseEntity.ok().build();
    }

    // ADMIN03: Resolve booking case
    @PostMapping("/cases/{id}/resolve")
    @Operation(summary = "Booking case resolved")
    public ResponseEntity<Void> resolveBookingCase(
            @PathVariable @Positive Long id,
            @RequestParam BookingCaseDecision decision,
            @RequestParam @NotBlank @Size(max = 2000) String note,
            @RequestParam(name="tenant_refund_vnd", required=false) java.math.BigDecimal tenantRefund) throws IdInvalidException {
        adminService.resolveBookingCase(id, decision, note, tenantRefund);
        return ResponseEntity.ok().build();
    }

    // ADMIN04: Suspend user
    @PostMapping("/users/{id}/suspend")
    @Operation(summary = "User suspended")
    public ResponseEntity<Void> suspendUser(
            @PathVariable @Positive Long id,
            @RequestParam @NotBlank @Size(max = 500) String reason) throws IdInvalidException {
        adminService.suspendUser(id, reason);
        return ResponseEntity.ok().build();
    }

    // ADMIN: Unsuspend user
    @PostMapping("/users/{id}/unsuspend")
    @Operation(summary = "User unsuspended")
    public ResponseEntity<Void> unsuspendUser(@PathVariable @Positive Long id) throws IdInvalidException {
        adminService.unsuspendUser(id);
        return ResponseEntity.ok().build();
    }

    // ADMIN: Resolve report
    @PostMapping("/reports/{id}/resolve")
    @Operation(summary = "Report resolved")
    public ResponseEntity<Void> resolveReport(
            @PathVariable @Positive Long id,
            @RequestParam @NotBlank @Size(max = 1000) String note) throws IdInvalidException {
        adminService.resolveReport(id, note);
        return ResponseEntity.ok().build();
    }

    // ADMIN: Hide review
    @PostMapping("/reviews/{id}/hide")
    @Operation(summary = "Review hidden")
    public ResponseEntity<Void> hideReview(
            @PathVariable @Positive Long id,
            @RequestParam @NotBlank @Size(max = 500) String reason) throws IdInvalidException {
        adminService.hideReview(id, reason);
        return ResponseEntity.ok().build();
    }
}
