package com.homely.student14.dto.request.auth;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyEmailDTO {
    @NotBlank(message = "Vui lòng nhập email.")
    private String email;

    @NotBlank(message = "Vui lòng nhập mã xác minh.")
    private String code;
}
