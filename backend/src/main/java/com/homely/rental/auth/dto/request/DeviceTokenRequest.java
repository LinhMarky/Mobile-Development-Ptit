package com.homely.rental.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Device token registration request (AUTH10).
 */
@Getter
@Setter
public class DeviceTokenRequest {
    @NotBlank(message = "Token is required")
    @Size(min = 1, max = 4096, message = "Token must be between 1 and 4096 characters")
    private String token;

    @NotNull(message = "Platform is required")
    private String platform = "ANDROID";

    @NotBlank(message = "Device name is required")
    @Size(min = 1, max = 100, message = "Device name must be between 1 and 100 characters")
    private String deviceName;

    @NotBlank(message = "Installation ID is required")
    private String installationId;
}
