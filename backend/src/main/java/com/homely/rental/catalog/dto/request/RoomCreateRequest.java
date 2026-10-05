package com.homely.rental.catalog.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Room creation request (HOST01).
 */
@Getter
@Setter
public class RoomCreateRequest {

    @NotBlank(message = "Unit code is required")
    @Size(min = 1, max = 30, message = "Unit code must be between 1 and 30 characters")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Unit code must match [A-Z0-9_-]+")
    private String unitCode;

    @NotNull(message = "Room type is required")
    @Pattern(regexp = "SINGLE_ROOM|STUDIO|MINI_APARTMENT", message = "Room type is invalid")
    private String roomType;

    @NotNull(message = "Area is required")
    @DecimalMin(value = "2", message = "Area must be at least 2 m²")
    @DecimalMax(value = "1000", message = "Area must be at most 1000 m²")
    private BigDecimal areaM2;

    @NotNull(message = "Max occupants is required")
    @Min(value = 1, message = "Max occupants must be at least 1")
    @Max(value = 10, message = "Max occupants must be at most 10")
    private Integer maxOccupants;

    @Valid
    @NotNull(message = "Address is required")
    private AddressInput address;

    @Valid
    @NotNull(message = "Location is required")
    private GeoPointInput location;

    @NotNull(message = "Amenity IDs are required; use an empty array for no amenities")
    @Size(max = 30, message = "Maximum 30 amenities")
    private Set<@NotNull @Positive Long> amenityIds;
}
