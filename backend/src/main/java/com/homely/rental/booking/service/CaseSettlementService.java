package com.homely.rental.booking.service;
import com.homely.rental.booking.entity.*;
import com.homely.rental.payment.repository.PaymentRepository;
import com.homely.rental.payment.entity.*;
import com.homely.rental.payment.service.RefundService;
import com.homely.rental.catalog.entity.RoomAvailability;
import com.homely.rental.common.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.math.BigDecimal;
import java.time.Instant;

@Service @RequiredArgsConstructor
public class CaseSettlementService {
    private final BookingLocks locks;
    private final PaymentRepository payments;
    private final RefundService refunds;
    private final BookingTransitions transitions;
    @Transactional(propagation=Propagation.MANDATORY)
    public void settle(BookingCase item, BookingCaseDecision decision, BigDecimal tenantRefund) {
        if (decision == BookingCaseDecision.NO_ACTION) return;
        Booking booking = locks.lock(item.getBooking().getId());
        if (booking.getStatus() != BookingStatus.CONFIRMED || booking.getAllocatedPaymentId() == null)
            throw new ConflictException("INVALID_BOOKING_STATUS","A confirmed booking with an allocated payment is required");
        Payment payment = payments.findById(booking.getAllocatedPaymentId()).orElseThrow();
        if (payment.getStatus() != PaymentStatus.SUCCEEDED) throw new ConflictException("PAYMENT_REQUIRED","Payment has not succeeded");
        BigDecimal amount = switch(decision) {
            case REFUND_TENANT -> payment.getAmountVnd();
            case FORFEIT_DEPOSIT -> BigDecimal.ZERO;
            case SPLIT -> {
                if (tenantRefund == null || tenantRefund.scale() > 0 || tenantRefund.signum() <= 0
                        || tenantRefund.compareTo(payment.getAmountVnd()) >= 0)
                    throw new IllegalArgumentException("SPLIT requires tenant_refund_vnd strictly between zero and the deposit");
                yield tenantRefund;
            }
            default -> throw new IllegalArgumentException("Unsupported decision");
        };
        if (amount.signum() > 0) refunds.refund(payment, RefundReason.CASE_DECISION, amount);
        item.setTenantRefundVnd(amount);
        item.setHostRetainedVnd(payment.getAmountVnd().subtract(amount));
        booking.setStatus(BookingStatus.CANCELLED); booking.setCancelledAt(Instant.now());
        booking.setLastReason("Case resolution: " + decision);
        booking.getRoom().setAvailability(RoomAvailability.AVAILABLE);
        transitions.record(booking,BookingStatus.CONFIRMED,BookingStatus.CANCELLED,
                item.getResolvedBy(),ActorType.ADMIN,booking.getLastReason());
    }
}
