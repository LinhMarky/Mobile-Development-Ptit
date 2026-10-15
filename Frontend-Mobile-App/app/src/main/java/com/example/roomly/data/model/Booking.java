package com.example.roomly.data.model;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class Booking {

    public enum Status {
        PENDING,
        APPROVED,
        CONFIRMED,
        COMPLETED,
        CANCELLED,
        REJECTED,
        EXPIRED
    }

    private final String id;
    private final String roomId;
    private final String roomTitle;
    private final String roomAddress;

    private final long monthlyRentVnd;
    private final Long depositVnd;

    private final long desiredMoveInMillis;
    private final int occupantCount;
    private final String note;

    private final Status status;
    private final long createdAtMillis;
    private final boolean demo;

    public Booking(
            String id,
            String roomId,
            String roomTitle,
            String roomAddress,
            long monthlyRentVnd,
            Long depositVnd,
            long desiredMoveInMillis,
            int occupantCount,
            String note,
            Status status,
            long createdAtMillis,
            boolean demo
    ) {
        this.id = requireText(id, "Mã yêu cầu");
        this.roomId = requireText(roomId, "Mã phòng");
        this.roomTitle = requireText(roomTitle, "Tên phòng");
        this.roomAddress = normalize(roomAddress);

        if (monthlyRentVnd < 0) {
            throw new IllegalArgumentException(
                    "Giá thuê không được âm."
            );
        }

        if (depositVnd != null && depositVnd < 0) {
            throw new IllegalArgumentException(
                    "Tiền cọc không được âm."
            );
        }

        if (desiredMoveInMillis <= 0 || createdAtMillis <= 0) {
            throw new IllegalArgumentException(
                    "Ngày vào ở và ngày tạo phải hợp lệ."
            );
        }

        if (occupantCount < 1 || occupantCount > 10) {
            throw new IllegalArgumentException(
                    "Số người ở phải từ 1 đến 10."
            );
        }

        String normalizedNote = normalize(note);

        if (normalizedNote.codePointCount(
                0, normalizedNote.length()
        ) > 1000) {
            throw new IllegalArgumentException(
                    "Ghi chú tối đa 1.000 ký tự."
            );
        }

        if (status == null) {
            throw new IllegalArgumentException(
                    "Trạng thái không được để trống."
            );
        }

        this.monthlyRentVnd = monthlyRentVnd;
        this.depositVnd = depositVnd;
        this.desiredMoveInMillis = desiredMoveInMillis;
        this.occupantCount = occupantCount;
        this.note = normalizedNote;
        this.status = status;
        this.createdAtMillis = createdAtMillis;
        this.demo = demo;
    }

    public String getId() {
        return id;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getRoomTitle() {
        return roomTitle;
    }

    public String getRoomAddress() {
        return roomAddress;
    }

    public long getMonthlyRentVnd() {
        return monthlyRentVnd;
    }

    public Long getDepositVnd() {
        return depositVnd;
    }

    public long getDesiredMoveInMillis() {
        return desiredMoveInMillis;
    }

    public int getOccupantCount() {
        return occupantCount;
    }

    public String getNote() {
        return note;
    }

    public Status getStatus() {
        return status;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public boolean isDemo() {
        return demo;
    }

    public boolean canCancelDemo() {
        return demo && status == Status.PENDING;
    }

    public Booking cancelDemo() {
        if (!canCancelDemo()) {
            throw new IllegalStateException(
                    "Chỉ có thể hủy yêu cầu demo đang chờ duyệt."
            );
        }

        return copyWithStatus(Status.CANCELLED);
    }

    public boolean canReviewDemo() {
        return demo && status == Status.PENDING;
    }

    public Booking approveDemo() {
        if (!canReviewDemo()) {
            throw new IllegalStateException(
                    "Chỉ có thể duyệt yêu cầu demo đang chờ duyệt."
            );
        }

        return copyWithStatus(Status.APPROVED);
    }

    public Booking rejectDemo() {
        if (!canReviewDemo()) {
            throw new IllegalStateException(
                    "Chỉ có thể từ chối yêu cầu demo đang chờ duyệt."
            );
        }

        return copyWithStatus(Status.REJECTED);
    }

    public boolean canConfirmDemo() {
        return demo && status == Status.APPROVED;
    }

    public Booking confirmDemo() {
        if (!canConfirmDemo()) {
            throw new IllegalStateException(
                    "Chỉ có thể xác nhận yêu cầu demo đã được duyệt."
            );
        }

        return copyWithStatus(Status.CONFIRMED);
    }

    public boolean canCompleteDemo() {
        return demo && status == Status.CONFIRMED;
    }

    public Booking completeDemo() {
        if (!canCompleteDemo()) {
            throw new IllegalStateException(
                    "Chỉ có thể hoàn tất yêu cầu demo đã xác nhận."
            );
        }

        return copyWithStatus(Status.COMPLETED);
    }

    private Booking copyWithStatus(Status newStatus) {
        return new Booking(
                id,
                roomId,
                roomTitle,
                roomAddress,
                monthlyRentVnd,
                depositVnd,
                desiredMoveInMillis,
                occupantCount,
                note,
                newStatus,
                createdAtMillis,
                demo
        );
    }

    public String getMonthlyRentLabel() {
        return formatVnd(monthlyRentVnd) + "/tháng";
    }

    public String getDepositLabel() {
        if (depositVnd == null) {
            return "Chưa cung cấp";
        }

        if (depositVnd == 0) {
            return "Không yêu cầu cọc";
        }

        return formatVnd(depositVnd);
    }

    public String getDesiredMoveInLabel() {
        return formatDate(desiredMoveInMillis);
    }

    public String getCreatedAtLabel() {
        return formatDate(createdAtMillis);
    }

    public String getStatusLabel() {
        switch (status) {
            case PENDING:
                return "Chờ duyệt";
            case APPROVED:
                return "Đã duyệt";
            case CONFIRMED:
                return "Đã xác nhận";
            case COMPLETED:
                return "Hoàn tất";
            case CANCELLED:
                return "Đã hủy";
            case REJECTED:
                return "Bị từ chối";
            case EXPIRED:
                return "Hết hạn";
            default:
                return "Chưa xác định";
        }
    }

    public String getStatusDescription() {
        switch (status) {
            case PENDING:
                return "Yêu cầu đang chờ chủ trọ xem xét.";

            case APPROVED:
                return "Chủ trọ đã duyệt yêu cầu. "
                        + "Xem chi tiết để kiểm tra bước tiếp theo.";

            case CONFIRMED:
                return "Yêu cầu thuê đã được xác nhận. "
                        + "Theo dõi thông tin bàn giao trong chi tiết.";

            case COMPLETED:
                return "Quy trình thuê và bàn giao đã hoàn tất.";

            case CANCELLED:
                return "Yêu cầu thuê đã được hủy.";

            case REJECTED:
                return "Chủ trọ đã từ chối yêu cầu thuê.";

            case EXPIRED:
                return "Yêu cầu đã hết thời hạn xử lý.";

            default:
                return "Chưa có thông tin trạng thái.";
        }
    }

    private static String formatVnd(long amount) {
        NumberFormat formatter = NumberFormat.getIntegerInstance(
                new Locale("vi", "VN")
        );

        return formatter.format(amount) + " ₫";
    }

    private static String formatDate(long millis) {
        SimpleDateFormat formatter = new SimpleDateFormat(
                "dd/MM/yyyy",
                new Locale("vi", "VN")
        );

        return formatter.format(new Date(millis));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String requireText(
            String value,
            String fieldName
    ) {
        String normalized = normalize(value);

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    fieldName + " không được để trống."
            );
        }

        return normalized;
    }
}