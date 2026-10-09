package com.example.roomly.data.repository;

import com.example.roomly.data.model.AppMode;
import com.example.roomly.data.model.Booking;
import com.example.roomly.data.model.BookingHandover;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;

import java.util.HashMap;
import java.util.Map;

public final class DemoBookingHandoverRepository {

    private static final DemoBookingHandoverRepository INSTANCE =
            new DemoBookingHandoverRepository();

    private final Map<String, BookingHandover> handovers =
            new HashMap<>();

    private DemoBookingHandoverRepository() {
    }

    public static DemoBookingHandoverRepository getInstance() {
        return INSTANCE;
    }

    public synchronized BookingHandover getHandover(
            String bookingId
    ) {
        return handovers.get(requireBookingId(bookingId));
    }

    public synchronized BookingHandover getOrCreateHandover(
            String bookingId
    ) {
        Booking booking = requireEligibleBooking(bookingId);

        BookingHandover existing = handovers.get(booking.getId());

        if (existing != null) {
            return existing;
        }

        BookingHandover handover;

        if (booking.getStatus() == Booking.Status.COMPLETED) {
            // Mẫu Hoàn tất có sẵn được khởi tạo với hai xác nhận.
            // Không có thời gian bàn giao thực tế của mẫu này.
            handover = new BookingHandover(
                    booking.getId(),
                    booking.getCreatedAtMillis(),
                    booking.getCreatedAtMillis(),
                    true
            );
        } else {
            handover = new BookingHandover(
                    booking.getId(),
                    null,
                    null,
                    true
            );
        }

        handovers.put(booking.getId(), handover);

        return handover;
    }

    public synchronized BookingHandover confirmDemo(
            String bookingId,
            AppMode actorMode,
            String expectedUserId
    ) {
        requireActorSession(actorMode, expectedUserId);

        Booking booking = requireEligibleBooking(bookingId);

        BookingHandover current = getOrCreateHandover(
                booking.getId()
        );

        if (booking.getStatus() == Booking.Status.COMPLETED) {
            if (!current.isCompleted()) {
                throw new IllegalStateException(
                        "Thông tin bàn giao chưa khớp trạng thái yêu cầu."
                );
            }

            return current;
        }

        BookingHandover updated;

        if (actorMode == AppMode.HOST) {
            updated = current.confirmHostDemo();
        } else {
            updated = current.confirmTenantDemo();
        }

        handovers.put(booking.getId(), updated);

        return updated;
    }

    private Booking requireEligibleBooking(String bookingId) {
        String normalizedId = requireBookingId(bookingId);

        Booking booking = DemoBookingRepository.getInstance()
                .getBookingById(normalizedId);

        if (booking == null) {
            throw new IllegalArgumentException(
                    "Không tìm thấy yêu cầu thuê."
            );
        }

        if (!booking.isDemo()) {
            throw new IllegalStateException(
                    "Chức năng này chỉ áp dụng cho bàn giao demo."
            );
        }

        if (booking.getStatus() != Booking.Status.CONFIRMED
                && booking.getStatus() != Booking.Status.COMPLETED) {
            throw new IllegalStateException(
                    "Chỉ có thể xem bàn giao của yêu cầu "
                            + "đã xác nhận hoặc đã hoàn tất."
            );
        }

        return booking;
    }

    private void requireActorSession(
            AppMode actorMode,
            String expectedUserId
    ) {
        if (actorMode != AppMode.HOST
                && actorMode != AppMode.TENANT) {
            throw new IllegalArgumentException(
                    "Vai trò xác nhận bàn giao không hợp lệ."
            );
        }

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()
                || expectedUserId == null
                || !expectedUserId.equals(session.getUserId())) {
            throw new IllegalStateException(
                    "Tài khoản đã thay đổi. "
                            + "Vui lòng mở lại màn hình bàn giao."
            );
        }

        UserRole requiredRole = actorMode == AppMode.HOST
                ? UserRole.HOST
                : UserRole.TENANT;

        if (!session.hasRole(requiredRole)) {
            throw new IllegalStateException(
                    "Tài khoản không có quyền xác nhận "
                            + "theo vai trò đã chọn."
            );
        }
    }

    private static String requireBookingId(String bookingId) {
        if (bookingId == null || bookingId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Mã yêu cầu thuê không được để trống."
            );
        }

        return bookingId.trim();
    }
}