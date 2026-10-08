package com.example.roomly.data.model;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Bài đăng phía chủ trọ trong dữ liệu mẫu.
 * Giữ nguyên mã bài đăng, phòng và chủ sở hữu khi đổi trạng thái.
 */
public final class HostListing {

    public enum Status {
        DRAFT,
        PENDING,
        PUBLISHED,
        HIDDEN,
        REJECTED
    }

    private final String id;
    private final String ownerId;
    private final String roomId;
    private final String title;
    private final long monthlyRent;
    private final String description;
    private final long createdAtMillis;
    private final Status status;
    private final String moderationReason;

    /** Giữ constructor cũ; bài mới tạo mặc định là bản nháp. */
    public HostListing(
            String id,
            String ownerId,
            String roomId,
            String title,
            long monthlyRent,
            String description,
            long createdAtMillis
    ) {
        this(
                id,
                ownerId,
                roomId,
                title,
                monthlyRent,
                description,
                createdAtMillis,
                Status.DRAFT,
                ""
        );
    }

    /** Khởi tạo đầy đủ thông tin và trạng thái bài đăng. */
    public HostListing(
            String id,
            String ownerId,
            String roomId,
            String title,
            long monthlyRent,
            String description,
            long createdAtMillis,
            Status status,
            String moderationReason
    ) {
        if (status == null) {
            throw new IllegalArgumentException(
                    "Thiếu trạng thái bài đăng."
            );
        }

        this.id = id;
        this.ownerId = ownerId;
        this.roomId = roomId;
        this.title = title;
        this.monthlyRent = monthlyRent;
        this.description = description;
        this.createdAtMillis = createdAtMillis;
        this.status = status;
        this.moderationReason = moderationReason == null
                ? ""
                : moderationReason;
    }

    /** Trả về mã bài đăng. */
    public String getId() {
        return id;
    }

    /** Trả về mã chủ sở hữu. */
    public String getOwnerId() {
        return ownerId;
    }

    /** Trả về mã phòng liên kết. */
    public String getRoomId() {
        return roomId;
    }

    /** Trả về tiêu đề. */
    public String getTitle() {
        return title;
    }

    /** Trả về giá thuê theo tháng bằng đồng Việt Nam. */
    public long getMonthlyRent() {
        return monthlyRent;
    }

    /** Định dạng giá thuê để hiển thị. */
    public String getFormattedPrice() {
        return NumberFormat.getIntegerInstance(
                new Locale("vi", "VN")
        ).format(monthlyRent) + " ₫/tháng";
    }

    /** Trả về nội dung bài đăng. */
    public String getDescription() {
        return description;
    }

    /** Trả về thời điểm tạo bài đăng. */
    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    /** Trả về trạng thái hiện tại. */
    public Status getStatus() {
        return status;
    }

    /** Trả về lý do kiểm duyệt. */
    public String getModerationReason() {
        return moderationReason;
    }

    /** Chuyển trạng thái thành nội dung hiển thị. */
    public String getStatusLabel() {
        switch (status) {
            case PENDING:
                return "Chờ duyệt";
            case PUBLISHED:
                return "Đang hiển thị";
            case HIDDEN:
                return "Đã ẩn";
            case REJECTED:
                return "Đã từ chối";
            case DRAFT:
            default:
                return "Bản nháp";
        }
    }

    /** Tạo bản sao mang trạng thái kiểm duyệt mới. */
    public HostListing withModeration(
            Status newStatus,
            String reason
    ) {
        return new HostListing(
                id,
                ownerId,
                roomId,
                title,
                monthlyRent,
                description,
                createdAtMillis,
                newStatus,
                reason
        );
    }
}