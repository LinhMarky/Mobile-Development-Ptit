package com.example.roomly.data.repository;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;

import com.example.roomly.data.model.HostRoom;
import com.example.roomly.data.model.HostViewingSlot;
import com.example.roomly.data.model.SessionState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Quản lý khung giờ xem phòng trong dữ liệu mẫu.
 * Dữ liệu chưa được gửi đến backend và mất khi tiến trình app kết thúc.
 */
public class DemoHostViewingRepository {

    private static final DemoHostViewingRepository INSTANCE =
            new DemoHostViewingRepository();

    private final List<HostViewingSlot> slots = new ArrayList<>();

    /**
     * Khởi tạo repository dùng chung, không tạo sẵn khung giờ giả.
     */
    private DemoHostViewingRepository() {
    }

    /**
     * Trả về repository khung giờ dùng chung trong ứng dụng.
     */
    public static DemoHostViewingRepository getInstance() {
        return INSTANCE;
    }

    /**
     * Lấy các khung giờ của một phòng thuộc chủ trọ hiện tại.
     * Sắp xếp theo thời gian bắt đầu từ sớm đến muộn.
     */
    @MainThread
    public List<HostViewingSlot> getMySlots(String roomId) {
        List<HostViewingSlot> results = new ArrayList<>();

        HostRoom room = DemoHostRoomRepository.getInstance()
                .getMyRoomById(roomId);

        if (room == null) {
            return results;
        }

        for (HostViewingSlot slot : slots) {
            if (slot.getRoomId().equals(room.getId())
                    && slot.getOwnerId().equals(room.getOwnerId())) {
                results.add(slot);
            }
        }

        results.sort(
                Comparator.comparingLong(
                        HostViewingSlot::getStartTimeMillis
                )
        );

        return results;
    }

    /**
     * Lấy một khung giờ nếu phòng liên kết thuộc chủ trọ hiện tại.
     * Trả về null nếu khung giờ không tồn tại hoặc không có quyền xem.
     */
    @Nullable
    @MainThread
    public HostViewingSlot getMySlotById(String slotId) {
        if (slotId == null || slotId.trim().isEmpty()) {
            return null;
        }

        for (HostViewingSlot slot : slots) {
            if (!slot.getId().equals(slotId)) {
                continue;
            }

            HostRoom room = DemoHostRoomRepository.getInstance()
                    .getMyRoomById(slot.getRoomId());

            if (room != null
                    && room.getOwnerId().equals(slot.getOwnerId())) {
                return slot;
            }
        }

        return null;
    }

    /**
     * Tạo khung giờ mới cho phòng thuộc chủ trọ hiện tại.
     * Khung giờ phải bắt đầu trong tương lai và kết thúc sau lúc bắt đầu.
     */
    @MainThread
    public HostViewingSlot createSlot(
            String roomId,
            long startTimeMillis,
            long endTimeMillis
    ) {
        HostRoom room = requireEditableRoom(roomId);

        if (startTimeMillis <= System.currentTimeMillis()) {
            throw new IllegalArgumentException(
                    "Thời gian bắt đầu phải ở trong tương lai."
            );
        }

        if (endTimeMillis <= startTimeMillis) {
            throw new IllegalArgumentException(
                    "Giờ kết thúc phải sau giờ bắt đầu."
            );
        }

        HostViewingSlot slot = new HostViewingSlot(
                UUID.randomUUID().toString(),
                room.getOwnerId(),
                room.getId(),
                startTimeMillis,
                endTimeMillis,
                true
        );

        slots.add(slot);

        return slot;
    }

    /**
     * Mở hoặc đóng nhận lịch cho một khung giờ thuộc chủ trọ hiện tại.
     * Không cho mở lại khung giờ đã bắt đầu.
     * Thao tác này chỉ đổi trạng thái khung giờ, không hủy lịch hẹn.
     */
    @MainThread
    public HostViewingSlot setSlotOpen(
            String slotId,
            boolean open
    ) {
        HostViewingSlot currentSlot = getMySlotById(slotId);

        if (currentSlot == null) {
            throw new IllegalStateException(
                    "Không tìm thấy khung giờ thuộc tài khoản của bạn."
            );
        }

        requireEditableRoom(currentSlot.getRoomId());

        if (open
                && currentSlot.getStartTimeMillis()
                <= System.currentTimeMillis()) {
            throw new IllegalArgumentException(
                    "Không thể mở khung giờ đã bắt đầu."
            );
        }

        HostViewingSlot updatedSlot = currentSlot.withOpen(open);

        for (int index = 0; index < slots.size(); index++) {
            if (slots.get(index).getId().equals(slotId)) {
                slots.set(index, updatedSlot);
                return updatedSlot;
            }
        }

        throw new IllegalStateException(
                "Khung giờ không còn trong dữ liệu mẫu."
        );
    }

    /**
     * Kiểm tra đăng nhập, xác minh email và quyền sở hữu phòng.
     * Repository phòng chỉ trả về phòng của tài khoản có quyền chủ trọ.
     */
    private HostRoom requireEditableRoom(String roomId) {
        SessionState session =
                SessionRepository.getInstance().getCurrentSession();

        if (!session.isLoggedIn()) {
            throw new IllegalStateException(
                    "Bạn cần đăng nhập để quản lý khung giờ."
            );
        }

        if (!session.isEmailVerified()) {
            throw new IllegalStateException(
                    "Bạn cần xác minh email để quản lý khung giờ."
            );
        }

        HostRoom room = DemoHostRoomRepository.getInstance()
                .getMyRoomById(roomId);

        if (room == null) {
            throw new IllegalStateException(
                    "Không tìm thấy phòng thuộc tài khoản của bạn."
            );
        }

        return room;
    }
}