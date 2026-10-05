package com.homely.rental.catalog.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Room patch/update request (HOST02).
 * unit_code and host are not editable.
 */
@Getter
@Setter
public class RoomPatchRequest {

    @NotNull(message = "expected_version is required for optimistic locking")
    private Integer expectedVersion;

    @Pattern(regexp = "SINGLE_ROOM|STUDIO|MINI_APARTMENT", message = "Room type is invalid")
    private String roomType;

    @DecimalMin(value = "2", message = "Area must be at least 2 m²")
    @DecimalMax(value = "1000", message = "Area must be at most 1000 m²")
    private BigDecimal areaM2;

    @Min(value = 1, message = "Max occupants must be at least 1")
    @Max(value = 10, message = "Max occupants must be at most 10")
    private Integer maxOccupants;

    @Valid
    private AddressInput address;

    @Valid
    private GeoPointInput location;

    @Size(max = 30, message = "Maximum 30 amenities")
    private Set<@NotNull @Positive Long> amenityIds;
}
