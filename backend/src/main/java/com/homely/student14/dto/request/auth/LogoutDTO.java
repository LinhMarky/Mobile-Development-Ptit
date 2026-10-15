package com.homely.student14.dto.request.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LogoutDTO {
    private String refreshToken;
}
