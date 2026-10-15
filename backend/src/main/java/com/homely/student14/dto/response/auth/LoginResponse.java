package com.homely.student14.dto.response.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private int expiresIn;
    private UserAccountResponse user;
}
