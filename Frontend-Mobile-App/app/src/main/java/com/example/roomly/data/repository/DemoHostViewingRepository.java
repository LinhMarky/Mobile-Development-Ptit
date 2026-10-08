package com.example.roomly.data.repository;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;

import com.example.roomly.data.model.HostRoom;
import com.example.roomly.data.model.HostViewingSlot;
import com.example.roomly.data.model.RoomCard;
import com.example.roomly.data.model.SessionState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Quản lý khung giờ xem phòng trong bộ nhớ. */
public class DemoHostViewingRepository {

    private static final DemoHostViewingRepository INSTANCE =
            new DemoHostViewingRepository();

    private final List<HostViewingSlot> slots = new ArrayList<>();

    /** Không tạo sẵn khung giờ giả. */
    private DemoHostViewingRepository() {
    }

    /** Trả về repository dùng chung. */
    public static DemoHostViewingRepository getInstance() {
        return INSTANCE;
    }

    /** Lấy các khung giờ của phòng thuộc chủ trọ hiện tại. */
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

    /** Lấy một khung giờ thuộc phòng của chủ trọ hiện tại. */
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
     * Lấy khung giờ công khai của một bài đang hiển thị.
     * Không dùng dữ liệu mã chủ trọ do người gọi tự truyền vào.
     */
    @MainThread
    public List<HostViewingSlot> getOpenSlotsForListing(
            String listingId
    ) {
        List<HostViewingSlot> results = new ArrayList<>();

        if (listingId == null || listingId.trim().isEmpty()) {
            return results;
        }

        RoomCard publishedRoom = null;

        for (RoomCard card : DemoAdminListingRepository.getInstance()
                .getPublishedRoomCards()) {
            if (listingId.equals(card.getListingId())) {
                publishedRoom = card;
                break;
            }
        }

        if (publishedRoom == null) {
            return results;
        }

        long now = System.currentTimeMillis();

        for (HostViewingSlot slot : slots) {
            if (slot.isOpen()
                    && slot.getStartTimeMillis() > now
                    && slot.getRoomId().equals(
                    publishedRoom.getRoomId()
            )
                    && slot.getOwnerId().equals(
                    publishedRoom.getOwnerId()
            )) {
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

    /** Tạo khung giờ tương lai cho phòng thuộc chủ trọ hiện tại. */
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
     * Mở hoặc đóng nhận yêu cầu cho khung giờ của chủ trọ.
     * Đóng khung giờ không tự hủy yêu cầu đã gửi.
     */
    @MainThread
    public HostViewingSlot setSlotOpen(String slotId, boolean open) {
        HostViewingSlot current = getMySlotById(slotId);

        if (current == null) {
            throw new IllegalStateException(
                    "Không tìm thấy khung giờ thuộc tài khoản của bạn."
            );
        }

        requireEditableRoom(current.getRoomId());

        if (open && current.getStartTimeMillis()
                <= System.currentTimeMillis()) {
            throw new IllegalArgumentException(
                    "Không thể mở khung giờ đã bắt đầu."
            );
        }

        HostViewingSlot updated = current.withOpen(open);

        for (int index = 0; index < slots.size(); index++) {
            if (slots.get(index).getId().equals(slotId)) {
                slots.set(index, updated);
                return updated;
            }
        }

        throw new IllegalStateException(
                "Khung giờ không còn trong dữ liệu mẫu."
        );
    }

    /** Kiểm tra đăng nhập, xác minh email và quyền sở hữu phòng. */
    private HostRoom requireEditableRoom(String roomId) {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

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