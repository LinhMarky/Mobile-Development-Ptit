package com.homely.student14.dto.request.push;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RemoveDeviceTokenDTO {
    @NotBlank(message = "Mã cài đặt không được để trống.")
    @Size(max = 36, message = "Mã cài đặt không hợp lệ.")
    private String installationId;
}
