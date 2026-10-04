package com.homely.rental.catalog.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Listing write request (CAT01–04).
 * Used for both create and update listing operations.
 */
@Getter
@Setter
public class ListingWriteRequest {

    @Size(max = 150, message = "Title must not exceed 150 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @DecimalMin(value = "0", inclusive = false, message = "Rent must be greater than 0")
    private BigDecimal rentVnd;

    @DecimalMin(value = "0", message = "Deposit must be >= 0")
    private BigDecimal depositVnd;

    /** Required for update (optimistic locking) */
    private Integer expectedVersion;
}
