package com.example.roomly.data.model;

import androidx.annotation.Nullable;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Dữ liệu bài đăng dùng để thử giao diện kiểm duyệt.
 * Không phải DTO gửi trực tiếp đến backend.
 */
public class AdminListing {

    /**
     * Các trạng thái phục vụ hiển thị giao diện mẫu.
     */
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
    private final String description;

    private final String hostName;
    private final String unitCode;
    private final String address;

    private final String imageUri;
    private final Status status;
    private final String moderationReason;

    /**
     * Khởi tạo bài đăng mẫu và thông tin phục vụ kiểm duyệt.
     * Ảnh có thể để null khi bài đăng chưa có ảnh.
     */
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

        this.id = id;
        this.hostId = hostId;
        this.roomId = roomId;
        this.title = title;
        this.monthlyRent = monthlyRent;
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

    /**
     * Trả về mã bài đăng.
     */
    public String getId() {
        return id;
    }

    /**
     * Trả về mã tài khoản chủ trọ sở hữu bài đăng.
     */
    public String getHostId() {
        return hostId;
    }

    /**
     * Trả về mã phòng liên kết với bài đăng.
     */
    public String getRoomId() {
        return roomId;
    }

    /**
     * Trả về tiêu đề bài đăng.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Trả về giá thuê theo tháng, đơn vị đồng.
     */
    public long getMonthlyRent() {
        return monthlyRent;
    }

    /**
     * Định dạng giá thuê để hiển thị theo cách viết tiếng Việt.
     */
    public String getFormattedPrice() {
        NumberFormat formatter = NumberFormat.getNumberInstance(
                new Locale("vi", "VN")
        );

        return formatter.format(monthlyRent) + " ₫/tháng";
    }

    /**
     * Trả về toàn bộ nội dung bài đăng.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Trả về tên chủ trọ để hiển thị.
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * Trả về mã phòng.
     */
    public String getUnitCode() {
        return unitCode;
    }

    /**
     * Trả về địa chỉ phòng.
     */
    public String getAddress() {
        return address;
    }

    /**
     * Trả về URI ảnh hoặc null nếu chưa có ảnh.
     */
    @Nullable
    public String getImageUri() {
        return imageUri;
    }

    /**
     * Trả về trạng thái bài đăng trong dữ liệu mẫu.
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Chuyển trạng thái thành nội dung tiếng Việt để hiển thị.
     */
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

    /**
     * Trả về lý do kiểm duyệt, hoặc chuỗi trống nếu chưa có.
     */
    public String getModerationReason() {
        return moderationReason;
    }

    /**
     * Tạo bản sao với trạng thái và lý do kiểm duyệt mới.
     * Repository sẽ kiểm tra quyền và điều kiện trước khi sử dụng.
     */
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