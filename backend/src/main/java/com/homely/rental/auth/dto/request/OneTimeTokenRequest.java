package com.homely.rental.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OneTimeTokenRequest {
    @NotBlank(message = "Token is required")
    @Size(min = 43, max = 512, message = "Token must be between 43 and 512 characters")
    private String token;
}
