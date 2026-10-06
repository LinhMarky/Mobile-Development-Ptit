package com.example.roomly.data.model;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Bản nháp bài đăng dùng để thử giao diện chủ trọ.
 * Chưa đại diện cho bài đăng đã được duyệt hoặc hiển thị công khai.
 */
public class HostListing {

    private final String id;
    private final String ownerId;
    private final String roomId;
    private final String title;
    private final long monthlyRent;
    private final String description;
    private final long createdAtMillis;

    /**
     * Khởi tạo bản nháp bài đăng gắn với một phòng.
     * Mã chủ sở hữu và mã phòng được giữ cố định.
     */
    public HostListing(
            String id,
            String ownerId,
            String roomId,
            String title,
            long monthlyRent,
            String description,
            long createdAtMillis
    ) {
        this.id = id;
        this.ownerId = ownerId;
        this.roomId = roomId;
        this.title = title;
        this.monthlyRent = monthlyRent;
        this.description = description;
        this.createdAtMillis = createdAtMillis;
    }

    /**
     * Trả về mã định danh bản nháp.
     */
    public String getId() {
        return id;
    }

    /**
     * Trả về mã tài khoản sở hữu bài đăng.
     */
    public String getOwnerId() {
        return ownerId;
    }

    /**
     * Trả về mã phòng được liên kết với bài đăng.
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
     * Trả về giá thuê mỗi tháng bằng đồng Việt Nam.
     */
    public long getMonthlyRent() {
        return monthlyRent;
    }

    /**
     * Định dạng giá thuê để hiển thị.
     * Ví dụ: 3500000 thành 3.500.000 ₫/tháng.
     */
    public String getFormattedPrice() {
        NumberFormat formatter = NumberFormat.getIntegerInstance(
                new Locale("vi", "VN")
        );

        return formatter.format(monthlyRent) + " ₫/tháng";
    }

    /**
     * Trả về nội dung bài đăng.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Trả về thời điểm tạo bản nháp theo mili giây.
     */
    public long getCreatedAtMillis() {
        return createdAtMillis;
    }
}