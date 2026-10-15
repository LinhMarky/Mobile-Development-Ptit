package com.homely.student14.dto.response.auth;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserAccountResponse {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private boolean emailVerified;
    private String status;
    private List<String> roles;
}
