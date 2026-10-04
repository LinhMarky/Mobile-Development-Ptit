package com.homely.rental.auth.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class UserResponse {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private boolean host;
    private boolean emailVerified;
    private String status;
    private Instant createdAt;
}
