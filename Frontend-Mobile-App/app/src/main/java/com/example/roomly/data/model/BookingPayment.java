package com.example.roomly.data.model;

import java.text.NumberFormat;
import java.util.Locale;

public final class BookingPayment {

    public enum Status {
        PENDING,
        SUCCESS,
        FAILED,
        EXPIRED
    }

    private final String id;
    private final String bookingId;
    private final long amountVnd;
    private final Status status;
    private final long createdAtMillis;
    private final long updatedAtMillis;
    private final boolean demo;

    public BookingPayment(
            String id,
            String bookingId,
            long amountVnd,
            Status status,
            long createdAtMillis,
            long updatedAtMillis,
            boolean demo
    ) {
        this.id = requireText(id, "Mã thanh toán");
        this.bookingId = requireText(bookingId, "Mã yêu cầu thuê");

        if (amountVnd <= 0) {
            throw new IllegalArgumentException(
                    "Số tiền thanh toán phải lớn hơn 0."
            );
        }

        if (status == null) {
            throw new IllegalArgumentException(
                    "Trạng thái thanh toán không được để trống."
            );
        }

        if (createdAtMillis <= 0
                || updatedAtMillis < createdAtMillis) {
            throw new IllegalArgumentException(
                    "Thời gian thanh toán không hợp lệ."
            );
        }

        this.amountVnd = amountVnd;
        this.status = status;
        this.createdAtMillis = createdAtMillis;
        this.updatedAtMillis = updatedAtMillis;
        this.demo = demo;
    }

    public String getId() {
        return id;
    }

    public String getBookingId() {
        return bookingId;
    }

    public long getAmountVnd() {
        return amountVnd;
    }

    public Status getStatus() {
        return status;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public long getUpdatedAtMillis() {
        return updatedAtMillis;
    }

    public boolean isDemo() {
        return demo;
    }

    public boolean canSimulateResult() {
        return demo && status == Status.PENDING;
    }

    public boolean canRetryDemo() {
        return demo
                && (status == Status.FAILED
                || status == Status.EXPIRED);
    }

    public BookingPayment simulateResult(Status result) {
        if (!canSimulateResult()) {
            throw new IllegalStateException(
                    "Chỉ có thể mô phỏng thanh toán demo đang chờ."
            );
        }

        if (result != Status.SUCCESS
                && result != Status.FAILED
                && result != Status.EXPIRED) {
            throw new IllegalArgumentException(
                    "Kết quả mô phỏng không hợp lệ."
            );
        }

        return new BookingPayment(
                id,
                bookingId,
                amountVnd,
                result,
                createdAtMillis,
                Math.max(
                        System.currentTimeMillis(),
                        updatedAtMillis
                ),
                demo
        );
    }

    public String getAmountLabel() {
        NumberFormat formatter = NumberFormat.getIntegerInstance(
                new Locale("vi", "VN")
        );

        return formatter.format(amountVnd) + " ₫";
    }

    public String getStatusLabel() {
        switch (status) {
            case PENDING:
                return "Chờ thanh toán";

            case SUCCESS:
                return "Thanh toán thành công";

            case FAILED:
                return "Thanh toán thất bại";

            case EXPIRED:
                return "Thanh toán hết hạn";

            default:
                return "Chưa xác định";
        }
    }

    public String getStatusDescription() {
        String description;

        switch (status) {
            case PENDING:
                description = "Đang chờ kết quả thanh toán tiền cọc.";
                break;

            case SUCCESS:
                description = "Thanh toán tiền cọc đã thành công.";
                break;

            case FAILED:
                description = "Thanh toán chưa thành công. "
                        + "Bạn có thể thử lại.";
                break;

            case EXPIRED:
                description = "Lượt thanh toán đã hết hạn. "
                        + "Bạn có thể tạo lượt thanh toán mới.";
                break;

            default:
                description = "Chưa có thông tin thanh toán.";
                break;
        }

        if (demo) {
            description += "\nĐây là trạng thái mô phỏng, "
                    + "không phát sinh giao dịch hoặc thu tiền thực tế.";
        }

        return description;
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