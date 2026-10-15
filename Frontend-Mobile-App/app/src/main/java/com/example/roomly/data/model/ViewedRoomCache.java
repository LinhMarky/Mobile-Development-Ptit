package com.example.roomly.data.model;

/**
 * Bản sao thông tin phòng đã xem trên điện thoại.
 *
 * payloadJson chứa dữ liệu phục vụ hiển thị.
 * Trạng thái trong cache không xác nhận phòng còn trống.
 */
public final class ViewedRoomCache {

    private final String userId;
    private final String listingId;
    private final String roomId;
    private final String payloadJson;
    private final long cachedAtMillis;
    private final long viewedAtMillis;

    public ViewedRoomCache(
            String userId,
            String listingId,
            String roomId,
            String payloadJson,
            long cachedAtMillis,
            long viewedAtMillis
    ) {
        this.userId = requireText(userId, "mã tài khoản");
        this.listingId = requireText(listingId, "mã bài đăng");
        this.roomId = requireText(roomId, "mã phòng");
        this.payloadJson = requireText(
                payloadJson,
                "dữ liệu phòng"
        );

        if (cachedAtMillis < 0 || viewedAtMillis < 0) {
            throw new IllegalArgumentException(
                    "Thời gian cache không được âm."
            );
        }

        this.cachedAtMillis = cachedAtMillis;
        this.viewedAtMillis = viewedAtMillis;
    }

    public String getUserId() {
        return userId;
    }

    public String getListingId() {
        return listingId;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    /**
     * Thời điểm thông tin phòng được cập nhật vào cache.
     */
    public long getCachedAtMillis() {
        return cachedAtMillis;
    }

    /**
     * Thời điểm người dùng xem phòng gần nhất.
     * Xem lại cache không làm mới thời điểm cập nhật dữ liệu.
     */
    public long getViewedAtMillis() {
        return viewedAtMillis;
    }

    private static String requireText(
            String value,
            String fieldName
    ) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Thiếu " + fieldName + "."
            );
        }

        return value.trim();
    }
}