package com.example.roomly.data.model;

/**
 * Thông tin yêu cầu xem phòng phía chủ trọ.
 * Trạng thái phục vụ dữ liệu thử, chưa phải DTO chính thức của API.
 */
public final class HostViewingAppointment {

    public enum Status {
        PENDING,
        CONFIRMED,
        REJECTED,
        CANCELLED
    }

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
    private final Status status;
    private final String decisionReason;

    /** Giữ constructor cũ cho chức năng tạo yêu cầu thử. */
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
            String legacyStatusLabel
    ) {
        this(
                id, ownerId, roomId, guestId,
                roomName, unitCode, guestName,
                startTimeMillis, endTimeMillis, note,
                Status.PENDING, ""
        );
    }

    /** Khởi tạo đầy đủ thông tin yêu cầu và trạng thái xử lý. */
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
            Status status,
            String decisionReason
    ) {
        if (isBlank(id) || isBlank(ownerId)
                || isBlank(roomId) || isBlank(guestId)) {
            throw new IllegalArgumentException(
                    "Thiếu mã yêu cầu, phòng hoặc tài khoản."
            );
        }

        if (ownerId.equals(guestId)) {
            throw new IllegalArgumentException(
                    "Chủ trọ không thể đặt lịch xem phòng của mình."
            );
        }

        if (endTimeMillis <= startTimeMillis) {
            throw new IllegalArgumentException(
                    "Thời gian kết thúc phải sau thời gian bắt đầu."
            );
        }

        if (status == null) {
            throw new IllegalArgumentException("Thiếu trạng thái yêu cầu.");
        }

        String reason = decisionReason == null
                ? "" : decisionReason.trim();

        if (status == Status.REJECTED && reason.isEmpty()) {
            throw new IllegalArgumentException(
                    "Yêu cầu bị từ chối phải có lý do."
            );
        }

        this.id = id;
        this.ownerId = ownerId;
        this.roomId = roomId;
        this.guestId = guestId;
        this.roomName = roomName == null ? "" : roomName;
        this.unitCode = unitCode == null ? "" : unitCode;
        this.guestName = guestName == null ? "" : guestName;
        this.startTimeMillis = startTimeMillis;
        this.endTimeMillis = endTimeMillis;
        this.note = note == null ? "" : note;
        this.status = status;
        this.decisionReason = reason;
    }

    /** Kiểm tra định danh bị thiếu. */
    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /** Trả về mã yêu cầu. */
    public String getId() {
        return id;
    }

    /** Trả về mã chủ trọ. */
    public String getOwnerId() {
        return ownerId;
    }

    /** Trả về mã phòng. */
    public String getRoomId() {
        return roomId;
    }

    /** Trả về mã người gửi. */
    public String getGuestId() {
        return guestId;
    }

    /** Trả về tên phòng. */
    public String getRoomName() {
        return roomName;
    }

    /** Trả về mã phòng hiển thị. */
    public String getUnitCode() {
        return unitCode;
    }

    /** Trả về tên người gửi. */
    public String getGuestName() {
        return guestName;
    }

    /** Trả về thời điểm bắt đầu. */
    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    /** Trả về thời điểm kết thúc. */
    public long getEndTimeMillis() {
        return endTimeMillis;
    }

    /** Trả về ghi chú của người gửi. */
    public String getNote() {
        return note;
    }

    /** Trả về trạng thái để kiểm tra thao tác được phép. */
    public Status getStatus() {
        return status;
    }

    /** Trả về lý do xử lý. */
    public String getDecisionReason() {
        return decisionReason;
    }

    /** Chuyển trạng thái thành nội dung hiển thị. */
    public String getStatusLabel() {
        switch (status) {
            case CONFIRMED:
                return "Đã xác nhận";
            case REJECTED:
                return "Đã từ chối";
            case CANCELLED:
                return "Đã hủy";
            case PENDING:
            default:
                return "Chờ xác nhận";
        }
    }

    /** Tạo bản sao sau khi chủ trọ xử lý một yêu cầu đang chờ. */
    public HostViewingAppointment withDecision(
            Status newStatus,
            String reason
    ) {
        if (status != Status.PENDING) {
            throw new IllegalStateException("Yêu cầu này đã được xử lý.");
        }

        if (newStatus != Status.CONFIRMED
                && newStatus != Status.REJECTED) {
            throw new IllegalArgumentException(
                    "Trạng thái xử lý không hợp lệ."
            );
        }

        return new HostViewingAppointment(
                id, ownerId, roomId, guestId,
                roomName, unitCode, guestName,
                startTimeMillis, endTimeMillis, note,
                newStatus, reason
        );
    }
}