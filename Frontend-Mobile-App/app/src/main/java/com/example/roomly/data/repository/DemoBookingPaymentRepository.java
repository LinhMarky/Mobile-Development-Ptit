package com.example.roomly.data.repository;

import com.example.roomly.data.model.Booking;
import com.example.roomly.data.model.BookingPayment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class DemoBookingPaymentRepository {

    private static final DemoBookingPaymentRepository INSTANCE =
            new DemoBookingPaymentRepository();

    private final List<BookingPayment> payments = new ArrayList<>();

    private DemoBookingPaymentRepository() {
    }

    public static DemoBookingPaymentRepository getInstance() {
        return INSTANCE;
    }

    public synchronized BookingPayment getLatestPayment(
            String bookingId
    ) {
        String normalizedId = requireText(
                bookingId,
                "Mã yêu cầu thuê"
        );

        for (int index = payments.size() - 1; index >= 0; index--) {
            BookingPayment payment = payments.get(index);

            if (payment.getBookingId().equals(normalizedId)) {
                return payment;
            }
        }

        return null;
    }

    public synchronized List<BookingPayment> getPaymentHistory(
            String bookingId
    ) {
        String normalizedId = requireText(
                bookingId,
                "Mã yêu cầu thuê"
        );

        List<BookingPayment> history = new ArrayList<>();

        for (int index = payments.size() - 1; index >= 0; index--) {
            BookingPayment payment = payments.get(index);

            if (payment.getBookingId().equals(normalizedId)) {
                history.add(payment);
            }
        }

        return Collections.unmodifiableList(history);
    }

    public synchronized BookingPayment getOrCreatePayment(
            String bookingId
    ) {
        Booking booking = requireDemoBooking(bookingId);

        BookingPayment latest = getLatestPayment(booking.getId());

        // Cho phép xem lại kết quả sau khi yêu cầu đã xác nhận
        // hoặc đã hoàn tất bàn giao.
        if (booking.getStatus() == Booking.Status.CONFIRMED
                || booking.getStatus() == Booking.Status.COMPLETED) {
            if (latest != null
                    && latest.getStatus()
                    == BookingPayment.Status.SUCCESS) {
                return latest;
            }

            throw new IllegalStateException(
                    "Không có kết quả thanh toán demo thành công "
                            + "cho yêu cầu này."
            );
        }

        requirePaymentEligibility(booking);

        if (latest != null) {
            return latest;
        }

        return createPayment(booking);
    }

    public synchronized BookingPayment retryPayment(
            String bookingId
    ) {
        Booking booking = requireDemoBooking(bookingId);
        requirePaymentEligibility(booking);

        BookingPayment latest = getLatestPayment(booking.getId());

        if (latest == null || !latest.canRetryDemo()) {
            throw new IllegalStateException(
                    "Chỉ có thể thử lại thanh toán demo "
                            + "đã thất bại hoặc hết hạn."
            );
        }

        return createPayment(booking);
    }

    public synchronized BookingPayment simulateResult(
            String paymentId,
            BookingPayment.Status result
    ) {
        String normalizedId = requireText(
                paymentId,
                "Mã thanh toán"
        );

        for (int index = 0; index < payments.size(); index++) {
            BookingPayment current = payments.get(index);

            if (!current.getId().equals(normalizedId)) {
                continue;
            }

            Booking booking = requireDemoBooking(
                    current.getBookingId()
            );

            requirePaymentEligibility(booking);

            BookingPayment latest = getLatestPayment(
                    booking.getId()
            );

            if (latest == null
                    || !latest.getId().equals(current.getId())) {
                throw new IllegalStateException(
                        "Lượt thanh toán này đã được thay thế. "
                                + "Vui lòng sử dụng lượt mới nhất."
                );
            }

            Long deposit = booking.getDepositVnd();

            if (deposit == null
                    || current.getAmountVnd() != deposit.longValue()) {
                throw new IllegalStateException(
                        "Số tiền thanh toán không khớp tiền cọc."
                );
            }

            BookingPayment updated = current.simulateResult(result);

            payments.set(index, updated);

            if (result == BookingPayment.Status.SUCCESS) {
                try {
                    DemoBookingRepository.getInstance()
                            .confirmDemoRequest(booking.getId());
                } catch (RuntimeException exception) {
                    // Khôi phục lượt thanh toán nếu xác nhận thất bại.
                    payments.set(index, current);
                    throw exception;
                }
            }

            return updated;
        }

        throw new IllegalArgumentException(
                "Không tìm thấy lượt thanh toán."
        );
    }

    private BookingPayment createPayment(Booking booking) {
        requirePaymentEligibility(booking);

        long now = System.currentTimeMillis();

        BookingPayment payment = new BookingPayment(
                "DEMO-PAY-" + UUID.randomUUID().toString(),
                booking.getId(),
                booking.getDepositVnd(),
                BookingPayment.Status.PENDING,
                now,
                now,
                true
        );

        payments.add(payment);

        return payment;
    }

    private Booking requireDemoBooking(String bookingId) {
        String normalizedId = requireText(
                bookingId,
                "Mã yêu cầu thuê"
        );

        Booking booking = DemoBookingRepository.getInstance()
                .getBookingById(normalizedId);

        if (booking == null) {
            throw new IllegalArgumentException(
                    "Không tìm thấy yêu cầu thuê."
            );
        }

        if (!booking.isDemo()) {
            throw new IllegalStateException(
                    "Chức năng này chỉ áp dụng cho yêu cầu demo."
            );
        }

        return booking;
    }

    private void requirePaymentEligibility(Booking booking) {
        if (booking.getStatus() != Booking.Status.APPROVED) {
            throw new IllegalStateException(
                    "Chỉ có thể thanh toán tiền cọc "
                            + "cho yêu cầu demo đã được duyệt."
            );
        }

        Long deposit = booking.getDepositVnd();

        if (deposit == null) {
            throw new IllegalStateException(
                    "Chưa cung cấp tiền cọc. "
                            + "Không thể tạo lượt thanh toán."
            );
        }

        if (deposit <= 0) {
            throw new IllegalStateException(
                    "Phòng không yêu cầu cọc, "
                            + "không cần tạo lượt thanh toán."
            );
        }
    }

    private static String requireText(
            String value,
            String fieldName
    ) {
        String normalized = value == null ? "" : value.trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    fieldName + " không được để trống."
            );
        }

        return normalized;
    }
}