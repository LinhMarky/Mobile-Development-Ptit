package com.homely.rental.auth.security;

import lombok.Getter;

@Getter
public class AccountAccessException extends RuntimeException {
    private final int status;
    private final String code;

    public AccountAccessException(int status, String code, String detail) {
        super(detail);
        this.status = status;
        this.code = code;
    }
}
