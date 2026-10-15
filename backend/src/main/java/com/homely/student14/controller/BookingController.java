package com.homely.student14.controller;

import com.homely.student14.dto.ApiResponse;
import com.homely.student14.dto.request.booking.BookingActionDTO;
import com.homely.student14.dto.request.booking.CreateBookingDTO;
import com.homely.student14.dto.request.booking.CreatePaymentDTO;
import com.homely.student14.dto.request.booking.CreateReviewDTO;
import com.homely.student14.dto.request.booking.SimulatePaymentDTO;
import com.homely.student14.repository.DatabaseRepository;
import com.homely.student14.service.BookingService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;

    @GetMapping("/bookings/my")
    public ApiResponse<?> myBookings(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(DatabaseRepository.page(bookingService.list(jwt, false)));
    }

    @GetMapping("/bookings/host")
    public ApiResponse<?> hostBookings(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(DatabaseRepository.page(bookingService.list(jwt, true)));
    }

    @GetMapping("/bookings/{id}")
    public ApiResponse<?> booking(@PathVariable long id, @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(bookingService.get(id, jwt));
    }

    @PostMapping("/bookings")
    public ApiResponse<?> create(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateBookingDTO request) {
        return ApiResponse.ok(bookingService.create(jwt, request));
    }

    @PostMapping("/bookings/{id}/approve")
    public ApiResponse<?> approve(@PathVariable long id, @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(bookingService.approve(id, jwt));
    }

    @PostMapping("/bookings/{id}/reject")
    public ApiResponse<?> reject(
            @PathVariable long id,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BookingActionDTO request) {
        return ApiResponse.ok(bookingService.reject(id, jwt, request));
    }

    @PostMapping("/bookings/{id}/cancel")
    public ApiResponse<?> cancel(
            @PathVariable long id,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BookingActionDTO request) {
        return ApiResponse.ok(bookingService.cancel(id, jwt, request));
    }

    @PostMapping("/bookings/{id}/confirm-deposit")
    public ApiResponse<?> confirmDeposit(
            @PathVariable long id,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BookingActionDTO request) {
        return ApiResponse.ok(bookingService.confirmDeposit(id, jwt, request));
    }

    @PostMapping("/bookings/{id}/handover-tenant")
    public ApiResponse<?> tenantHandover(@PathVariable long id, @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(bookingService.confirmHandover(id, jwt, false));
    }

    @PostMapping("/bookings/{id}/handover-host")
    public ApiResponse<?> hostHandover(@PathVariable long id, @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(bookingService.confirmHandover(id, jwt, true));
    }

    @PostMapping("/payments")
    public ApiResponse<?> payment(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreatePaymentDTO request) {
        return ApiResponse.ok(bookingService.payment(jwt, request));
    }

    @GetMapping("/payments/booking/{id}")
    public ApiResponse<?> payments(@PathVariable long id, @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(DatabaseRepository.page(bookingService.payments(id, jwt)));
    }

    @PostMapping("/payments/{id}/simulate")
    public ApiResponse<?> simulate(
            @PathVariable long id,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SimulatePaymentDTO request) {
        return ApiResponse.ok(bookingService.simulate(id, jwt, request));
    }

    @PostMapping("/reviews")
    public ApiResponse<?> review(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateReviewDTO request) {
        return ApiResponse.ok(bookingService.review(jwt, request));
    }
}
