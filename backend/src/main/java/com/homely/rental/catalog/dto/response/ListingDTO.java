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
public class ListingDTO {
    private Long id;

    @JsonProperty("room_id")
    private Long roomId;

    private String title;
    private String description;

    @JsonProperty("rent_vnd") @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private BigDecimal rentVnd;

    @JsonProperty("deposit_vnd") @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private BigDecimal depositVnd;

    private String status;

    @JsonProperty("terms_version")
    private int termsVersion;

    @JsonProperty("published_at")
    private Instant publishedAt;

    @JsonProperty("expires_at")
    private Instant expiresAt;

    private int version;

    // Denormalized room info for search results
    private RoomSummary room;
    private List<FeeDTO> fees;

    @JsonProperty("created_at")
    private Instant createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomSummary {
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
        private RoomDTO.AddressDTO address;
        private RoomDTO.GeoPointDTO location;
        private String availability;
        private List<RoomDTO.AmenityDTO> amenities;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeeDTO {
        private Long id;
        @JsonProperty("fee_code")
        private String feeCode;
        @JsonProperty("fee_mode")
        private String feeMode;
        @JsonProperty("amount_vnd") @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
        private BigDecimal amountVnd;
        @JsonProperty("unit_name")
        private String unitName;
        private String note;
        @JsonProperty("sort_order")
        private int sortOrder;
    }
}
