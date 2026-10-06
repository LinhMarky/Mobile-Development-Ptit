package com.example.roomly.data.model;

import java.math.BigDecimal;

/**
 * Thông tin phòng trong giao diện quản lý của chủ trọ.
 * Đây là mô hình dùng cho dữ liệu mẫu, chưa phải DTO của API.
 */
public class HostRoom {

    private final String id;
    private final String ownerId;
    private final String unitCode;
    private final String name;
    private final String address;
    private final BigDecimal area;
    private final String description;

    // Đường dẫn ảnh trên thiết bị; null nghĩa là chưa chọn ảnh.
    private final String imageUri;

    /**
     * Khởi tạo phòng chưa có ảnh.
     * Giữ tương thích với phần code tạo phòng hiện tại.
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
     * Khởi tạo phòng kèm đường dẫn ảnh đã chọn trên thiết bị.
     * Mã chủ trọ và mã phòng không có setter để tránh thay đổi sau khi tạo.
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
        this.id = id;
        this.ownerId = ownerId;
        this.unitCode = unitCode;
        this.name = name;
        this.address = address;
        this.area = area;
        this.description = description;
        this.imageUri = imageUri;
    }

    /**
     * Trả về mã định danh của phòng.
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
     * Trả về mã phòng do chủ trọ nhập, ví dụ P101.
     */
    public String getUnitCode() {
        return unitCode;
    }

    /**
     * Trả về tên phòng trong danh sách quản lý.
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
     * Trả về diện tích phòng theo mét vuông.
     */
    public BigDecimal getArea() {
        return area;
    }

    /**
     * Trả về diện tích đã định dạng để hiển thị.
     * Ví dụ: 28.50 thành 28,5 m².
     */
    public String getFormattedArea() {
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
     * Trả về đường dẫn ảnh trên thiết bị.
     * Giá trị null nghĩa là phòng chưa có ảnh.
     */
    public String getImageUri() {
        return imageUri;
    }
}