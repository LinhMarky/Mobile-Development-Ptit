package com.example.roomly.data.repository;

import androidx.annotation.MainThread;

import com.example.roomly.data.model.HostRoom;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Lưu phòng mẫu trong bộ nhớ, tách theo tài khoản sở hữu.
 * Dữ liệu mất khi tiến trình ứng dụng kết thúc.
 */
public class DemoHostRoomRepository {

    private static final DemoHostRoomRepository INSTANCE =
            new DemoHostRoomRepository();

    private final List<HostRoom> rooms = new ArrayList<>();

    /**
     * Chỉ cho phép sử dụng đối tượng repository dùng chung.
     */
    private DemoHostRoomRepository() {
    }

    /**
     * Trả về repository dùng chung trong ứng dụng.
     */
    public static DemoHostRoomRepository getInstance() {
        return INSTANCE;
    }

    /**
     * Trả về các phòng thuộc tài khoản chủ trọ hiện tại.
     * Khách hoặc tài khoản không có quyền HOST nhận danh sách trống.
     */
    @MainThread
    public List<HostRoom> getMyRooms() {
        List<HostRoom> result = new ArrayList<>();

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()
                || !session.hasRole(UserRole.HOST)) {
            return result;
        }

        for (HostRoom room : rooms) {
            if (session.getUserId().equals(room.getOwnerId())) {
                result.add(room);
            }
        }

        return result;
    }

    /**
     * Tạo phòng chưa có ảnh.
     * Giữ tương thích với cách gọi hiện tại trong biểu mẫu.
     */
    @MainThread
    public HostRoom createRoom(
            String unitCode,
            String name,
            String address,
            BigDecimal area,
            String description
    ) {
        return createRoom(
                unitCode,
                name,
                address,
                area,
                description,
                null
        );
    }

    /**
     * Tạo phòng mẫu kèm đường dẫn ảnh trên thiết bị.
     * Chủ sở hữu được lấy từ phiên hiện tại, không nhận từ biểu mẫu.
     */
    @MainThread
    public HostRoom createRoom(
            String unitCode,
            String name,
            String address,
            BigDecimal area,
            String description,
            String imageUri
    ) {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()
                || !session.hasRole(UserRole.HOST)
                || !session.isEmailVerified()) {
            throw new IllegalStateException(
                    "Bạn cần đăng nhập bằng tài khoản chủ trọ "
                            + "và xác minh email để tạo phòng."
            );
        }

        String cleanCode = cleanText(unitCode);
        String cleanName = cleanText(name);
        String cleanAddress = cleanText(address);
        String cleanDescription = cleanText(description);
        String cleanImageUri = cleanText(imageUri);

        if (cleanCode.isEmpty()) {
            throw new IllegalArgumentException(
                    "Vui lòng nhập mã phòng."
            );
        }

        if (cleanName.isEmpty()) {
            throw new IllegalArgumentException(
                    "Vui lòng nhập tên phòng."
            );
        }

        if (cleanAddress.isEmpty()) {
            throw new IllegalArgumentException(
                    "Vui lòng nhập địa chỉ."
            );
        }

        if (area == null || area.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Diện tích phải lớn hơn 0."
            );
        }

        String ownerId = session.getUserId();

        if (hasDuplicateCode(ownerId, cleanCode)) {
            throw new IllegalArgumentException(
                    "Bạn đã có phòng sử dụng mã này."
            );
        }

        HostRoom room = new HostRoom(
                UUID.randomUUID().toString(),
                ownerId,
                cleanCode,
                cleanName,
                cleanAddress,
                area,
                cleanDescription,
                cleanImageUri.isEmpty() ? null : cleanImageUri
        );

        rooms.add(room);

        return room;
    }

    /**
     * Kiểm tra mã phòng trùng trong các phòng của cùng chủ trọ.
     * Dữ liệu mẫu so sánh không phân biệt chữ hoa và chữ thường.
     */
    private boolean hasDuplicateCode(
            String ownerId,
            String unitCode
    ) {
        for (HostRoom room : rooms) {
            boolean sameOwner = ownerId.equals(room.getOwnerId());
            boolean sameCode =
                    unitCode.equalsIgnoreCase(room.getUnitCode());

            if (sameOwner && sameCode) {
                return true;
            }
        }

        return false;
    }

    /**
     * Loại bỏ khoảng trắng ở hai đầu và xử lý giá trị null.
     */
    private String cleanText(String text) {
        return text == null ? "" : text.trim();
    }

    /**
     * Tìm phòng theo mã định danh.
     * Chỉ trả về phòng thuộc tài khoản chủ trọ đang đăng nhập.
     * Trả về null nếu không tìm thấy hoặc không có quyền truy cập.
     */
    @MainThread
    public HostRoom getMyRoomById(String roomId) {
        if (roomId == null || roomId.trim().isEmpty()) {
            return null;
        }

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()
                || !session.hasRole(UserRole.HOST)) {
            return null;
        }

        for (HostRoom room : rooms) {
            boolean sameRoom = roomId.equals(room.getId());

            boolean sameOwner = session.getUserId()
                    .equals(room.getOwnerId());

            if (sameRoom && sameOwner) {
                return room;
            }
        }

        return null;
    }

    /**
     * Cập nhật thông tin và ảnh của phòng thuộc tài khoản hiện tại.
     * Giữ nguyên ID phòng, chủ sở hữu và mã phòng.
     * Yêu cầu quyền HOST và email đã xác minh trước khi cập nhật.
     */
    @MainThread
    public HostRoom updateRoom(
            String roomId,
            String name,
            String address,
            BigDecimal area,
            String description,
            String imageUri
    ) {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()
                || !session.hasRole(UserRole.HOST)
                || !session.isEmailVerified()) {
            throw new IllegalStateException(
                    "Bạn cần đăng nhập bằng tài khoản chủ trọ "
                            + "và xác minh email để chỉnh sửa phòng."
            );
        }

        if (roomId == null || roomId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Mã định danh phòng không hợp lệ."
            );
        }

        String cleanName = cleanText(name);
        String cleanAddress = cleanText(address);
        String cleanDescription = cleanText(description);
        String cleanImageUri = cleanText(imageUri);

        if (cleanName.isEmpty()) {
            throw new IllegalArgumentException(
                    "Vui lòng nhập tên phòng."
            );
        }

        if (cleanAddress.isEmpty()) {
            throw new IllegalArgumentException(
                    "Vui lòng nhập địa chỉ."
            );
        }

        if (area == null || area.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Diện tích phải lớn hơn 0."
            );
        }

        for (int index = 0; index < rooms.size(); index++) {
            HostRoom currentRoom = rooms.get(index);

            boolean sameRoom = roomId.equals(currentRoom.getId());

            boolean sameOwner = session.getUserId()
                    .equals(currentRoom.getOwnerId());

            if (sameRoom && sameOwner) {
                HostRoom updatedRoom = new HostRoom(
                        currentRoom.getId(),
                        currentRoom.getOwnerId(),
                        currentRoom.getUnitCode(),
                        cleanName,
                        cleanAddress,
                        area,
                        cleanDescription,
                        cleanImageUri.isEmpty() ? null : cleanImageUri
                );

                // Thay đối tượng phòng tại vị trí cũ trong danh sách.
                rooms.set(index, updatedRoom);

                return updatedRoom;
            }
        }

        throw new IllegalStateException(
                "Không tìm thấy phòng thuộc tài khoản của bạn."
        );
    }
}