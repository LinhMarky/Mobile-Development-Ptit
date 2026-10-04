package com.homely.rental.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshRequest {
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;

    // Optional: used during token rotation to maintain device session identity
    private String installationId;
    private String deviceName;
}

