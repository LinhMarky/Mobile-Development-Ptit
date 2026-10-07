package com.example.roomly.data.repository;

import androidx.annotation.MainThread;

import com.example.roomly.BuildConfig;
import com.example.roomly.data.model.HostRoom;
import com.example.roomly.data.model.HostViewingAppointment;
import com.example.roomly.data.model.HostViewingSlot;
import com.example.roomly.data.model.SessionState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Quản lý yêu cầu xem phòng mẫu phía chủ trọ.
 * Dữ liệu chỉ nằm trong bộ nhớ, chưa kết nối backend.
 */
public class DemoHostAppointmentRepository {

    private static final DemoHostAppointmentRepository INSTANCE =
            new DemoHostAppointmentRepository();

    private final List<HostViewingAppointment> appointments =
            new ArrayList<>();

    /**
     * Khởi tạo repository, ban đầu chưa có yêu cầu mẫu.
     */
    private DemoHostAppointmentRepository() {
    }

    /**
     * Trả về repository dùng chung trong ứng dụng.
     */
    public static DemoHostAppointmentRepository getInstance() {
        return INSTANCE;
    }

    /**
     * Lấy yêu cầu mẫu của các phòng thuộc chủ trọ hiện tại.
     * Sắp xếp lịch hẹn từ sớm đến muộn.
     */
    @MainThread
    public List<HostViewingAppointment> getMyAppointments() {
        List<HostViewingAppointment> results = new ArrayList<>();

        List<HostRoom> myRooms =
                DemoHostRoomRepository.getInstance().getMyRooms();

        for (HostViewingAppointment appointment : appointments) {
            for (HostRoom room : myRooms) {
                boolean belongsToRoom =
                        room.getId().equals(appointment.getRoomId());

                boolean belongsToOwner =
                        room.getOwnerId().equals(
                                appointment.getOwnerId()
                        );

                if (belongsToRoom && belongsToOwner) {
                    results.add(appointment);
                    break;
                }
            }
        }

        results.sort(
                Comparator.comparingLong(
                        HostViewingAppointment::getStartTimeMillis
                )
        );

        return results;
    }

    /**
     * Tạo một yêu cầu thử cho khung giờ đang mở gần nhất của phòng.
     * Chỉ dùng trong bản debug để kiểm tra giao diện chủ trọ.
     * Người gửi mẫu có mã riêng, khác mã tài khoản chủ trọ.
     */
    @MainThread
    public HostViewingAppointment createDemoRequest(String roomId) {
        if (!BuildConfig.DEBUG) {
            throw new IllegalStateException(
                    "Chức năng tạo yêu cầu thử chỉ dùng trong bản debug."
            );
        }

        SessionState session =
                SessionRepository.getInstance().getCurrentSession();

        if (!session.isLoggedIn()) {
            throw new IllegalStateException(
                    "Bạn cần đăng nhập để thử giao diện."
            );
        }

        if (!session.isEmailVerified()) {
            throw new IllegalStateException(
                    "Bạn cần xác minh email trước khi thử."
            );
        }

        HostRoom room = DemoHostRoomRepository.getInstance()
                .getMyRoomById(roomId);

        if (room == null) {
            throw new IllegalStateException(
                    "Không tìm thấy phòng thuộc tài khoản của bạn."
            );
        }

        HostViewingSlot selectedSlot = null;

        List<HostViewingSlot> slots =
                DemoHostViewingRepository.getInstance()
                        .getMySlots(roomId);

        long now = System.currentTimeMillis();

        // Danh sách đã được sắp xếp từ sớm đến muộn.
        for (HostViewingSlot slot : slots) {
            if (slot.isOpen()
                    && slot.getStartTimeMillis() > now
                    && !hasDemoRequestForSlot(slot)) {
                selectedSlot = slot;
                break;
            }
        }

        if (selectedSlot == null) {
            throw new IllegalStateException(
                    "Hãy tạo một khung giờ đang mở trong tương lai "
                            + "chưa có yêu cầu thử."
            );
        }

        HostViewingAppointment appointment =
                new HostViewingAppointment(
                        UUID.randomUUID().toString(),
                        room.getOwnerId(),
                        room.getId(),
                        "debug-guest-" + UUID.randomUUID(),
                        room.getName(),
                        room.getUnitCode(),
                        "Người xem phòng mẫu",
                        selectedSlot.getStartTimeMillis(),
                        selectedSlot.getEndTimeMillis(),
                        "Đây là ghi chú thử để kiểm tra giao diện. "
                                + "Không phải yêu cầu từ người dùng thật.",
                        "Yêu cầu mẫu • Chỉ để thử giao diện"
                );

        appointments.add(appointment);

        return appointment;
    }

    /**
     * Kiểm tra khung giờ đã có yêu cầu thử hay chưa.
     * Quy tắc này chỉ tránh tạo trùng dữ liệu demo khi bấm nhiều lần.
     */
    private boolean hasDemoRequestForSlot(HostViewingSlot slot) {
        for (HostViewingAppointment appointment : appointments) {
            if (appointment.getOwnerId().equals(slot.getOwnerId())
                    && appointment.getRoomId().equals(slot.getRoomId())
                    && appointment.getStartTimeMillis()
                    == slot.getStartTimeMillis()
                    && appointment.getEndTimeMillis()
                    == slot.getEndTimeMillis()) {
                return true;
            }
        }

        return false;
    }
}