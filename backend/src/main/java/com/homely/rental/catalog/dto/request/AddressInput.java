package com.homely.rental.catalog.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Address input DTO for room creation/update.
 * ward_code and ward_name must be both present or both null.
 */
@Getter
@Setter
public class AddressInput {

    @NotBlank(message = "Address line is required")
    @Size(min = 5, max = 300, message = "Address line must be between 5 and 300 characters")
    private String line;

    @NotBlank(message = "Province code is required")
    @Size(min = 1, max = 20)
    private String provinceCode;

    @NotBlank(message = "Province name is required")
    @Size(min = 1, max = 100)
    private String provinceName;

    @Size(min = 1, max = 20)
    private String wardCode;

    @Size(min = 1, max = 100)
    private String wardName;

    @JsonIgnore
    @AssertTrue(message = "ward_code and ward_name must both be nonblank or both be null")
    public boolean isWardPairValid() {
        return (wardCode == null && wardName == null)
                || (wardCode != null && !wardCode.isBlank() && wardName != null && !wardName.isBlank());
    }
}
