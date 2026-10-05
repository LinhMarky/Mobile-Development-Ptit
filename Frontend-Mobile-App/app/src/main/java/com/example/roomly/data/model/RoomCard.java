package com.example.roomly.data.model;

/**
 * Dữ liệu dùng để hiển thị một thẻ phòng.
 * Hiện sử dụng ảnh có sẵn trong drawable.
 */
public class RoomCard {

    private final String title;
    private final String price;
    private final String address;
    private final String amenities;
    private final int imageResId;
    private boolean saved;

    /**
     * Khởi tạo thông tin phòng và trạng thái lưu ban đầu.
     */
    public RoomCard(String title, String price, String address,
                    String amenities, int imageResId, boolean saved) {
        this.title = title;
        this.price = price;
        this.address = address;
        this.amenities = amenities;
        this.imageResId = imageResId;
        this.saved = saved;
    }

    /** Trả về tên phòng. */
    public String getTitle() {
        return title;
    }

    /** Trả về giá đã định dạng để hiển thị. */
    public String getPrice() {
        return price;
    }

    /** Trả về địa chỉ phòng. */
    public String getAddress() {
        return address;
    }

    /** Trả về mô tả diện tích và tiện ích. */
    public String getAmenities() {
        return amenities;
    }

    /** Trả về mã tài nguyên ảnh trong drawable. */
    public int getImageResId() {
        return imageResId;
    }

    /** Cho biết phòng đang được lưu hay chưa. */
    public boolean isSaved() {
        return saved;
    }

    /** Cập nhật trạng thái lưu trong dữ liệu giao diện. */
    public void setSaved(boolean saved) {
        this.saved = saved;
    }
}