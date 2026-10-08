package com.example.roomly.data.model;

import com.example.roomly.data.model.RoomCard.RoomType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Thông tin phòng dùng cho giao diện chủ trọ.
 * Đây là mô hình giao diện, chưa phải DTO của API.
 */
public class HostRoom {

    private final String id;
    private final String ownerId;
    private final String unitCode;
    private final String name;
    private final String address;
    private final BigDecimal area;
    private final String description;
    private final String imageUri;

    private final RoomType roomType;

    // Giá trị 0 nghĩa là dữ liệu cũ chưa cung cấp số người tối đa.
    private final int maxOccupants;

    // Tên tiện ích dùng để thử giao diện, không phải amenity_ids của API.
    private final List<String> amenities;

    /**
     * Giữ tương thích với cách tạo phòng chưa có ảnh.
     */
    public HostRoom(
            String id,
            String ownerId,
            String unitCode,
            String name,
            String address,
            BigDecimal area,
            String description
    ) {
        this(
                id,
                ownerId,
                unitCode,
                name,
                address,
                area,
                description,
                null
        );
    }

    /**
     * Giữ tương thích với dữ liệu phòng cũ có ảnh.
     * Số người tối đa chưa được cung cấp sẽ có giá trị 0.
     */
    public HostRoom(
            String id,
            String ownerId,
            String unitCode,
            String name,
            String address,
            BigDecimal area,
            String description,
            String imageUri
    ) {
        this(
                id,
                ownerId,
                unitCode,
                name,
                address,
                area,
                description,
                imageUri,
                RoomType.ROOM,
                0,
                Collections.emptyList()
        );
    }

    /**
     * Khởi tạo phòng với loại phòng, sức chứa và tiện ích.
     * Sao chép danh sách để dữ liệu không bị đổi từ bên ngoài.
     */
    public HostRoom(
            String id,
            String ownerId,
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
        this.id = id;
        this.ownerId = ownerId;
        this.unitCode = unitCode;
        this.name = name;
        this.address = address;
        this.area = area;
        this.description = description == null ? "" : description;
        this.imageUri = imageUri;

        this.roomType = roomType == null
                ? RoomType.ROOM
                : roomType;

        this.maxOccupants = maxOccupants;

        List<String> copiedAmenities = new ArrayList<>();

        if (amenities != null) {
            for (String amenity : amenities) {
                if (amenity == null) {
                    continue;
                }

                String cleanedAmenity = amenity.trim();

                if (!cleanedAmenity.isEmpty()
                        && !copiedAmenities.contains(cleanedAmenity)) {
                    copiedAmenities.add(cleanedAmenity);
                }
            }
        }

        this.amenities = Collections.unmodifiableList(copiedAmenities);
    }

    /**
     * Trả về mã định danh phòng.
     */
    public String getId() {
        return id;
    }

    /**
     * Trả về mã tài khoản sở hữu phòng.
     */
    public String getOwnerId() {
        return ownerId;
    }

    /**
     * Trả về mã phòng do chủ trọ nhập.
     */
    public String getUnitCode() {
        return unitCode;
    }

    /**
     * Trả về tên phòng.
     */
    public String getName() {
        return name;
    }

    /**
     * Trả về địa chỉ phòng.
     */
    public String getAddress() {
        return address;
    }

    /**
     * Trả về diện tích theo mét vuông.
     */
    public BigDecimal getArea() {
        return area;
    }

    /**
     * Định dạng diện tích mà vẫn giữ phần thập phân.
     */
    public String getFormattedArea() {
        if (area == null) {
            return "Chưa cung cấp";
        }

        return area.stripTrailingZeros()
                .toPlainString()
                .replace('.', ',') + " m²";
    }

    /**
     * Trả về mô tả phòng.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Trả về URI ảnh, hoặc null nếu chưa có ảnh.
     */
    public String getImageUri() {
        return imageUri;
    }

    /**
     * Trả về loại phòng dùng cho giao diện và bộ lọc.
     */
    public RoomType getRoomType() {
        return roomType;
    }

    /**
     * Chuyển loại phòng thành tên tiếng Việt.
     */
    public String getRoomTypeLabel() {
        switch (roomType) {
            case APARTMENT:
                return "Căn hộ";

            case STUDIO:
                return "Studio";

            case ROOM:
            default:
                return "Phòng trọ";
        }
    }

    /**
     * Trả về số người tối đa.
     * Giá trị 0 nghĩa là dữ liệu cũ chưa cung cấp.
     */
    public int getMaxOccupants() {
        return maxOccupants;
    }

    /**
     * Định dạng số người tối đa để hiển thị.
     */
    public String getMaxOccupantsLabel() {
        if (maxOccupants <= 0) {
            return "Số người tối đa chưa cung cấp";
        }

        return "Tối đa " + maxOccupants + " người";
    }

    /**
     * Trả về danh sách tiện ích không cho phép sửa trực tiếp.
     */
    public List<String> getAmenities() {
        return amenities;
    }

    /**
     * Ghép các tiện ích thành nội dung để hiển thị.
     */
    public String getFormattedAmenities() {
        if (amenities.isEmpty()) {
            return "Chưa cung cấp tiện ích";
        }

        StringBuilder result = new StringBuilder();

        for (String amenity : amenities) {
            if (result.length() > 0) {
                result.append(" • ");
            }

            result.append(amenity);
        }

        return result.toString();
    }

    /**
     * Tạo nội dung tóm tắt dùng trên thẻ phòng công khai.
     */
    public String getDisplaySummary() {
        String summary = getRoomTypeLabel()
                + " • " + getFormattedArea()
                + " • " + getMaxOccupantsLabel();

        if (!amenities.isEmpty()) {
            summary += " • " + getFormattedAmenities();
        }

        return summary;
    }
}