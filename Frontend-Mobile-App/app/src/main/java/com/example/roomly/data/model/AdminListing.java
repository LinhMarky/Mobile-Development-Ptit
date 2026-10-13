package com.example.roomly.data.model;

import androidx.annotation.Nullable;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Dữ liệu bài đăng dùng để thử giao diện kiểm duyệt.
 * Không phải DTO gửi trực tiếp đến backend.
 */
public class AdminListing {

    public enum Status {
        PENDING,
        PUBLISHED,
        HIDDEN,
        REJECTED
    }

    private final String id;
    private final String hostId;
    private final String roomId;
    private final String title;
    private final long monthlyRent;

    @Nullable
    private final Long depositVnd;

    private final String description;
    private final String hostName;
    private final String unitCode;
    private final String address;

    @Nullable
    private final String imageUri;

    private final Status status;
    private final String moderationReason;

    // Giữ tương thích với constructor cũ.
    public AdminListing(
            String id,
            String hostId,
            String roomId,
            String title,
            long monthlyRent,
            String description,
            String hostName,
            String unitCode,
            String address,
            @Nullable String imageUri,
            Status status,
            @Nullable String moderationReason
    ) {
        this(
                id,
                hostId,
                roomId,
                title,
                monthlyRent,
                null,
                description,
                hostName,
                unitCode,
                address,
                imageUri,
                status,
                moderationReason
        );
    }

    // Constructor có thêm tiền cọc.
    public AdminListing(
            String id,
            String hostId,
            String roomId,
            String title,
            long monthlyRent,
            @Nullable Long depositVnd,
            String description,
            String hostName,
            String unitCode,
            String address,
            @Nullable String imageUri,
            Status status,
            @Nullable String moderationReason
    ) {
        if (status == null) {
            throw new IllegalArgumentException(
                    "Bài đăng cần có trạng thái."
            );
        }

        if (monthlyRent <= 0) {
            throw new IllegalArgumentException(
                    "Giá thuê phải lớn hơn 0."
            );
        }

        if (depositVnd != null && depositVnd < 0) {
            throw new IllegalArgumentException(
                    "Tiền cọc không được nhỏ hơn 0."
            );
        }

        this.id = id;
        this.hostId = hostId;
        this.roomId = roomId;
        this.title = title;
        this.monthlyRent = monthlyRent;
        this.depositVnd = depositVnd;
        this.description = description;
        this.hostName = hostName;
        this.unitCode = unitCode;
        this.address = address;
        this.imageUri = imageUri;
        this.status = status;
        this.moderationReason = moderationReason == null
                ? ""
                : moderationReason;
    }

    public String getId() {
        return id;
    }

    public String getHostId() {
        return hostId;
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
        NumberFormat formatter = NumberFormat.getNumberInstance(
                new Locale("vi", "VN")
        );

        return formatter.format(monthlyRent) + " ₫/tháng";
    }

    @Nullable
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

        return NumberFormat.getIntegerInstance(
                new Locale("vi", "VN")
        ).format(depositVnd) + " ₫";
    }

    public String getDescription() {
        return description;
    }

    public String getHostName() {
        return hostName;
    }

    public String getUnitCode() {
        return unitCode;
    }

    public String getAddress() {
        return address;
    }

    @Nullable
    public String getImageUri() {
        return imageUri;
    }

    public Status getStatus() {
        return status;
    }

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

            default:
                return "";
        }
    }

    public String getModerationReason() {
        return moderationReason;
    }

    public AdminListing withModeration(
            Status newStatus,
            @Nullable String reason
    ) {
        return new AdminListing(
                id,
                hostId,
                roomId,
                title,
                monthlyRent,
                depositVnd,
                description,
                hostName,
                unitCode,
                address,
                imageUri,
                newStatus,
                reason
        );
    }
}