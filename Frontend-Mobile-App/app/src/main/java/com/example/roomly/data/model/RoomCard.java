package com.example.roomly.data.model;

import java.math.BigDecimal;

/**
 * Dữ liệu hiển thị phòng trên các màn hình công khai.
 * Giữ định danh để liên kết bài đăng, phòng và chủ trọ.
 */
public class RoomCard {

    public enum RoomType {
        ROOM,
        APARTMENT,
        STUDIO
    }

    private String title;
    private String price;
    private String address;
    private String amenities;
    private int imageResId;
    private RoomType roomType;

    private final String roomId;
    private final String ownerId;
    private final String unitCode;

    private String listingId = "";
    private String imageUri = "";
    private String description = "";
    private String hostName = "";

    private long monthlyRent;

    // null nghĩa là chưa có dữ liệu diện tích.
    private BigDecimal area;

    private boolean saved;

    /** Giữ constructor cũ với loại phòng mặc định. */
    public RoomCard(
            String title,
            String price,
            String address,
            String amenities,
            int imageResId,
            boolean saved
    ) {
        this(
                title, price, address, amenities,
                imageResId, saved, RoomType.ROOM
        );
    }

    /** Giữ constructor cũ chưa có định danh phòng và chủ trọ. */
    public RoomCard(
            String title,
            String price,
            String address,
            String amenities,
            int imageResId,
            boolean saved,
            RoomType roomType
    ) {
        this(
                title, price, address, amenities,
                imageResId, saved, roomType,
                "", "", ""
        );
    }

    /** Khởi tạo thông tin thẻ cùng định danh phòng và chủ trọ. */
    public RoomCard(
            String title,
            String price,
            String address,
            String amenities,
            int imageResId,
            boolean saved,
            RoomType roomType,
            String roomId,
            String ownerId,
            String unitCode
    ) {
        this.title = cleanText(title);
        this.price = cleanText(price);
        this.address = cleanText(address);
        this.amenities = cleanText(amenities);
        this.imageResId = imageResId;
        this.saved = saved;
        this.roomType = roomType == null ? RoomType.ROOM : roomType;

        this.roomId = cleanText(roomId);
        this.ownerId = cleanText(ownerId);
        this.unitCode = cleanText(unitCode);
    }

    /** Trả về tiêu đề bài đăng hoặc tên phòng. */
    public String getTitle() {
        return title;
    }

    /** Trả về giá thuê đã định dạng. */
    public String getPrice() {
        return price;
    }

    /** Trả về địa chỉ phòng. */
    public String getAddress() {
        return address;
    }

    /** Trả về thông tin ngắn hiển thị trên thẻ. */
    public String getAmenities() {
        return amenities;
    }

    /** Trả về mã ảnh drawable; 0 nghĩa là không có ảnh drawable. */
    public int getImageResId() {
        return imageResId;
    }

    /** Trả về loại phòng dùng cho bộ lọc hiện tại. */
    public RoomType getRoomType() {
        return roomType;
    }

    /** Trả về mã định danh phòng. */
    public String getRoomId() {
        return roomId;
    }

    /** Trả về mã tài khoản chủ trọ. */
    public String getOwnerId() {
        return ownerId;
    }

    /** Trả về mã phòng do chủ trọ đặt. */
    public String getUnitCode() {
        return unitCode;
    }

    /** Trả về mã bài đăng liên kết với thẻ. */
    public String getListingId() {
        return listingId;
    }

    /** Gán mã bài đăng từ nguồn dữ liệu công khai. */
    public void setListingId(String listingId) {
        this.listingId = cleanText(listingId);
    }

    /** Trả về URI ảnh hoặc chuỗi trống khi chưa có ảnh. */
    public String getImageUri() {
        return imageUri;
    }

    /** Gán URI ảnh phòng. */
    public void setImageUri(String imageUri) {
        this.imageUri = cleanText(imageUri);
    }

    /** Trả về nội dung mô tả bài đăng. */
    public String getDescription() {
        return description;
    }

    /** Gán nội dung mô tả từ bài đăng được công khai. */
    public void setDescription(String description) {
        this.description = cleanText(description);
    }

    /** Trả về tên chủ trọ được lưu cùng bài đăng. */
    public String getHostName() {
        return hostName;
    }

    /** Gán tên chủ trọ từ nguồn dữ liệu bài đăng. */
    public void setHostName(String hostName) {
        this.hostName = cleanText(hostName);
    }

    /** Cho biết thẻ đang được lưu hay chưa. */
    public boolean isSaved() {
        return saved;
    }

    /** Cập nhật trạng thái lưu của thẻ. */
    public void setSaved(boolean saved) {
        this.saved = saved;
    }

    /** Trả về giá thuê theo tháng bằng đồng Việt Nam. */
    public long getMonthlyRent() {
        return monthlyRent;
    }

    /** Gán giá thuê dạng số để bộ lọc sử dụng. */
    public void setMonthlyRent(long monthlyRent) {
        this.monthlyRent = monthlyRent;
    }

    /**
     * Giữ getter diện tích số nguyên cho code bộ lọc đang dùng.
     * Phần thập phân vẫn được giữ trong getArea().
     */
    public int getAreaSquareMeters() {
        return area == null ? 0 : area.intValue();
    }

    /** Giữ setter cũ cho các phòng minh họa có diện tích số nguyên. */
    public void setAreaSquareMeters(int areaSquareMeters) {
        area = areaSquareMeters > 0
                ? BigDecimal.valueOf(areaSquareMeters)
                : null;
    }

    /** Trả về diện tích chính xác; null nghĩa là chưa có dữ liệu. */
    public BigDecimal getArea() {
        return area;
    }

    /** Gán diện tích chính xác, giữ phần thập phân. */
    public void setArea(BigDecimal area) {
        this.area = area != null
                && area.compareTo(BigDecimal.ZERO) > 0
                ? area
                : null;
    }

    /** Định dạng diện tích để hiển thị, ví dụ 28,5 m². */
    public String getFormattedArea() {
        if (area == null) {
            return "Chưa cung cấp";
        }

        return area.stripTrailingZeros()
                .toPlainString()
                .replace('.', ',') + " m²";
    }

    /**
     * Làm mới dữ liệu hiển thị của cùng một bài đăng.
     * Giữ nguyên định danh và trạng thái lưu.
     */
    public void updateDisplayFrom(RoomCard incoming) {
        if (incoming == null
                || !listingId.equals(incoming.getListingId())
                || !roomId.equals(incoming.getRoomId())
                || !ownerId.equals(incoming.getOwnerId())
                || !unitCode.equals(incoming.getUnitCode())) {
            throw new IllegalArgumentException(
                    "Không thể cập nhật thẻ bằng dữ liệu khác định danh."
            );
        }

        title = incoming.getTitle();
        price = incoming.getPrice();
        address = incoming.getAddress();
        amenities = incoming.getAmenities();
        imageResId = incoming.getImageResId();
        roomType = incoming.getRoomType();
        imageUri = incoming.getImageUri();
        description = incoming.getDescription();
        hostName = incoming.getHostName();
        monthlyRent = incoming.getMonthlyRent();
        area = incoming.getArea();
    }

    /** Chuẩn hóa chuỗi và xử lý giá trị null. */
    private String cleanText(String text) {
        return text == null ? "" : text.trim();
    }
}