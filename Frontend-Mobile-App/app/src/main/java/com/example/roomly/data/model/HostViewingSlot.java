package com.example.roomly.data.model;

/**
 * Lưu thông tin một khung giờ xem phòng do chủ trọ thiết lập.
 * Thời gian được lưu bằng mili giây tính từ Unix epoch.
 */
public class HostViewingSlot {

    private final String id;
    private final String ownerId;
    private final String roomId;

    private final long startTimeMillis;
    private final long endTimeMillis;

    private final boolean open;

    /**
     * Khởi tạo khung giờ xem phòng.
     * Thời gian kết thúc phải lớn hơn thời gian bắt đầu.
     */
    public HostViewingSlot(
            String id,
            String ownerId,
            String roomId,
            long startTimeMillis,
            long endTimeMillis,
            boolean open
    ) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Khung giờ cần có mã định danh."
            );
        }

        if (ownerId == null || ownerId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Khung giờ cần có mã chủ trọ."
            );
        }

        if (roomId == null || roomId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Khung giờ cần liên kết với một phòng."
            );
        }

        if (endTimeMillis <= startTimeMillis) {
            throw new IllegalArgumentException(
                    "Giờ kết thúc phải sau giờ bắt đầu."
            );
        }

        this.id = id;
        this.ownerId = ownerId;
        this.roomId = roomId;
        this.startTimeMillis = startTimeMillis;
        this.endTimeMillis = endTimeMillis;
        this.open = open;
    }

    /**
     * Trả về mã định danh của khung giờ.
     */
    public String getId() {
        return id;
    }

    /**
     * Trả về mã tài khoản chủ trọ sở hữu khung giờ.
     */
    public String getOwnerId() {
        return ownerId;
    }

    /**
     * Trả về mã phòng được liên kết với khung giờ.
     */
    public String getRoomId() {
        return roomId;
    }

    /**
     * Trả về thời điểm bắt đầu xem phòng.
     */
    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    /**
     * Trả về thời điểm kết thúc xem phòng.
     */
    public long getEndTimeMillis() {
        return endTimeMillis;
    }

    /**
     * Cho biết chủ trọ đang mở khung giờ để nhận lịch hay không.
     * Việc đặt lịch còn cần kiểm tra thời gian và các điều kiện khác.
     */
    public boolean isOpen() {
        return open;
    }

    /**
     * Tạo bản sao với trạng thái mở hoặc đóng mới.
     * Giữ nguyên mã, chủ sở hữu, phòng và thời gian của khung giờ.
     */
    public HostViewingSlot withOpen(boolean open) {
        return new HostViewingSlot(
                id,
                ownerId,
                roomId,
                startTimeMillis,
                endTimeMillis,
                open
        );
    }
}