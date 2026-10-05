package com.homely.rental.payment.service;
import com.homely.rental.auth.security.AccountAccessService;
import com.homely.rental.booking.entity.*;
import com.homely.rental.booking.service.*;
import com.homely.rental.catalog.entity.RoomAvailability;
import com.homely.rental.common.exception.*;
import com.homely.rental.payment.dto.*;
import com.homely.rental.payment.entity.*;
import com.homely.rental.payment.repository.*;
import com.homely.rental.notification.entity.NotificationType;
import com.homely.rental.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

@Service @RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository payments;
    private final WebhookReceiptRepository receipts;
    private final RefundService refunds;
    private final BookingLocks locks;
    private final BookingTransitions transitions;
    private final AccountAccessService accounts;
    private final NotificationService notifications;
    private final jakarta.persistence.EntityManager entityManager;

    @Transactional
    public PaymentDTO createPayment(PaymentCreateRequest dto) {
        var user = accounts.requireCurrent();
        Booking booking = locks.lock(dto.getBookingId());
        if (!booking.getTenant().getId().equals(user.getId())) throw new ResourceNotFoundException("Booking", dto.getBookingId());
        Instant now = Instant.now();
        if (booking.getStatus() != BookingStatus.APPROVED || booking.getHoldExpiresAt() == null || !booking.getHoldExpiresAt().isAfter(now))
            throw new ConflictException("INVALID_BOOKING_STATUS", "An unexpired approved booking is required");
        for (Payment old : payments.findByBookingIdOrderByCreatedAtDesc(booking.getId())) {
            if (old.getStatus() == PaymentStatus.PENDING) {
                if (old.getExpiresAt().isAfter(now)) return toDTO(old);
                markExpired(old);
            }
        }
        payments.flush();
        Payment p = new Payment();
        p.setBooking(booking); p.setPayer(user);
        p.setProviderPaymentId("MOCK-" + UUID.randomUUID());
        p.setAmountVnd(booking.getDepositVnd());
        Instant expires = now.plus(15, ChronoUnit.MINUTES);
        p.setExpiresAt(expires.isBefore(booking.getHoldExpiresAt()) ? expires : booking.getHoldExpiresAt());
        return toDTO(payments.save(p));
    }

    /** One attempt per transaction; the scan is only a hint and may already be stale. */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public boolean expirePayment(Long paymentId, Instant cutoff) {
        Long bookingId = payments.findBookingIdByPaymentId(paymentId).orElse(null);
        if (bookingId == null) return false;
        locks.lock(bookingId);
        Payment payment = payments.lockById(paymentId).orElse(null);
        if (payment == null) return false;
        entityManager.refresh(payment);
        if (payment.getStatus() != PaymentStatus.PENDING || payment.getExpiresAt().isAfter(cutoff)) return false;
        markExpired(payment);
        // Expiring an attempt does not cancel the booking or release its still-valid hold.
        return true;
    }

    private void markExpired(Payment payment) {
        payment.setStatus(PaymentStatus.EXPIRED);
        notifyPayer(payment, NotificationType.PAYMENT_EXPIRED, "Lượt thanh toán hết hạn",
                "Lượt thanh toán #" + payment.getId() + " đã hết hạn. Bạn có thể thử lại nếu yêu cầu thuê còn được giữ chỗ.");
    }

    @Transactional
    public void processWebhook(WebhookPayload payload) {
        validate(payload);
        Long bookingId = payments.findBookingId(payload.getProviderPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment", payload.getProviderPaymentId()));
        Booking booking = locks.lock(bookingId);
        Payment p = payments.lockByProviderId(payload.getProviderPaymentId()).orElseThrow();
        entityManager.refresh(p);
        byte[] fingerprint = digest(payload);
        var previous = receipts.findByProviderAndEventId("MOCK_SANDBOX", payload.getEventId());
        if (previous.isPresent()) {
            if (!MessageDigest.isEqual(previous.get().getPayloadHash(), fingerprint))
                throw new ConflictException("WEBHOOK_EVENT_REUSED", "Event ID already used with another payload");
            return;
        }
        String outcome;
        if (p.getStatus() == PaymentStatus.SUCCEEDED) outcome = "DUPLICATE";
        else if ("SUCCEEDED".equals(payload.getStatus())) {
            Instant now = Instant.now();
            p.setStatus(PaymentStatus.SUCCEEDED); p.setSucceededAt(now);
            boolean eligible = booking.getStatus() == BookingStatus.APPROVED
                    && booking.getHoldExpiresAt() != null && booking.getHoldExpiresAt().isAfter(now)
                    && p.getExpiresAt().isAfter(now) && booking.getAllocatedPaymentId() == null
                    && booking.getRoom().getAvailability() == RoomAvailability.HELD
                    && p.getAmountVnd().compareTo(booking.getDepositVnd()) == 0;
            if (eligible) {
                booking.setAllocatedPaymentId(p.getId()); booking.setStatus(BookingStatus.CONFIRMED);
                booking.setTenantTermsAcceptedAt(now); booking.setHandoverDueAt(now.plus(7, ChronoUnit.DAYS));
                transitions.record(booking, BookingStatus.APPROVED, BookingStatus.CONFIRMED, null, ActorType.SYSTEM, "Sandbox payment succeeded");
                notifyPayer(p, NotificationType.PAYMENT_SUCCEEDED, "Thanh toán thành công",
                        "Tiền cọc thử nghiệm cho yêu cầu thuê #" + booking.getId() + " đã được ghi nhận.");
                outcome = "ALLOCATED";
            } else {
                refunds.refund(p, RefundReason.BOOKING_EXPIRED, p.getAmountVnd());
                notifyPayer(p, NotificationType.PAYMENT_REFUNDED, "Đã hoàn tiền thử nghiệm",
                        "Khoản thanh toán #" + p.getId() + " không thể áp dụng và đã được hoàn trong sandbox.");
                if (booking.getStatus() == BookingStatus.APPROVED && booking.getHoldExpiresAt() != null && !booking.getHoldExpiresAt().isAfter(now)) {
                    booking.setStatus(BookingStatus.EXPIRED); booking.setCancelledAt(now);
                    booking.setLastReason("Hold expired before payment");
                    booking.getRoom().setAvailability(RoomAvailability.AVAILABLE);
                    transitions.record(booking, BookingStatus.APPROVED, BookingStatus.EXPIRED, null, ActorType.SYSTEM, booking.getLastReason());
                }
                outcome = "REFUNDED";
            }
        } else {
            if (p.getStatus() == PaymentStatus.PENDING) {
                p.setStatus(PaymentStatus.FAILED); p.setFailureCode(payload.getFailureCode());
                notifyPayer(p, NotificationType.PAYMENT_FAILED, "Thanh toán chưa thành công",
                        "Lượt thanh toán #" + p.getId() + " thất bại. Bạn có thể thử lại nếu yêu cầu thuê còn được giữ chỗ.");
            }
            outcome = "FAILED";
        }
        WebhookReceipt receipt = new WebhookReceipt();
        receipt.setProvider("MOCK_SANDBOX"); receipt.setEventId(payload.getEventId());
        receipt.setPayment(p); receipt.setPayloadHash(fingerprint); receipt.setOutcome(outcome);
        receipts.saveAndFlush(receipt);
    }

    @Transactional(readOnly = true)
    public PaymentDTO getPaymentInfo(Long bookingId) {
        var user = accounts.requireCurrent();
        Payment p = payments.findByBookingId(bookingId).orElseThrow(() -> new ResourceNotFoundException("Payment for Booking", bookingId));
        if (!p.getBooking().getTenant().getId().equals(user.getId()) && !p.getBooking().getHost().getId().equals(user.getId()))
            throw new ResourceNotFoundException("Payment for Booking", bookingId);
        return toDTO(p);
    }

    @Transactional
    public PaymentDTO simulate(Long paymentId, String status) {
        var user = accounts.requireCurrent();
        Payment snapshot = payments.findById(paymentId).orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
        if (!snapshot.getPayer().getId().equals(user.getId())) throw new ResourceNotFoundException("Payment", paymentId);
        WebhookPayload payload = new WebhookPayload();
        payload.setProviderPaymentId(snapshot.getProviderPaymentId());
        payload.setEventId(UUID.randomUUID().toString()); payload.setStatus(status);
        processWebhook(payload);
        return toDTO(snapshot);
    }

    private void validate(WebhookPayload p) {
        if (p.getProviderPaymentId() == null || p.getProviderPaymentId().length() > 100
                || p.getEventId() == null || !p.getEventId().matches("[0-9a-fA-F-]{36}")
                || !java.util.Set.of("SUCCEEDED","FAILED").contains(p.getStatus() == null ? "" : p.getStatus())
                || (p.getFailureCode() != null && p.getFailureCode().length() > 100))
            throw new IllegalArgumentException("Invalid webhook payload");
        p.setEventId(UUID.fromString(p.getEventId()).toString());
    }

    private void notifyPayer(Payment payment, NotificationType type, String title, String body) {
        notifications.createNotification(payment.getPayer(), type, title, body, "booking", payment.getBooking().getId());
    }

    private byte[] digest(WebhookPayload p) {
        try {
            String value = p.getProviderPaymentId() + "\n" + p.getStatus() + "\n" + java.util.Objects.toString(p.getFailureCode(), "");
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private PaymentDTO toDTO(Payment p) {
        return PaymentDTO.builder().id(p.getId()).bookingId(p.getBooking().getId()).payerId(p.getPayer().getId())
                .provider(p.getProvider().name()).providerPaymentId(p.getProviderPaymentId()).amountVnd(p.getAmountVnd())
                .status(p.getStatus().name()).expiresAt(p.getExpiresAt()).succeededAt(p.getSucceededAt())
                .isTest(p.isTest()).createdAt(p.getCreatedAt()).build();
    }
}
