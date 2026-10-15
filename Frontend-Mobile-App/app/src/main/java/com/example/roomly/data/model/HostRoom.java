package com.example.roomly.data.model;

import com.example.roomly.data.model.RoomCard.RoomType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private final int maxOccupants;
    private final List<String> amenities;
    private final RoomCard.Availability availability;

    public HostRoom(
            String id,
            String ownerId,
            String unitCode,
            String name,
            String address,
            BigDecimal area,
            String description) {
        this(id, ownerId, unitCode, name, address, area, description, null);
    }

    public HostRoom(
            String id,
            String ownerId,
            String unitCode,
            String name,
            String address,
            BigDecimal area,
            String description,
            String imageUri) {
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
                Collections.emptyList());
    }

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
            List<String> amenities) {
        this(
                id,
                ownerId,
                unitCode,
                name,
                address,
                area,
                description,
                imageUri,
                roomType,
                maxOccupants,
                amenities,
                RoomCard.Availability.AVAILABLE);
    }

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
            List<String> amenities,
            RoomCard.Availability availability) {
        this.id = id;
        this.ownerId = ownerId;
        this.unitCode = unitCode;
        this.name = name;
        this.address = address;
        this.area = area;
        this.description = description == null ? "" : description;
        this.imageUri = imageUri;
        this.roomType = roomType == null ? RoomType.ROOM : roomType;
        this.maxOccupants = maxOccupants;

        // Không coi trạng thái chưa xác định là phòng còn trống.
        this.availability = availability == null ? RoomCard.Availability.UNKNOWN : availability;

        List<String> copiedAmenities = new ArrayList<>();

        if (amenities != null) {
            for (String amenity : amenities) {
                if (amenity == null) {
                    continue;
                }

                String cleanedAmenity = amenity.trim();

                if (!cleanedAmenity.isEmpty() && !copiedAmenities.contains(cleanedAmenity)) {
                    copiedAmenities.add(cleanedAmenity);
                }
            }
        }

        this.amenities = Collections.unmodifiableList(copiedAmenities);
    }

    public String getId() {
        return id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getUnitCode() {
        return unitCode;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public BigDecimal getArea() {
        return area;
    }

    public String getFormattedArea() {
        if (area == null) {
            return "Chưa cung cấp";
        }

        return area.stripTrailingZeros().toPlainString().replace('.', ',') + " m²";
    }

    public String getDescription() {
        return description;
    }

    public String getImageUri() {
        return imageUri;
    }

    public RoomType getRoomType() {
        return roomType;
    }

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

    public int getMaxOccupants() {
        return maxOccupants;
    }

    public String getMaxOccupantsLabel() {
        if (maxOccupants <= 0) {
            return "Số người tối đa chưa cung cấp";
        }

        return "Tối đa " + maxOccupants + " người";
    }

    public List<String> getAmenities() {
        return amenities;
    }

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

    public String getDisplaySummary() {
        String summary =
                getRoomTypeLabel() + " • " + getFormattedArea() + " • " + getMaxOccupantsLabel();

        if (!amenities.isEmpty()) {
            summary += " • " + getFormattedAmenities();
        }

        return summary;
    }

    public RoomCard.Availability getAvailability() {
        return availability;
    }

    public String getAvailabilityLabel() {
        switch (availability) {
            case AVAILABLE:
                return "Còn trống";

            case HELD:
                return "Đang giữ chỗ";

            case RENTED:
                return "Đã cho thuê";

            case UNKNOWN:
            default:
                return "Chưa xác định";
        }
    }
}
