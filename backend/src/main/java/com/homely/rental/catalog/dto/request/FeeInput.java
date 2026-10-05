package com.homely.rental.catalog.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Fee input DTO for creating/updating listing fees (CAT08–10).
 */
@Getter
@Setter
public class FeeInput {

    @NotBlank(message = "Fee code is required")
    @Size(max = 50, message = "Fee code must not exceed 50 characters")
    private String feeCode;

    @NotBlank(message = "Fee mode is required")
    @Size(max = 20, message = "Fee mode must not exceed 20 characters")
    private String feeMode = "FIXED";

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0", message = "Amount must be >= 0")
    private BigDecimal amountVnd;

    @Size(max = 50, message = "Unit name must not exceed 50 characters")
    private String unitName;

    @Size(max = 500, message = "Note must not exceed 500 characters")
    private String note;

    private int sortOrder = 0;
}
