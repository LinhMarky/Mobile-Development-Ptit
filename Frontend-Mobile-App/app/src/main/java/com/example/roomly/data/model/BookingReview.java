package com.example.roomly.data.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class BookingReview {

    private final String id;
    private final String bookingId;
    private final String reviewerId;
    private final int rating;
    private final String comment;
    private final long createdAtMillis;
    private final boolean demo;

    public BookingReview(
            String id,
            String bookingId,
            String reviewerId,
            int rating,
            String comment,
            long createdAtMillis,
            boolean demo
    ) {
        this.id = requireText(id, "Mã đánh giá");
        this.bookingId = requireText(bookingId, "Mã yêu cầu thuê");
        this.reviewerId = requireText(reviewerId, "Mã người đánh giá");

        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException(
                    "Vui lòng chọn từ 1 đến 5 sao."
            );
        }

        String normalizedComment = comment == null
                ? ""
                : comment.trim();

        if (normalizedComment.codePointCount(
                0, normalizedComment.length()
        ) > 1000) {
            throw new IllegalArgumentException(
                    "Nhận xét tối đa 1.000 ký tự."
            );
        }

        if (createdAtMillis <= 0) {
            throw new IllegalArgumentException(
                    "Thời gian đánh giá không hợp lệ."
            );
        }

        this.rating = rating;
        this.comment = normalizedComment;
        this.createdAtMillis = createdAtMillis;
        this.demo = demo;
    }

    public String getId() {
        return id;
    }

    public String getBookingId() {
        return bookingId;
    }

    public String getReviewerId() {
        return reviewerId;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public boolean isDemo() {
        return demo;
    }

    public String getRatingLabel() {
        switch (rating) {
            case 1:
                return "1 sao — Rất không hài lòng";

            case 2:
                return "2 sao — Không hài lòng";

            case 3:
                return "3 sao — Bình thường";

            case 4:
                return "4 sao — Hài lòng";

            case 5:
                return "5 sao — Rất hài lòng";

            default:
                return "Chưa đánh giá";
        }
    }

    public String getCreatedAtLabel() {
        SimpleDateFormat formatter = new SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                new Locale("vi", "VN")
        );

        return formatter.format(new Date(createdAtMillis));
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