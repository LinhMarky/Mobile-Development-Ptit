package com.homely.rental.catalog.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomDTO {
    private Long id;

    @JsonProperty("host_id")
    private Long hostId;

    @JsonProperty("unit_code")
    private String unitCode;

    @JsonProperty("room_type")
    private String roomType;

    @JsonProperty("area_m2")
    private BigDecimal areaM2;

    @JsonProperty("max_occupants")
    private int maxOccupants;

    private AddressDTO address;
    private GeoPointDTO location;
    private String availability;

    @JsonProperty("terms_version")
    private int termsVersion;

    private List<AmenityDTO> amenities;
    private int version;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressDTO {
        private String line;
        @JsonProperty("province_code")
        private String provinceCode;
        @JsonProperty("province_name")
        private String provinceName;
        @JsonProperty("ward_code")
        private String wardCode;
        @JsonProperty("ward_name")
        private String wardName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeoPointDTO {
        private Double latitude;
        private Double longitude;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AmenityDTO {
        private Long id;
        private String name;
        private String icon;
        private String category;
    }
}
