package com.homely.rental.booking.controller;

import com.homely.rental.booking.dto.BookingActionRequest;
import com.homely.rental.booking.dto.BookingCreateRequest;
import com.homely.rental.booking.dto.BookingDTO;
import com.homely.rental.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import com.homely.rental.common.dto.PageResponse;
import com.homely.rental.common.exception.IdInvalidException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Booking controller (BOOK01–BOOK09).
 */
@RestController
@RequestMapping(path = "${apiPrefix}/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    @GetMapping("/{id}")
    public BookingDTO detail(@PathVariable Long id) throws IdInvalidException { return bookingService.getBooking(id); }
    @PostMapping("/{id}/cases")
    public ResponseEntity<com.homely.rental.booking.dto.BookingCaseDTO> openCase(@PathVariable Long id,
            @Valid @RequestBody com.homely.rental.booking.dto.CaseCreateRequest request) throws IdInvalidException {
        return ResponseEntity.status(201).body(bookingService.openCase(id, request));
    }
    @GetMapping("/{id}/cases")
    public java.util.List<com.homely.rental.booking.dto.BookingCaseDTO> cases(@PathVariable Long id) throws IdInvalidException {
        return bookingService.getCases(id);
    }

    // BOOK01: Create booking
    @PostMapping
    @Operation(summary = "Create booking request")
    public ResponseEntity<BookingDTO> createBooking(
            @Valid @RequestBody BookingCreateRequest request) throws IdInvalidException {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(request));
    }

    // BOOK02: My bookings (Tenant)
    @GetMapping("/my")
    @Operation(summary = "Get my bookings")
    public ResponseEntity<PageResponse<BookingDTO>> getMyBookings(Pageable pageable) throws IdInvalidException {
        return ResponseEntity.ok(bookingService.getTenantBookings(pageable));
    }

    // BOOK03: Host bookings
    @GetMapping("/host")
    @Operation(summary = "Get host bookings")
    public ResponseEntity<PageResponse<BookingDTO>> getHostBookings(Pageable pageable) throws IdInvalidException {
        return ResponseEntity.ok(bookingService.getHostBookings(pageable));
    }

    // BOOK04: Approve booking
    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve booking")
    public ResponseEntity<BookingDTO> approveBooking(@PathVariable Long id) throws IdInvalidException {
        return ResponseEntity.ok(bookingService.approveBooking(id));
    }

    // BOOK05: Reject booking
    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject booking")
    public ResponseEntity<BookingDTO> rejectBooking(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) BookingActionRequest request) throws IdInvalidException {
        return ResponseEntity.ok(bookingService.rejectBooking(id, request));
    }

    // BOOK06: Confirm deposit
    @PostMapping("/{id}/confirm-deposit")
    @Operation(summary = "Confirm deposit payment")
    public ResponseEntity<BookingDTO> confirmDeposit(@PathVariable Long id) throws IdInvalidException {
        return ResponseEntity.ok(bookingService.confirmDeposit(id));
    }

    // BOOK07: Tenant handover
    @PostMapping("/{id}/handover-tenant")
    @Operation(summary = "Tenant confirms handover")
    public ResponseEntity<BookingDTO> tenantHandover(@PathVariable Long id) throws IdInvalidException {
        return ResponseEntity.ok(bookingService.tenantHandover(id));
    }

    // BOOK08: Host handover
    @PostMapping("/{id}/handover-host")
    @Operation(summary = "Host confirms handover")
    public ResponseEntity<BookingDTO> hostHandover(@PathVariable Long id) throws IdInvalidException {
        return ResponseEntity.ok(bookingService.hostHandover(id));
    }

    // BOOK09: Cancel booking
    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel booking")
    public ResponseEntity<BookingDTO> cancelBooking(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) BookingActionRequest request) throws IdInvalidException {
        return ResponseEntity.ok(bookingService.cancelBooking(id, request));
    }
}
