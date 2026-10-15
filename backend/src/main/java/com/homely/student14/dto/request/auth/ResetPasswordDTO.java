package com.homely.student14.dto.request.auth;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordDTO extends VerifyEmailDTO {
    @NotBlank(message = "Vui lòng nhập mật khẩu mới.")
    private String newPassword;
}
