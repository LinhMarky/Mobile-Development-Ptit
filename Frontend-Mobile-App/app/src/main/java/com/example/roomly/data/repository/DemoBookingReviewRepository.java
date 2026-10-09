package com.example.roomly.data.repository;

import com.example.roomly.data.model.Booking;
import com.example.roomly.data.model.BookingReview;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DemoBookingReviewRepository {

    private static final DemoBookingReviewRepository INSTANCE =
            new DemoBookingReviewRepository();

    private final Map<String, Map<String, BookingReview>> reviews =
            new HashMap<>();

    private DemoBookingReviewRepository() {
    }

    public static DemoBookingReviewRepository getInstance() {
        return INSTANCE;
    }

    public synchronized BookingReview getReview(
            String bookingId,
            String reviewerId
    ) {
        String normalizedBookingId = requireText(
                bookingId,
                "Mã yêu cầu thuê"
        );

        String normalizedReviewerId = requireText(
                reviewerId,
                "Mã người đánh giá"
        );

        Map<String, BookingReview> bookingReviews =
                reviews.get(normalizedBookingId);

        if (bookingReviews == null) {
            return null;
        }

        return bookingReviews.get(normalizedReviewerId);
    }

    public synchronized BookingReview createDemoReview(
            String bookingId,
            String expectedUserId,
            int rating,
            String comment
    ) {
        String normalizedBookingId = requireText(
                bookingId,
                "Mã yêu cầu thuê"
        );

        String normalizedUserId = requireText(
                expectedUserId,
                "Mã người đánh giá"
        );

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()
                || !normalizedUserId.equals(session.getUserId())) {
            throw new IllegalStateException(
                    "Tài khoản đã thay đổi. "
                            + "Vui lòng mở lại màn hình đánh giá."
            );
        }

        if (!session.hasRole(UserRole.TENANT)) {
            throw new IllegalStateException(
                    "Chỉ tài khoản Người thuê mới được đánh giá."
            );
        }

        Booking booking = DemoBookingRepository.getInstance()
                .getBookingById(normalizedBookingId);

        if (booking == null) {
            throw new IllegalArgumentException(
                    "Không tìm thấy yêu cầu thuê."
            );
        }

        if (!booking.isDemo()) {
            throw new IllegalStateException(
                    "Chức năng này chỉ áp dụng cho đánh giá demo."
            );
        }

        if (booking.getStatus() != Booking.Status.COMPLETED) {
            throw new IllegalStateException(
                    "Chỉ có thể đánh giá yêu cầu thuê đã Hoàn tất."
            );
        }

        if (getReview(normalizedBookingId, normalizedUserId) != null) {
            throw new IllegalStateException(
                    "Bạn đã lưu đánh giá demo cho yêu cầu này."
            );
        }

        BookingReview review = new BookingReview(
                "DEMO-REVIEW-" + UUID.randomUUID().toString(),
                normalizedBookingId,
                normalizedUserId,
                rating,
                comment,
                System.currentTimeMillis(),
                true
        );

        Map<String, BookingReview> bookingReviews =
                reviews.get(normalizedBookingId);

        if (bookingReviews == null) {
            bookingReviews = new HashMap<>();
            reviews.put(normalizedBookingId, bookingReviews);
        }

        bookingReviews.put(normalizedUserId, review);

        return review;
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