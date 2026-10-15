package com.homely.student14.dto.request.auth;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterDTO {
    @NotBlank(message = "Vui lòng nhập email.")
    private String email;

    @NotBlank(message = "Vui lòng nhập mật khẩu.")
    private String password;

    @NotBlank(message = "Vui lòng nhập họ tên.")
    private String fullName;

    private String phone;
}
