package com.example.roomly.data.model;

import java.text.NumberFormat;
import java.util.Locale;

public final class HostListing {

    public enum Status {
        DRAFT,
        PENDING_REVIEW,
        PUBLISHED,
        HIDDEN,
        REJECTED,
        SUSPENDED,
        EXPIRED,
        ARCHIVED;

        public boolean canEdit() {
            return this == DRAFT || this == REJECTED || this == HIDDEN || this == EXPIRED || this == PUBLISHED;
        }

        public boolean canSubmit() {
            return this == DRAFT || this == REJECTED || this == HIDDEN || this == EXPIRED;
        }

        public boolean canHide() {
            return this == PUBLISHED;
        }
    }

    private final String id;
    private final String ownerId;
    private final String roomId;
    private final String title;
    private final long monthlyRent;
    private final Long depositVnd;
    private final String description;
    private final long createdAtMillis;
    private final Status status;
    private final String moderationReason;

    // Giữ tương thích với constructor cũ.
    public HostListing(
            String id,
            String ownerId,
            String roomId,
            String title,
            long monthlyRent,
            String description,
            long createdAtMillis) {
        this(
                id,
                ownerId,
                roomId,
                title,
                monthlyRent,
                null,
                description,
                createdAtMillis,
                Status.DRAFT,
                "");
    }

    // Tạo bản nháp có thông tin tiền cọc.
    public HostListing(
            String id,
            String ownerId,
            String roomId,
            String title,
            long monthlyRent,
            Long depositVnd,
            String description,
            long createdAtMillis) {
        this(
                id,
                ownerId,
                roomId,
                title,
                monthlyRent,
                depositVnd,
                description,
                createdAtMillis,
                Status.DRAFT,
                "");
    }

    // Giữ tương thích với constructor đầy đủ cũ.
    public HostListing(
            String id,
            String ownerId,
            String roomId,
            String title,
            long monthlyRent,
            String description,
            long createdAtMillis,
            Status status,
            String moderationReason) {
        this(
                id,
                ownerId,
                roomId,
                title,
                monthlyRent,
                null,
                description,
                createdAtMillis,
                status,
                moderationReason);
    }

    public HostListing(
            String id,
            String ownerId,
            String roomId,
            String title,
            long monthlyRent,
            Long depositVnd,
            String description,
            long createdAtMillis,
            Status status,
            String moderationReason) {
        if (status == null) {
            throw new IllegalArgumentException("Thiếu trạng thái bài đăng.");
        }

        if (depositVnd != null && depositVnd < 0) {
            throw new IllegalArgumentException("Tiền cọc không được nhỏ hơn 0.");
        }

        this.id = id;
        this.ownerId = ownerId;
        this.roomId = roomId;
        this.title = title;
        this.monthlyRent = monthlyRent;
        this.depositVnd = depositVnd;
        this.description = description;
        this.createdAtMillis = createdAtMillis;
        this.status = status;
        this.moderationReason = moderationReason == null ? "" : moderationReason;
    }

    public String getId() {
        return id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getTitle() {
        return title;
    }

    public long getMonthlyRent() {
        return monthlyRent;
    }

    public String getFormattedPrice() {
        return NumberFormat.getIntegerInstance(new Locale("vi", "VN")).format(monthlyRent)
                + " ₫/tháng";
    }

    public Long getDepositVnd() {
        return depositVnd;
    }

    public String getFormattedDeposit() {
        if (depositVnd == null) {
            return "Chưa cung cấp";
        }

        if (depositVnd == 0) {
            return "Không yêu cầu cọc";
        }

        return NumberFormat.getIntegerInstance(new Locale("vi", "VN")).format(depositVnd) + " ₫";
    }

    public String getDescription() {
        return description;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public Status getStatus() {
        return status;
    }

    public String getModerationReason() {
        return moderationReason;
    }

    public String getStatusLabel() {
        switch (status) {
            case PENDING_REVIEW:
                return "Chờ duyệt";

            case PUBLISHED:
                return "Đang hiển thị";

            case HIDDEN:
                return "Đã ẩn";

            case SUSPENDED:
                return "Bị tạm ngưng";
            case EXPIRED:
                return "Đã hết hạn";
            case ARCHIVED:
                return "Đã lưu trữ";
            case REJECTED:
                return "Đã từ chối";

            case DRAFT:
            default:
                return "Bản nháp";
        }
    }

    public HostListing withModeration(Status newStatus, String reason) {
        return new HostListing(
                id,
                ownerId,
                roomId,
                title,
                monthlyRent,
                depositVnd,
                description,
                createdAtMillis,
                newStatus,
                reason);
    }
}
