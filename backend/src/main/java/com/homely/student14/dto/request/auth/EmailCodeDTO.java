package com.homely.student14.dto.request.auth;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailCodeDTO {
    @NotBlank(message = "Vui lòng nhập email.")
    private String email;
}
