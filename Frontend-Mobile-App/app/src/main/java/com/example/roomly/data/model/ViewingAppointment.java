package com.example.roomly.data.model;

/**
 * Dữ liệu lịch hẹn xem phòng dùng để hiển thị giao diện.
 */
public class ViewingAppointment {

    private final String roomTitle;
    private final String roomAddress;
    private final int roomImageResId;

    // Thời điểm hẹn, tính bằng mili giây.
    private final long appointmentTimeMillis;

    private final String note;

    /**
     * Khởi tạo lịch xem phòng từ phòng được chọn,
     * thời gian hẹn và lời nhắn của người dùng.
     */
    public ViewingAppointment(
            RoomCard room,
            long appointmentTimeMillis,
            String note
    ) {
        this.roomTitle = room.getTitle();
        this.roomAddress = room.getAddress();
        this.roomImageResId = room.getImageResId();
        this.appointmentTimeMillis = appointmentTimeMillis;
        this.note = note;
    }

    /**
     * Trả về tên phòng được hẹn xem.
     */
    public String getRoomTitle() {
        return roomTitle;
    }

    /**
     * Trả về địa chỉ phòng.
     */
    public String getRoomAddress() {
        return roomAddress;
    }

    /**
     * Trả về mã ảnh phòng trong drawable.
     */
    public int getRoomImageResId() {
        return roomImageResId;
    }

    /**
     * Trả về thời điểm xem phòng để hiển thị và sắp xếp lịch.
     */
    public long getAppointmentTimeMillis() {
        return appointmentTimeMillis;
    }

    /**
     * Trả về lời nhắn đi kèm lịch hẹn.
     */
    public String getNote() {
        return note;
    }
}