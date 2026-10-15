package com.example.roomly.data.repository;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;

import com.example.roomly.BuildConfig;
import com.example.roomly.data.model.HostRoom;
import com.example.roomly.data.model.HostViewingAppointment;
import com.example.roomly.data.model.HostViewingSlot;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Cung cấp yêu cầu phía chủ trọ.
 * Yêu cầu người thuê gửi được đọc từ repository lịch chung.
 * Danh sách riêng chỉ dành cho nút tạo yêu cầu thử debug.
 */
public final class DemoHostAppointmentRepository {

    private static final DemoHostAppointmentRepository INSTANCE =
            new DemoHostAppointmentRepository();

    private final List<HostViewingAppointment> demoAppointments =
            new ArrayList<>();

    /** Khởi tạo danh sách yêu cầu debug trống. */
    private DemoHostAppointmentRepository() {
    }

    /** Trả về repository dùng chung. */
    public static DemoHostAppointmentRepository getInstance() {
        return INSTANCE;
    }

    /** Ghép yêu cầu người thuê gửi và yêu cầu debug của đúng chủ trọ. */
    @MainThread
    public List<HostViewingAppointment> getMyAppointments() {
        List<HostViewingAppointment> results = new ArrayList<>();

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()
                || !session.hasRole(UserRole.HOST)) {
            return results;
        }

        results.addAll(
                DemoAppointmentRepository.getInstance()
                        .getMyHostAppointments()
        );

        for (HostViewingAppointment appointment : demoAppointments) {
            if (!session.getUserId().equals(appointment.getOwnerId())) {
                continue;
            }

            HostRoom room = DemoHostRoomRepository.getInstance()
                    .getMyRoomById(appointment.getRoomId());

            if (room != null
                    && room.getOwnerId().equals(appointment.getOwnerId())) {
                results.add(appointment);
            }
        }

        results.sort(
                Comparator.comparingLong(
                        HostViewingAppointment::getStartTimeMillis
                )
        );

        return results;
    }

    /** Tìm yêu cầu thuộc đúng chủ trọ hiện tại. */
    @Nullable
    @MainThread
    public HostViewingAppointment getMyAppointmentById(
            @Nullable String appointmentId
    ) {
        if (appointmentId == null) {
            return null;
        }

        for (HostViewingAppointment appointment : getMyAppointments()) {
            if (appointmentId.equals(appointment.getId())) {
                return appointment;
            }
        }

        return null;
    }

    /** Xác nhận yêu cầu đang chờ. */
    @MainThread
    public HostViewingAppointment confirmAppointment(String id) {
        return applyDecision(id, true, "");
    }

    /** Từ chối yêu cầu đang chờ với lý do bắt buộc. */
    @MainThread
    public HostViewingAppointment rejectAppointment(
            String id,
            String reason
    ) {
        return applyDecision(id, false, reason);
    }

    /**
     * Chuyển yêu cầu người thuê sang repository chung để cập nhật.
     * Với yêu cầu debug riêng, cập nhật danh sách debug.
     */
    private HostViewingAppointment applyDecision(
            String id,
            boolean confirmed,
            String reason
    ) {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()
                || !session.hasRole(UserRole.HOST)
                || !session.isEmailVerified()) {
            throw new IllegalStateException(
                    "Bạn cần quyền chủ trọ và email đã xác minh."
            );
        }

        HostViewingAppointment current = getMyAppointmentById(id);

        if (current == null) {
            throw new IllegalStateException(
                    "Không tìm thấy yêu cầu thuộc phòng của bạn."
            );
        }

        if (current.getStatus()
                != HostViewingAppointment.Status.PENDING) {
            throw new IllegalStateException(
                    "Yêu cầu đã được xử lý hoặc đã hủy."
            );
        }

        if (current.getStartTimeMillis() <= System.currentTimeMillis()) {
            throw new IllegalStateException(
                    "Lịch đã bắt đầu hoặc đã qua."
            );
        }

        String cleanReason = reason == null ? "" : reason.trim();

        if (!confirmed && cleanReason.isEmpty()) {
            throw new IllegalArgumentException(
                    "Bạn cần nhập lý do từ chối."
            );
        }

        for (int index = 0; index < demoAppointments.size(); index++) {
            if (demoAppointments.get(index).getId().equals(id)) {
                HostViewingAppointment updated = current.withDecision(
                        confirmed
                                ? HostViewingAppointment.Status.CONFIRMED
                                : HostViewingAppointment.Status.REJECTED,
                        confirmed ? "" : cleanReason
                );

                demoAppointments.set(index, updated);
                return updated;
            }
        }

        return DemoAppointmentRepository.getInstance()
                .processHostDecision(id, confirmed, cleanReason);
    }

    /** Tạo yêu cầu debug cho khung giờ mở gần nhất của phòng mình. */
    @MainThread
    public HostViewingAppointment createDemoRequest(String roomId) {
        if (!BuildConfig.DEBUG) {
            throw new IllegalStateException(
                    "Chỉ được tạo yêu cầu thử trong bản debug."
            );
        }

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()
                || !session.hasRole(UserRole.HOST)
                || !session.isEmailVerified()) {
            throw new IllegalStateException(
                    "Bạn cần quyền chủ trọ và email đã xác minh."
            );
        }

        HostRoom room = DemoHostRoomRepository.getInstance()
                .getMyRoomById(roomId);

        if (room == null) {
            throw new IllegalStateException(
                    "Không tìm thấy phòng thuộc tài khoản của bạn."
            );
        }

        HostViewingSlot selected = null;

        for (HostViewingSlot slot :
                DemoHostViewingRepository.getInstance()
                        .getMySlots(roomId)) {
            if (slot.isOpen()
                    && slot.getStartTimeMillis()
                    > System.currentTimeMillis()
                    && !hasDemoRequestForSlot(slot)) {
                selected = slot;
                break;
            }
        }

        if (selected == null) {
            throw new IllegalStateException(
                    "Hãy tạo khung giờ mở trong tương lai "
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
                        selected.getStartTimeMillis(),
                        selected.getEndTimeMillis(),
                        "Yêu cầu tạo bằng nút thử giao diện, "
                                + "không phải người thuê gửi.",
                        HostViewingAppointment.Status.PENDING,
                        ""
                );

        demoAppointments.add(appointment);
        return appointment;
    }

    /** Tránh tạo trùng yêu cầu debug cho cùng khung giờ. */
    private boolean hasDemoRequestForSlot(HostViewingSlot slot) {
        for (HostViewingAppointment appointment : demoAppointments) {
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