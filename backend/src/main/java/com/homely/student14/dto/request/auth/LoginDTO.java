package com.homely.student14.dto.request.auth;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginDTO {
    @NotBlank(message = "Vui lòng nhập email.")
    private String email;

    @NotBlank(message = "Vui lòng nhập mật khẩu.")
    private String password;

    @NotBlank(message = "Thiếu mã cài đặt ứng dụng.")
    private String installationId;

    @NotBlank(message = "Thiếu tên thiết bị.")
    private String deviceName;
}
