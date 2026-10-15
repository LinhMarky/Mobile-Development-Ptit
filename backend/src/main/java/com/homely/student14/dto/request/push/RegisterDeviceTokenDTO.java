package com.homely.student14.dto.request.push;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterDeviceTokenDTO {
    @NotBlank(message = "Token thiết bị không được để trống.")
    @Size(max = 4096, message = "Token thiết bị quá dài.")
    private String token;

    @NotBlank(message = "Mã cài đặt không được để trống.")
    @Size(max = 36, message = "Mã cài đặt không hợp lệ.")
    private String installationId;

    @Size(max = 100, message = "Tên thiết bị tối đa 100 ký tự.")
    private String deviceName;
}
