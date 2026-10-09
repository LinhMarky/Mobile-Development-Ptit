package com.example.roomly.data.repository;

import com.example.roomly.data.model.Booking;
import com.example.roomly.data.model.BookingHandover;
import com.example.roomly.data.model.BookingPayment;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class DemoBookingRepository {

    private static final DemoBookingRepository INSTANCE =
            new DemoBookingRepository();

    private final List<Booking> bookings = new ArrayList<>();

    private DemoBookingRepository() {
        createDemoBookings();
    }

    public static DemoBookingRepository getInstance() {
        return INSTANCE;
    }

    public synchronized List<Booking> getBookings() {
        return Collections.unmodifiableList(
                new ArrayList<>(bookings)
        );
    }

    public synchronized Booking getBookingById(String bookingId) {
        if (bookingId == null) {
            return null;
        }

        String normalizedId = bookingId.trim();

        for (Booking booking : bookings) {
            if (booking.getId().equals(normalizedId)) {
                return booking;
            }
        }

        return null;
    }

    public synchronized Booking createDemoRequest(
            String roomId,
            String roomTitle,
            String roomAddress,
            long monthlyRentVnd,
            Long depositVnd,
            long desiredMoveInMillis,
            int occupantCount,
            String note
    ) {
        if (desiredMoveInMillis < dateFromToday(1)) {
            throw new IllegalArgumentException(
                    "Ngày dự kiến vào ở phải từ ngày mai trở đi."
            );
        }

        Booking booking = new Booking(
                "DEMO-" + UUID.randomUUID().toString(),
                roomId,
                roomTitle,
                roomAddress,
                monthlyRentVnd,
                depositVnd,
                desiredMoveInMillis,
                occupantCount,
                note,
                Booking.Status.PENDING,
                System.currentTimeMillis(),
                true
        );

        bookings.add(0, booking);

        return booking;
    }

    public synchronized Booking cancelDemoRequest(String bookingId) {
        String normalizedId = requireBookingId(bookingId);

        for (int index = 0; index < bookings.size(); index++) {
            Booking current = bookings.get(index);

            if (!current.getId().equals(normalizedId)) {
                continue;
            }

            requireDemoBooking(current);

            if (current.getStatus() == Booking.Status.CANCELLED) {
                return current;
            }

            Booking cancelled = current.cancelDemo();
            bookings.set(index, cancelled);

            return cancelled;
        }

        throw new IllegalArgumentException(
                "Không tìm thấy yêu cầu thuê."
        );
    }

    public synchronized Booking approveDemoRequest(String bookingId) {
        return reviewDemoRequest(bookingId, true);
    }

    public synchronized Booking rejectDemoRequest(String bookingId) {
        return reviewDemoRequest(bookingId, false);
    }

    private Booking reviewDemoRequest(
            String bookingId,
            boolean approve
    ) {
        requireHostSession();

        String normalizedId = requireBookingId(bookingId);

        for (int index = 0; index < bookings.size(); index++) {
            Booking current = bookings.get(index);

            if (!current.getId().equals(normalizedId)) {
                continue;
            }

            if (!current.canReviewDemo()) {
                throw new IllegalStateException(
                        "Chỉ có thể xử lý yêu cầu demo đang chờ duyệt."
                );
            }

            Booking updated = approve
                    ? current.approveDemo()
                    : current.rejectDemo();

            bookings.set(index, updated);

            return updated;
        }

        throw new IllegalArgumentException(
                "Không tìm thấy yêu cầu thuê."
        );
    }

    public Booking confirmDemoRequest(String bookingId) {
        String normalizedId = requireBookingId(bookingId);

        BookingPayment latestPayment =
                DemoBookingPaymentRepository.getInstance()
                        .getLatestPayment(normalizedId);

        synchronized (this) {
            for (int index = 0; index < bookings.size(); index++) {
                Booking current = bookings.get(index);

                if (!current.getId().equals(normalizedId)) {
                    continue;
                }

                requireDemoBooking(current);

                if (current.getStatus() == Booking.Status.CONFIRMED) {
                    return current;
                }

                if (!current.canConfirmDemo()) {
                    throw new IllegalStateException(
                            "Chỉ có thể xác nhận yêu cầu demo đã được duyệt."
                    );
                }

                Long deposit = current.getDepositVnd();

                if (deposit == null) {
                    throw new IllegalStateException(
                            "Chưa cung cấp tiền cọc. "
                                    + "Chưa thể xác nhận yêu cầu demo."
                    );
                }

                if (deposit > 0) {
                    if (latestPayment == null
                            || !latestPayment.isDemo()
                            || latestPayment.getStatus()
                            != BookingPayment.Status.SUCCESS
                            || latestPayment.getAmountVnd()
                            != deposit.longValue()) {
                        throw new IllegalStateException(
                                "Cần thanh toán tiền cọc demo thành công "
                                        + "và đúng số tiền trước khi xác nhận."
                        );
                    }
                }

                Booking confirmed = current.confirmDemo();
                bookings.set(index, confirmed);

                return confirmed;
            }
        }

        throw new IllegalArgumentException(
                "Không tìm thấy yêu cầu thuê."
        );
    }

    public Booking completeDemoRequest(String bookingId) {
        String normalizedId = requireBookingId(bookingId);

        BookingHandover handover =
                DemoBookingHandoverRepository.getInstance()
                        .getHandover(normalizedId);

        if (handover == null
                || !handover.isDemo()
                || !handover.isCompleted()) {
            throw new IllegalStateException(
                    "Cần cả chủ trọ và người thuê xác nhận "
                            + "bàn giao demo trước khi hoàn tất."
            );
        }

        synchronized (this) {
            for (int index = 0; index < bookings.size(); index++) {
                Booking current = bookings.get(index);

                if (!current.getId().equals(normalizedId)) {
                    continue;
                }

                requireDemoBooking(current);

                if (current.getStatus() == Booking.Status.COMPLETED) {
                    return current;
                }

                Booking completed = current.completeDemo();
                bookings.set(index, completed);

                return completed;
            }
        }

        throw new IllegalArgumentException(
                "Không tìm thấy yêu cầu thuê."
        );
    }

    private void requireDemoBooking(Booking booking) {
        if (!booking.isDemo()) {
            throw new IllegalStateException(
                    "Chức năng này chỉ áp dụng cho yêu cầu demo."
            );
        }
    }

    private void requireHostSession() {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()
                || !session.hasRole(UserRole.HOST)) {
            throw new IllegalStateException(
                    "Chỉ tài khoản Chủ trọ mới được duyệt "
                            + "hoặc từ chối yêu cầu demo."
            );
        }
    }

    private void createDemoBookings() {
        addBooking(
                "DEMO-001", "demo-room-001",
                "Studio đầy đủ tiện nghi",
                "Cầu Giấy, Hà Nội",
                3_500_000L, 3_500_000L,
                10, 2,
                "Mong muốn vào ở đầu tháng, cần chỗ để xe.",
                Booking.Status.PENDING, -1
        );

        addBooking(
                "DEMO-002", "demo-room-002",
                "Phòng trọ gần trường",
                "Hà Đông, Hà Nội",
                2_800_000L, 0L,
                7, 1,
                "Mình muốn thuê dài hạn.",
                Booking.Status.APPROVED, -2
        );

        addBooking(
                "DEMO-003", "demo-room-003",
                "Căn hộ mini có ban công",
                "Thanh Xuân, Hà Nội",
                4_200_000L, 4_200_000L,
                5, 2, "",
                Booking.Status.CONFIRMED, -3
        );

        addBooking(
                "DEMO-004", "demo-room-004",
                "Phòng trọ khép kín",
                "Nam Từ Liêm, Hà Nội",
                3_000_000L, 3_000_000L,
                -10, 1,
                "Đã hoàn tất bàn giao phòng.",
                Booking.Status.COMPLETED, -20
        );

        addBooking(
                "DEMO-005", "demo-room-005",
                "Studio gần trạm xe buýt",
                "Đống Đa, Hà Nội",
                3_800_000L, null,
                8, 1,
                "Cần trao đổi thêm về các khoản phí.",
                Booking.Status.CANCELLED, -4
        );

        addBooking(
                "DEMO-006", "demo-room-006",
                "Phòng trọ có gác",
                "Hoàng Mai, Hà Nội",
                2_500_000L, 2_500_000L,
                12, 2, "",
                Booking.Status.REJECTED, -5
        );

        addBooking(
                "DEMO-007", "demo-room-007",
                "Căn hộ mini gần trường",
                "Hà Đông, Hà Nội",
                3_200_000L, 3_200_000L,
                6, 1, "",
                Booking.Status.EXPIRED, -7
        );

        addBooking(
                "DEMO-008", "demo-room-008",
                "Studio thử thanh toán tiền cọc",
                "Cầu Giấy, Hà Nội",
                3_600_000L, 3_600_000L,
                9, 2,
                "Mẫu kiểm tra thanh toán demo có tiền cọc.",
                Booking.Status.APPROVED, -1
        );

        addBooking(
                "DEMO-009", "demo-room-009",
                "Phòng thử trường hợp chưa cung cấp cọc",
                "Thanh Xuân, Hà Nội",
                3_100_000L, null,
                11, 1,
                "Mẫu kiểm tra giao diện khi chưa có thông tin tiền cọc.",
                Booking.Status.APPROVED, -2
        );
    }

    private void addBooking(
            String id,
            String roomId,
            String roomTitle,
            String roomAddress,
            long monthlyRentVnd,
            Long depositVnd,
            int moveInDaysFromToday,
            int occupantCount,
            String note,
            Booking.Status status,
            int createdDaysFromToday
    ) {
        bookings.add(
                new Booking(
                        id,
                        roomId,
                        roomTitle,
                        roomAddress,
                        monthlyRentVnd,
                        depositVnd,
                        dateFromToday(moveInDaysFromToday),
                        occupantCount,
                        note,
                        status,
                        dateFromToday(createdDaysFromToday),
                        true
                )
        );
    }

    private static String requireBookingId(String bookingId) {
        if (bookingId == null || bookingId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Mã yêu cầu không được để trống."
            );
        }

        return bookingId.trim();
    }

    private static long dateFromToday(int daysFromToday) {
        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DAY_OF_MONTH, daysFromToday);

        return calendar.getTimeInMillis();
    }
}