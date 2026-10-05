package com.homely.rental.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Account deletion request (AUTH12).
 */
@Getter
@Setter
public class DeletionRequest {
    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @Size(max = 1000, message = "Reason must not exceed 1000 characters")
    private String reason = "";
}
