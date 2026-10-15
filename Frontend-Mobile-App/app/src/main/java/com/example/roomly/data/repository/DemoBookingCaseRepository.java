package com.example.roomly.data.repository;

import com.example.roomly.BuildConfig;
import com.example.roomly.data.model.Booking;
import com.example.roomly.data.model.BookingCase;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class DemoBookingCaseRepository {

    private static final DemoBookingCaseRepository INSTANCE =
            new DemoBookingCaseRepository();

    private final List<BookingCase> cases = new ArrayList<>();

    private DemoBookingCaseRepository() {
    }

    public static DemoBookingCaseRepository getInstance() {
        return INSTANCE;
    }

    public synchronized BookingCase getCaseByBookingId(String bookingId) {
        requireDemoAccess(null);

        String cleanedId = requireText(
                bookingId,
                "Mã yêu cầu thuê"
        );

        return findLatestCase(cleanedId);
    }

    public synchronized BookingCase createDemoCase(
            String bookingId,
            String expectedUserId,
            BookingCase.Type type,
            String description
    ) {
        String userId = requireText(expectedUserId, "Mã người gửi");
        SessionState session = requireDemoAccess(userId);

        String cleanedBookingId = requireText(
                bookingId,
                "Mã yêu cầu thuê"
        );

        Booking booking = DemoBookingRepository.getInstance()
                .getBookingById(cleanedBookingId);

        if (booking == null || !booking.isDemo()) {
            throw new IllegalStateException(
                    "Không tìm thấy yêu cầu thuê demo."
            );
        }

        for (BookingCase existing : cases) {
            if (existing.getBookingId().equals(cleanedBookingId)
                    && existing.isOpen()) {
                throw new IllegalStateException(
                        "Yêu cầu thuê này đã có yêu cầu hỗ trợ "
                                + "chưa xử lý. Bạn hãy xem yêu cầu hiện tại."
                );
            }
        }

        BookingCase bookingCase = new BookingCase(
                "DEMO-CASE-" + UUID.randomUUID(),
                cleanedBookingId,
                session.getUserId(),
                type,
                description,
                BookingCase.Status.OPEN,
                "",
                System.currentTimeMillis()
        );

        cases.add(bookingCase);
        return bookingCase;
    }

    // closed: null = tất cả, false = chưa xử lý, true = đã xử lý.
    public synchronized List<BookingCase> getAdminCases(Boolean closed) {
        requireAdminAccess(null);

        List<BookingCase> results = new ArrayList<>();

        for (BookingCase bookingCase : cases) {
            boolean isClosed = !bookingCase.isOpen();

            if (closed == null || closed.booleanValue() == isClosed) {
                results.add(bookingCase);
            }
        }

        results.sort(
                Comparator.comparingLong(
                        BookingCase::getCreatedAtMillis
                ).reversed()
        );

        return Collections.unmodifiableList(results);
    }

    public synchronized BookingCase getAdminCaseById(String caseId) {
        requireAdminAccess(null);

        String cleanedId = requireText(caseId, "Mã yêu cầu hỗ trợ");

        for (BookingCase bookingCase : cases) {
            if (bookingCase.getId().equals(cleanedId)) {
                return bookingCase;
            }
        }

        return null;
    }

    public synchronized BookingCase resolveDemoCase(
            String caseId,
            String expectedAdminUserId,
            BookingCase.Decision decision,
            String note
    ) {
        SessionState session = requireAdminAccess(
                requireText(expectedAdminUserId, "Mã Admin")
        );

        String cleanedId = requireText(caseId, "Mã yêu cầu hỗ trợ");

        for (int index = 0; index < cases.size(); index++) {
            BookingCase current = cases.get(index);

            if (current.getId().equals(cleanedId)) {
                BookingCase updated = current.resolve(
                        decision,
                        note,
                        session.getUserId()
                );

                cases.set(index, updated);
                return updated;
            }
        }

        throw new IllegalStateException(
                "Không tìm thấy yêu cầu hỗ trợ."
        );
    }

    private BookingCase findLatestCase(String bookingId) {
        for (int index = cases.size() - 1; index >= 0; index--) {
            BookingCase bookingCase = cases.get(index);

            if (bookingCase.getBookingId().equals(bookingId)) {
                return bookingCase;
            }
        }

        return null;
    }

    private SessionState requireDemoAccess(String expectedUserId) {
        SessionState session = requireSession(expectedUserId);

        if (!session.hasRole(UserRole.TENANT)
                && !session.hasRole(UserRole.HOST)) {
            throw new IllegalStateException(
                    "Tài khoản cần có quyền Người thuê hoặc Chủ trọ."
            );
        }

        return session;
    }

    private SessionState requireAdminAccess(String expectedUserId) {
        SessionState session = requireSession(expectedUserId);

        if (!session.hasRole(UserRole.ADMIN)) {
            throw new IllegalStateException(
                    "Tài khoản không có quyền quản trị."
            );
        }

        return session;
    }

    private SessionState requireSession(String expectedUserId) {
        if (!BuildConfig.DEBUG) {
            throw new IllegalStateException(
                    "Yêu cầu hỗ trợ demo chỉ dùng trong bản debug."
            );
        }

        SessionState session =
                SessionRepository.getInstance().getCurrentSession();

        if (!session.isLoggedIn()) {
            throw new IllegalStateException(
                    "Bạn cần đăng nhập để sử dụng chức năng này."
            );
        }

        if (expectedUserId != null
                && !expectedUserId.equals(session.getUserId())) {
            throw new IllegalStateException(
                    "Phiên đăng nhập đã thay đổi. "
                            + "Vui lòng mở lại màn hình."
            );
        }

        return session;
    }

    private static String requireText(String value, String fieldName) {
        String cleaned = value == null ? "" : value.trim();

        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException(
                    fieldName + " không được để trống."
            );
        }

        return cleaned;
    }
}