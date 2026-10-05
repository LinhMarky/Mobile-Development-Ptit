package com.example.roomly.data.model;

/**
 * Dữ liệu hiển thị một thẻ phòng.
 * Hiện dùng ảnh trong drawable và dữ liệu mẫu.
 */
public class RoomCard {

    /**
     * Các loại phòng dùng cho bộ lọc giao diện.
     */
    public enum RoomType {
        ROOM,
        APARTMENT,
        STUDIO
    }

    private final String title;
    private final String price;
    private final String address;
    private final String amenities;
    private final int imageResId;
    private final RoomType roomType;

    // Giá thuê theo tháng, đơn vị đồng.
    private long monthlyRent;

    // Diện tích phòng, đơn vị mét vuông.
    private int areaSquareMeters;

    private boolean saved;

    /**
     * Giữ cách khởi tạo cũ để code hiện tại vẫn hoạt động.
     * Phòng chưa được gán loại sẽ tạm thuộc nhóm Phòng trọ.
     */
    public RoomCard(
            String title,
            String price,
            String address,
            String amenities,
            int imageResId,
            boolean saved
    ) {
        this(
                title,
                price,
                address,
                amenities,
                imageResId,
                saved,
                RoomType.ROOM
        );
    }

    /**
     * Khởi tạo thông tin phòng, trạng thái lưu và loại phòng.
     */
    public RoomCard(
            String title,
            String price,
            String address,
            String amenities,
            int imageResId,
            boolean saved,
            RoomType roomType
    ) {
        this.title = title;
        this.price = price;
        this.address = address;
        this.amenities = amenities;
        this.imageResId = imageResId;
        this.saved = saved;
        this.roomType = roomType;
    }

    /**
     * Trả về tên phòng.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Trả về giá đã định dạng để hiển thị.
     */
    public String getPrice() {
        return price;
    }

    /**
     * Trả về địa chỉ phòng.
     */
    public String getAddress() {
        return address;
    }

    /**
     * Trả về mô tả diện tích và tiện ích.
     */
    public String getAmenities() {
        return amenities;
    }

    /**
     * Trả về mã tài nguyên ảnh trong drawable.
     */
    public int getImageResId() {
        return imageResId;
    }

    /**
     * Trả về loại phòng để lọc danh sách.
     */
    public RoomType getRoomType() {
        return roomType;
    }

    /**
     * Cho biết phòng đang được lưu hay chưa.
     */
    public boolean isSaved() {
        return saved;
    }

    /**
     * Cập nhật trạng thái lưu trong dữ liệu giao diện.
     */
    public void setSaved(boolean saved) {
        this.saved = saved;
    }

    /**
     * Trả về giá thuê dạng số để lọc theo khoảng giá.
     */
    public long getMonthlyRent() {
        return monthlyRent;
    }

    /**
     * Gán giá thuê theo tháng cho dữ liệu phòng mẫu.
     */
    public void setMonthlyRent(long monthlyRent) {
        this.monthlyRent = monthlyRent;
    }

    /**
     * Trả về diện tích dạng số để lọc phòng.
     */
    public int getAreaSquareMeters() {
        return areaSquareMeters;
    }

    /**
     * Gán diện tích cho dữ liệu phòng mẫu.
     */
    public void setAreaSquareMeters(int areaSquareMeters) {
        this.areaSquareMeters = areaSquareMeters;
    }
}