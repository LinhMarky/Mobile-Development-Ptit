package com.example.roomly.data.repository;

import com.example.roomly.data.model.ViewingAppointment;

import java.util.ArrayList;
import java.util.List;

/**
 * Lưu các lịch hẹn mẫu dùng chung trong ứng dụng.
 * Dữ liệu được giữ trong bộ nhớ khi app đang chạy.
 */
public class DemoAppointmentRepository {

    private static final DemoAppointmentRepository INSTANCE =
            new DemoAppointmentRepository();

    private final List<ViewingAppointment> appointments =
            new ArrayList<>();

    /**
     * Khởi tạo repository với danh sách lịch hẹn trống.
     * Các màn hình truy cập qua getInstance().
     */
    private DemoAppointmentRepository() {
        // Chưa có lịch hẹn cho đến khi người dùng đặt lịch.
    }

    /**
     * Trả về repository lịch hẹn dùng chung.
     */
    public static DemoAppointmentRepository getInstance() {
        return INSTANCE;
    }

    /**
     * Thêm một lịch hẹn vào danh sách dữ liệu mẫu.
     */
    public void addAppointment(ViewingAppointment appointment) {
        appointments.add(appointment);
    }

    /**
     * Trả về bản sao danh sách, sắp xếp lịch sớm nhất trước.
     */
    public List<ViewingAppointment> getAppointments() {
        List<ViewingAppointment> result =
                new ArrayList<>(appointments);

        result.sort((first, second) ->
                Long.compare(
                        first.getAppointmentTimeMillis(),
                        second.getAppointmentTimeMillis()
                )
        );

        return result;
    }

    /**
     * Xóa lịch hẹn được chọn khỏi danh sách dữ liệu mẫu.
     */
    public void removeAppointment(ViewingAppointment appointment) {
        appointments.remove(appointment);
    }
}