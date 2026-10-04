package com.homely.rental.payment.service;

import com.homely.rental.common.exception.IdInvalidException;
import com.homely.rental.common.exception.ResourceNotFoundException;
import com.homely.rental.payment.entity.Payment;
import com.homely.rental.payment.entity.Refund;
import com.homely.rental.payment.entity.RefundReason;
import com.homely.rental.payment.entity.RefundStatus;
import com.homely.rental.payment.repository.PaymentRepository;
import com.homely.rental.payment.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Refund service. 
 * Handles generating mock refunds for cancelled/expired bookings.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public void processRefund(Long paymentId, RefundReason reason) throws IdInvalidException {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
        refund(payment, reason, payment.getAmountVnd());
    }

    /** Caller holds the booking/room lock; sandbox records the actual refunded amount. */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void refund(Payment payment, RefundReason reason, java.math.BigDecimal amount) {
        Long paymentId = payment.getId();

        if (payment.getStatus() != com.homely.rental.payment.entity.PaymentStatus.SUCCEEDED) {
            throw new IllegalArgumentException("Only successful payments can be refunded");
        }
        if (amount == null || amount.signum() < 0 || amount.scale() > 0 || amount.compareTo(payment.getAmountVnd()) > 0)
            throw new IllegalArgumentException("Invalid refund amount");

        // Check if refund already exists
        var existing = refundRepository.findByPaymentId(paymentId);
        if (existing.isPresent()) {
            if (existing.get().getAmountVnd().compareTo(amount) != 0) throw new com.homely.rental.common.exception.ConflictException("REFUND_CONFLICT", "Payment already refunded with another amount");
            log.info("Refund already initiated for payment {}", paymentId);
            return;
        }

        Refund refund = new Refund();
        refund.setPayment(payment);
        // Mock provider refund ID
        refund.setProviderRefundId("REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        refund.setAmountVnd(amount);
        refund.setReason(reason);
        refund.setStatus(RefundStatus.PENDING);
        refund.setAttemptCount(1);
        
        // Mocking immediate success for sandbox
        refund.setStatus(RefundStatus.SUCCEEDED);
        refund.setCompletedAt(Instant.now());
        
        refundRepository.save(refund);
        log.info("Mock refund processed successfully for payment {} (Reason: {})", paymentId, reason);
    }
}
