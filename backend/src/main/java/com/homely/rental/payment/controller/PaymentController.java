package com.homely.rental.payment.controller;

import io.swagger.v3.oas.annotations.Operation;
import com.homely.rental.common.exception.IdInvalidException;
import com.homely.rental.payment.dto.PaymentCreateRequest;
import com.homely.rental.payment.dto.PaymentDTO;
import com.homely.rental.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Payment controller (PAY01).
 */
@RestController
@RequestMapping(path = "${apiPrefix}/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // PAY01: Create mock payment link
    @PostMapping
    @Operation(summary = "Create payment for booking")
    public ResponseEntity<PaymentDTO> createPayment(
            @Valid @RequestBody PaymentCreateRequest request) throws IdInvalidException {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.createPayment(request));
    }

    // Get payment info
    @GetMapping("/booking/{bookingId}")
    @Operation(summary = "Get payment info for booking")
    public ResponseEntity<PaymentDTO> getPaymentInfo(@PathVariable Long bookingId) throws IdInvalidException {
        return ResponseEntity.ok(paymentService.getPaymentInfo(bookingId));
    }
}
