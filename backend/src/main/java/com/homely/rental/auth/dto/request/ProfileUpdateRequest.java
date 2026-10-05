package com.homely.rental.auth.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Profile update request (AUTH08).
 * Only allows updating whitelisted fields.
 */
@Getter
@Setter
public class ProfileUpdateRequest {
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @Size(max = 15, message = "Phone number must not exceed 15 characters")
    private String phone;

    @Size(max = 512, message = "Avatar URL must not exceed 512 characters")
    private String avatarUrl;
}
