package com.example.roomly.data.model;

/**
 * Bản nháp đang soạn trên điện thoại.
 * Không phải bài đăng đã lưu trên backend.
 */
public final class ListingDraft {

    private final String userId;
    private final String roomId;
    private final String title;
    private final String priceText;
    private final String depositText;
    private final String description;
    private final long updatedAtMillis;

    public ListingDraft(
            String userId,
            String roomId,
            String title,
            String priceText,
            String depositText,
            String description,
            long updatedAtMillis
    ) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Bản nháp cần có mã tài khoản."
            );
        }

        if (roomId == null || roomId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Bản nháp cần có mã phòng."
            );
        }

        this.userId = userId.trim();
        this.roomId = roomId.trim();
        this.title = textOrEmpty(title);
        this.priceText = textOrEmpty(priceText);
        this.depositText = textOrEmpty(depositText);
        this.description = textOrEmpty(description);
        this.updatedAtMillis = updatedAtMillis;
    }

    public String getUserId() {
        return userId;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getTitle() {
        return title;
    }

    public String getPriceText() {
        return priceText;
    }

    public String getDepositText() {
        return depositText;
    }

    public String getDescription() {
        return description;
    }

    public long getUpdatedAtMillis() {
        return updatedAtMillis;
    }

    /**
     * Dùng để tránh lưu bản nháp không có nội dung.
     * Chuỗi "0" vẫn được xem là đã nhập.
     */
    public boolean isEmpty() {
        return title.trim().isEmpty()
                && priceText.trim().isEmpty()
                && depositText.trim().isEmpty()
                && description.trim().isEmpty();
    }

    private static String textOrEmpty(String value) {
        return value == null ? "" : value;
    }
}