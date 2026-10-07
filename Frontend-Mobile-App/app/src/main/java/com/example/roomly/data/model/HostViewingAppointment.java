package com.example.roomly.data.model;

/**
 * Dữ liệu hiển thị một yêu cầu xem phòng phía chủ trọ.
 * Tên và mã phòng được giữ cùng yêu cầu để hiển thị thông tin lịch hẹn.
 */
public class HostViewingAppointment {

    private final String id;
    private final String ownerId;
    private final String roomId;
    private final String guestId;

    private final String roomName;
    private final String unitCode;
    private final String guestName;

    private final long startTimeMillis;
    private final long endTimeMillis;

    private final String note;
    private final String statusLabel;

    /**
     * Khởi tạo thông tin yêu cầu xem phòng dùng cho giao diện mẫu.
     */
    public HostViewingAppointment(
            String id,
            String ownerId,
            String roomId,
            String guestId,
            String roomName,
            String unitCode,
            String guestName,
            long startTimeMillis,
            long endTimeMillis,
            String note,
            String statusLabel
    ) {
        if (endTimeMillis <= startTimeMillis) {
            throw new IllegalArgumentException(
                    "Thời gian kết thúc phải sau thời gian bắt đầu."
            );
        }

        if (ownerId != null && ownerId.equals(guestId)) {
            throw new IllegalArgumentException(
                    "Chủ trọ không thể đặt lịch xem phòng của mình."
            );
        }

        this.id = id;
        this.ownerId = ownerId;
        this.roomId = roomId;
        this.guestId = guestId;
        this.roomName = roomName;
        this.unitCode = unitCode;
        this.guestName = guestName;
        this.startTimeMillis = startTimeMillis;
        this.endTimeMillis = endTimeMillis;
        this.note = note == null ? "" : note;
        this.statusLabel = statusLabel == null ? "" : statusLabel;
    }

    /**
     * Trả về mã yêu cầu xem phòng.
     */
    public String getId() {
        return id;
    }

    /**
     * Trả về mã tài khoản chủ trọ nhận yêu cầu.
     */
    public String getOwnerId() {
        return ownerId;
    }

    /**
     * Trả về mã phòng được yêu cầu xem.
     */
    public String getRoomId() {
        return roomId;
    }

    /**
     * Trả về mã tài khoản gửi yêu cầu.
     */
    public String getGuestId() {
        return guestId;
    }

    /**
     * Trả về tên phòng được lưu cùng yêu cầu.
     */
    public String getRoomName() {
        return roomName;
    }

    /**
     * Trả về mã phòng được lưu cùng yêu cầu.
     */
    public String getUnitCode() {
        return unitCode;
    }

    /**
     * Trả về tên người gửi yêu cầu.
     */
    public String getGuestName() {
        return guestName;
    }

    /**
     * Trả về thời gian bắt đầu lịch hẹn.
     */
    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    /**
     * Trả về thời gian kết thúc lịch hẹn.
     */
    public long getEndTimeMillis() {
        return endTimeMillis;
    }

    /**
     * Trả về ghi chú, hoặc chuỗi trống nếu không có ghi chú.
     */
    public String getNote() {
        return note;
    }

    /**
     * Trả về nội dung trạng thái để hiển thị trên giao diện.
     */
    public String getStatusLabel() {
        return statusLabel;
    }
}