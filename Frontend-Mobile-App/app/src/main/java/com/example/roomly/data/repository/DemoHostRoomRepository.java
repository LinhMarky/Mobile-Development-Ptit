package com.example.roomly.data.repository;

import androidx.annotation.MainThread;

import com.example.roomly.data.model.HostRoom;
import com.example.roomly.data.model.RoomCard.RoomType;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Lưu phòng mẫu trong bộ nhớ, tách theo tài khoản sở hữu.
 * Dữ liệu mất khi tiến trình ứng dụng kết thúc.
 */
public class DemoHostRoomRepository {

    private static final DemoHostRoomRepository INSTANCE =
            new DemoHostRoomRepository();

    private static final BigDecimal MIN_AREA = new BigDecimal("2");
    private static final BigDecimal MAX_AREA = new BigDecimal("1000");

    private final List<HostRoom> rooms = new ArrayList<>();

    /**
     * Chỉ cho phép sử dụng repository dùng chung.
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
     * Lấy các phòng thuộc tài khoản có quyền chủ trọ hiện tại.
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
     * Giữ tương thích với biểu mẫu tạo phòng chưa có ảnh.
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
     * Giữ tương thích với biểu mẫu cũ.
     * Chưa tự gán sức chứa khi người dùng chưa nhập thông tin này.
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
        return createRoomInternal(
                unitCode,
                name,
                address,
                area,
                description,
                imageUri,
                RoomType.ROOM,
                0,
                Collections.emptyList(),
                false
        );
    }

    /**
     * Tạo phòng với đầy đủ thông tin bổ sung từ biểu mẫu mới.
     * Chủ sở hữu được lấy từ phiên hiện tại.
     */
    @MainThread
    public HostRoom createRoom(
            String unitCode,
            String name,
            String address,
            BigDecimal area,
            String description,
            String imageUri,
            RoomType roomType,
            int maxOccupants,
            List<String> amenities
    ) {
        return createRoomInternal(
                unitCode,
                name,
                address,
                area,
                description,
                imageUri,
                roomType,
                maxOccupants,
                amenities,
                true
        );
    }

    /**
     * Kiểm tra dữ liệu rồi thêm phòng vào bộ nhớ.
     */
    private HostRoom createRoomInternal(
            String unitCode,
            String name,
            String address,
            BigDecimal area,
            String description,
            String imageUri,
            RoomType roomType,
            int maxOccupants,
            List<String> amenities,
            boolean requireOccupants
    ) {
        SessionState session = requireHostSession();

        String cleanCode = cleanText(unitCode);
        String cleanName = cleanText(name);
        String cleanAddress = cleanText(address);

        validateUnitCode(cleanCode);
        validateRoomInformation(cleanName, cleanAddress, area);

        List<String> cleanAmenities = cleanAmenities(amenities);

        validateRoomOptions(
                roomType,
                maxOccupants,
                cleanAmenities,
                requireOccupants
        );

        if (hasDuplicateCode(session.getUserId(), cleanCode)) {
            throw new IllegalArgumentException(
                    "Bạn đã có phòng sử dụng mã này."
            );
        }

        HostRoom room = new HostRoom(
                UUID.randomUUID().toString(),
                session.getUserId(),
                cleanCode,
                cleanName,
                cleanAddress,
                area,
                cleanText(description),
                cleanImageUri(imageUri),
                roomType,
                maxOccupants,
                cleanAmenities
        );

        rooms.add(room);

        return room;
    }

    /**
     * Tìm phòng thuộc tài khoản có quyền chủ trọ hiện tại.
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
            if (roomId.equals(room.getId())
                    && session.getUserId().equals(room.getOwnerId())) {
                return room;
            }
        }

        return null;
    }

    /**
     * Giữ tương thích với biểu mẫu chỉnh sửa cũ.
     * Giữ nguyên loại phòng, sức chứa và tiện ích đã có.
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
        requireHostSession();

        HostRoom currentRoom = requireMyRoom(roomId);

        return updateRoomInternal(
                currentRoom,
                name,
                address,
                area,
                description,
                imageUri,
                currentRoom.getRoomType(),
                currentRoom.getMaxOccupants(),
                currentRoom.getAmenities(),
                false
        );
    }

    /**
     * Cập nhật đầy đủ thông tin phòng từ biểu mẫu mới.
     * Giữ nguyên ID phòng, chủ sở hữu và mã phòng.
     */
    @MainThread
    public HostRoom updateRoom(
            String roomId,
            String name,
            String address,
            BigDecimal area,
            String description,
            String imageUri,
            RoomType roomType,
            int maxOccupants,
            List<String> amenities
    ) {
        requireHostSession();

        HostRoom currentRoom = requireMyRoom(roomId);

        return updateRoomInternal(
                currentRoom,
                name,
                address,
                area,
                description,
                imageUri,
                roomType,
                maxOccupants,
                amenities,
                true
        );
    }

    /**
     * Kiểm tra dữ liệu và thay đối tượng phòng tại vị trí hiện tại.
     */
    private HostRoom updateRoomInternal(
            HostRoom currentRoom,
            String name,
            String address,
            BigDecimal area,
            String description,
            String imageUri,
            RoomType roomType,
            int maxOccupants,
            List<String> amenities,
            boolean requireOccupants
    ) {
        String cleanName = cleanText(name);
        String cleanAddress = cleanText(address);

        validateRoomInformation(cleanName, cleanAddress, area);

        List<String> cleanAmenities = cleanAmenities(amenities);

        validateRoomOptions(
                roomType,
                maxOccupants,
                cleanAmenities,
                requireOccupants
        );

        HostRoom updatedRoom = new HostRoom(
                currentRoom.getId(),
                currentRoom.getOwnerId(),
                currentRoom.getUnitCode(),
                cleanName,
                cleanAddress,
                area,
                cleanText(description),
                cleanImageUri(imageUri),
                roomType,
                maxOccupants,
                cleanAmenities
        );

        for (int index = 0; index < rooms.size(); index++) {
            if (rooms.get(index).getId().equals(currentRoom.getId())) {
                rooms.set(index, updatedRoom);
                return updatedRoom;
            }
        }

        throw new IllegalStateException(
                "Phòng không còn trong dữ liệu mẫu."
        );
    }

    /**
     * Yêu cầu phiên đăng nhập có quyền chủ trọ và email đã xác minh.
     */
    private SessionState requireHostSession() {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()) {
            throw new IllegalStateException(
                    "Bạn cần đăng nhập để quản lý phòng."
            );
        }

        if (!session.hasRole(UserRole.HOST)) {
            throw new IllegalStateException(
                    "Tài khoản chưa có quyền chủ trọ."
            );
        }

        if (!session.isEmailVerified()) {
            throw new IllegalStateException(
                    "Bạn cần xác minh email để quản lý phòng."
            );
        }

        return session;
    }

    /**
     * Yêu cầu phòng tồn tại và thuộc tài khoản hiện tại.
     */
    private HostRoom requireMyRoom(String roomId) {
        HostRoom room = getMyRoomById(roomId);

        if (room == null) {
            throw new IllegalStateException(
                    "Không tìm thấy phòng thuộc tài khoản của bạn."
            );
        }

        return room;
    }

    /**
     * Kiểm tra mã phòng gồm 1 đến 30 ký tự hợp lệ.
     */
    private void validateUnitCode(String unitCode) {
        if (!unitCode.matches("[A-Z0-9_-]{1,30}")) {
            throw new IllegalArgumentException(
                    "Mã phòng gồm 1–30 ký tự: chữ hoa, số, dấu _ hoặc -."
            );
        }
    }

    /**
     * Kiểm tra tên, địa chỉ và diện tích phòng.
     */
    private void validateRoomInformation(
            String name,
            String address,
            BigDecimal area
    ) {
        if (name.isEmpty()) {
            throw new IllegalArgumentException(
                    "Vui lòng nhập tên phòng."
            );
        }

        int addressLength = address.codePointCount(0, address.length());

        if (addressLength < 5 || addressLength > 300) {
            throw new IllegalArgumentException(
                    "Địa chỉ phải có từ 5 đến 300 ký tự."
            );
        }

        if (area == null
                || area.compareTo(MIN_AREA) < 0
                || area.compareTo(MAX_AREA) > 0) {
            throw new IllegalArgumentException(
                    "Diện tích phải từ 2 đến 1.000 m²."
            );
        }
    }

    /**
     * Kiểm tra loại phòng, sức chứa và số tiện ích.
     * Cho phép giá trị sức chứa 0 khi giữ tương thích dữ liệu cũ.
     */
    private void validateRoomOptions(
            RoomType roomType,
            int maxOccupants,
            List<String> amenities,
            boolean requireOccupants
    ) {
        if (roomType == null) {
            throw new IllegalArgumentException(
                    "Vui lòng chọn loại phòng."
            );
        }

        int minimum = requireOccupants ? 1 : 0;

        if (maxOccupants < minimum || maxOccupants > 10) {
            throw new IllegalArgumentException(
                    "Số người tối đa phải từ 1 đến 10."
            );
        }

        if (amenities.size() > 30) {
            throw new IllegalArgumentException(
                    "Chỉ được chọn tối đa 30 tiện ích."
            );
        }
    }

    /**
     * Kiểm tra mã phòng trùng trong các phòng của cùng chủ trọ.
     */
    private boolean hasDuplicateCode(
            String ownerId,
            String unitCode
    ) {
        for (HostRoom room : rooms) {
            if (ownerId.equals(room.getOwnerId())
                    && unitCode.equalsIgnoreCase(room.getUnitCode())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Làm sạch và loại bỏ tiện ích trùng nhau.
     */
    private List<String> cleanAmenities(List<String> amenities) {
        List<String> result = new ArrayList<>();

        if (amenities == null) {
            return result;
        }

        for (String amenity : amenities) {
            String cleaned = cleanText(amenity);

            if (!cleaned.isEmpty() && !result.contains(cleaned)) {
                result.add(cleaned);
            }
        }

        return result;
    }

    /**
     * Trả về null khi chưa có URI ảnh.
     */
    private String cleanImageUri(String imageUri) {
        String cleaned = cleanText(imageUri);
        return cleaned.isEmpty() ? null : cleaned;
    }

    /**
     * Loại bỏ khoảng trắng ở hai đầu và xử lý null.
     */
    private String cleanText(String text) {
        return text == null ? "" : text.trim();
    }
}