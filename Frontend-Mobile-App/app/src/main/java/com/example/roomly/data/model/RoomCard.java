package com.example.roomly.data.model;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Dữ liệu phòng dùng cho giao diện.
 */
public class RoomCard {

    public enum RoomType {
        ROOM,
        APARTMENT,
        STUDIO
    }

    public enum Availability {
        UNKNOWN,
        AVAILABLE,
        HELD,
        RENTED
    }

    /**
     * Một khoản phí của phòng.
     */
    public static final class Fee {

        public final String name;
        public final long amountVnd;
        public final String unit;

        public Fee(String name, long amountVnd, String unit) {
            if (name == null
                    || name.trim().isEmpty()
                    || amountVnd < 0) {
                throw new IllegalArgumentException(
                        "Khoản phí không hợp lệ."
                );
            }

            this.name = name.trim();
            this.amountVnd = amountVnd;
            this.unit = unit == null ? "" : unit.trim();
        }

        public String getDisplayAmount() {
            return formatVnd(amountVnd)
                    + (unit.isEmpty() ? "" : "/" + unit);
        }
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

    // null: chưa cung cấp; 0: không yêu cầu cọc.
    private Long depositVnd;

    private Availability availability = Availability.UNKNOWN;

    private final List<Fee> fees = new ArrayList<>();

    // Bộ ảnh cho màn hình chi tiết.
    private final List<RoomImage> images = new ArrayList<>();

    // null nghĩa là chưa cung cấp diện tích.
    private BigDecimal area;

    private boolean saved;

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
                title,
                price,
                address,
                amenities,
                imageResId,
                saved,
                roomType,
                "",
                "",
                ""
        );
    }

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

        this.roomType = roomType == null
                ? RoomType.ROOM
                : roomType;

        this.roomId = cleanText(roomId);
        this.ownerId = cleanText(ownerId);
        this.unitCode = cleanText(unitCode);
    }

    public String getTitle() {
        return title;
    }

    public String getPrice() {
        return price;
    }

    public String getAddress() {
        return address;
    }

    public String getAmenities() {
        return amenities;
    }

    public int getImageResId() {
        return imageResId;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getUnitCode() {
        return unitCode;
    }

    public String getListingId() {
        return listingId;
    }

    public void setListingId(String listingId) {
        this.listingId = cleanText(listingId);
    }

    public String getImageUri() {
        return imageUri;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = cleanText(imageUri);
    }

    /**
     * Trả về bộ ảnh.
     * Nếu chưa có bộ ảnh riêng, dùng ảnh đơn hiện có.
     */
    public List<RoomImage> getImages() {
        if (!images.isEmpty()) {
            return Collections.unmodifiableList(
                    new ArrayList<>(images)
            );
        }

        List<RoomImage> fallback = new ArrayList<>();

        if (!imageUri.isEmpty()) {
            fallback.add(
                    RoomImage.fromUri(
                            imageUri,
                            "Ảnh phòng " + title
                    )
            );
        } else if (imageResId > 0) {
            fallback.add(
                    RoomImage.fromDrawable(
                            imageResId,
                            "Ảnh phòng " + title
                    )
            );
        }

        return Collections.unmodifiableList(fallback);
    }

    /**
     * Gán bộ ảnh mà không thay đổi ảnh bìa của thẻ phòng.
     * Danh sách null hoặc rỗng sẽ dùng ảnh đơn dự phòng.
     */
    public void setImages(List<RoomImage> values) {
        List<RoomImage> copy = values == null
                ? new ArrayList<>()
                : new ArrayList<>(values);

        if (copy.contains(null)) {
            throw new IllegalArgumentException(
                    "Ảnh phòng không được null."
            );
        }

        images.clear();
        images.addAll(copy);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = cleanText(description);
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = cleanText(hostName);
    }

    public boolean isSaved() {
        return saved;
    }

    public void setSaved(boolean saved) {
        this.saved = saved;
    }

    public long getMonthlyRent() {
        return monthlyRent;
    }

    public void setMonthlyRent(long monthlyRent) {
        this.monthlyRent = monthlyRent;
    }

    public Long getDepositVnd() {
        return depositVnd;
    }

    public void setDepositVnd(Long depositVnd) {
        if (depositVnd != null && depositVnd < 0) {
            throw new IllegalArgumentException(
                    "Tiền cọc không được âm."
            );
        }

        this.depositVnd = depositVnd;
    }

    public String getDepositLabel() {
        if (depositVnd == null) {
            return "Chưa cung cấp";
        }

        if (depositVnd == 0) {
            return "Không yêu cầu cọc";
        }

        return formatVnd(depositVnd);
    }

    public Availability getAvailability() {
        return availability;
    }

    public void setAvailability(Availability availability) {
        this.availability = availability == null
                ? Availability.UNKNOWN
                : availability;
    }

    public String getAvailabilityLabel() {
        switch (availability) {
            case AVAILABLE:
                return "Còn trống";

            case HELD:
                return "Đang giữ phòng";

            case RENTED:
                return "Đã cho thuê";

            case UNKNOWN:
            default:
                return "Chưa cập nhật tình trạng";
        }
    }

    public List<Fee> getFees() {
        return Collections.unmodifiableList(fees);
    }

    public void setFees(List<Fee> values) {
        List<Fee> copy = values == null
                ? new ArrayList<>()
                : new ArrayList<>(values);

        if (copy.contains(null)) {
            throw new IllegalArgumentException(
                    "Khoản phí không được null."
            );
        }

        fees.clear();
        fees.addAll(copy);
    }

    public static String formatVnd(long amount) {
        return NumberFormat
                .getIntegerInstance(new Locale("vi", "VN"))
                .format(amount) + " ₫";
    }

    public int getAreaSquareMeters() {
        return area == null ? 0 : area.intValue();
    }

    public void setAreaSquareMeters(int areaSquareMeters) {
        area = areaSquareMeters > 0
                ? BigDecimal.valueOf(areaSquareMeters)
                : null;
    }

    public BigDecimal getArea() {
        return area;
    }

    public void setArea(BigDecimal area) {
        this.area = area != null
                && area.compareTo(BigDecimal.ZERO) > 0
                ? area
                : null;
    }

    public String getFormattedArea() {
        if (area == null) {
            return "Chưa cung cấp";
        }

        return area.stripTrailingZeros()
                .toPlainString()
                .replace('.', ',') + " m²";
    }

    /**
     * Cập nhật dữ liệu cùng một bài đăng.
     * Giữ nguyên định danh và trạng thái đã lưu.
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

        // Sao chép trước để xử lý được cả incoming == this.
        List<RoomImage> incomingImages =
                new ArrayList<>(incoming.images);

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

        depositVnd = incoming.getDepositVnd();
        availability = incoming.getAvailability();

        setFees(incoming.getFees());
        setImages(incomingImages);
    }

    private String cleanText(String text) {
        return text == null ? "" : text.trim();
    }
}