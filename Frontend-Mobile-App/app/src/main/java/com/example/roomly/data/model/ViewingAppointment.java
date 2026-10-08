package com.example.roomly.data.model;

import java.util.UUID;

/**
 * Lịch xem phòng phía người thuê.
 * Trạng thái dưới đây dùng cho luồng thử, chưa phải DTO của API.
 */
public final class ViewingAppointment {

    public enum Status {
        PENDING,
        CONFIRMED,
        REJECTED,
        CANCELLED
    }

    private final String id;
    private final String listingId;
    private final String roomId;
    private final String ownerId;
    private final String guestId;
    private final String guestName;
    private final String slotId;
    private final String unitCode;

    private final String roomTitle;
    private final String roomAddress;
    private final int roomImageResId;
    private final String roomImageUri;

    private final long appointmentTimeMillis;
    private final long endTimeMillis;
    private final String note;
    private final Status status;
    private final String decisionReason;

    /**
     * Giữ cách tạo lịch cũ trong lúc chuyển sang chọn khung giờ.
     * Lịch này chưa có người gửi hoặc khung giờ để đồng bộ phía chủ trọ.
     */
    public ViewingAppointment(
            RoomCard room,
            long appointmentTimeMillis,
            String note
    ) {
        this(
                UUID.randomUUID().toString(),
                room,
                "",
                "",
                "",
                appointmentTimeMillis,
                appointmentTimeMillis,
                note,
                Status.PENDING,
                ""
        );
    }

    /** Khởi tạo lịch với đầy đủ định danh và thông tin khung giờ. */
    public ViewingAppointment(
            String id,
            RoomCard room,
            String guestId,
            String guestName,
            String slotId,
            long startTimeMillis,
            long endTimeMillis,
            String note,
            Status status,
            String decisionReason
    ) {
        if (room == null || status == null) {
            throw new IllegalArgumentException(
                    "Thiếu thông tin phòng hoặc trạng thái lịch."
            );
        }

        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Thiếu mã lịch hẹn.");
        }

        if (endTimeMillis < startTimeMillis) {
            throw new IllegalArgumentException(
                    "Thời gian kết thúc không hợp lệ."
            );
        }

        String cleanGuestId = guestId == null ? "" : guestId;

        if (!room.getOwnerId().isEmpty()
                && room.getOwnerId().equals(cleanGuestId)) {
            throw new IllegalArgumentException(
                    "Không thể đặt lịch xem phòng của chính mình."
            );
        }

        this.id = id;
        this.listingId = room.getListingId();
        this.roomId = room.getRoomId();
        this.ownerId = room.getOwnerId();
        this.guestId = cleanGuestId;
        this.guestName = guestName == null ? "" : guestName;
        this.slotId = slotId == null ? "" : slotId;
        this.unitCode = room.getUnitCode();

        this.roomTitle = room.getTitle();
        this.roomAddress = room.getAddress();
        this.roomImageResId = room.getImageResId();
        this.roomImageUri = room.getImageUri();

        this.appointmentTimeMillis = startTimeMillis;
        this.endTimeMillis = endTimeMillis;
        this.note = note == null ? "" : note;
        this.status = status;
        this.decisionReason = decisionReason == null
                ? ""
                : decisionReason;
    }

    /** Trả về mã lịch hẹn dùng chung giữa hai phía. */
    public String getId() {
        return id;
    }

    /** Trả về mã bài đăng. */
    public String getListingId() {
        return listingId;
    }

    /** Trả về mã phòng. */
    public String getRoomId() {
        return roomId;
    }

    /** Trả về mã chủ trọ. */
    public String getOwnerId() {
        return ownerId;
    }

    /** Trả về mã người thuê gửi yêu cầu. */
    public String getGuestId() {
        return guestId;
    }

    /** Trả về tên người gửi. */
    public String getGuestName() {
        return guestName;
    }

    /** Trả về mã khung giờ được chọn. */
    public String getSlotId() {
        return slotId;
    }

    /** Trả về mã phòng hiển thị. */
    public String getUnitCode() {
        return unitCode;
    }

    /** Trả về tên phòng. */
    public String getRoomTitle() {
        return roomTitle;
    }

    /** Trả về địa chỉ phòng. */
    public String getRoomAddress() {
        return roomAddress;
    }

    /** Trả về ảnh drawable của phòng minh họa. */
    public int getRoomImageResId() {
        return roomImageResId;
    }

    /** Trả về URI ảnh phòng. */
    public String getRoomImageUri() {
        return roomImageUri;
    }

    /** Trả về thời gian bắt đầu để hiển thị và sắp xếp. */
    public long getAppointmentTimeMillis() {
        return appointmentTimeMillis;
    }

    /** Trả về thời gian kết thúc. */
    public long getEndTimeMillis() {
        return endTimeMillis;
    }

    /** Trả về ghi chú của người thuê. */
    public String getNote() {
        return note;
    }

    /** Trả về trạng thái lịch hẹn. */
    public Status getStatus() {
        return status;
    }

    /** Trả về lý do xử lý. */
    public String getDecisionReason() {
        return decisionReason;
    }

    /** Chuyển trạng thái thành chữ hiển thị. */
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

    /** Cho biết lịch chưa bắt đầu và còn ở trạng thái có thể hủy. */
    public boolean canCancel() {
        return appointmentTimeMillis > System.currentTimeMillis()
                && (status == Status.PENDING
                || status == Status.CONFIRMED);
    }

    /**
     * Tạo bản sao mang kết quả xử lý mới.
     * Repository phải kiểm tra quyền và chuyển trạng thái trước khi gọi.
     */
    public ViewingAppointment withStatus(
            Status newStatus,
            String reason
    ) {
        RoomCard room = new RoomCard(
                roomTitle,
                "",
                roomAddress,
                "",
                roomImageResId,
                false,
                RoomCard.RoomType.ROOM,
                roomId,
                ownerId,
                unitCode
        );

        room.setListingId(listingId);
        room.setImageUri(roomImageUri);

        return new ViewingAppointment(
                id,
                room,
                guestId,
                guestName,
                slotId,
                appointmentTimeMillis,
                endTimeMillis,
                note,
                newStatus,
                reason
        );
    }
}