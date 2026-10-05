package com.homely.rental.chat.service;

import lombok.Getter;

@Getter
public class ChatException extends RuntimeException {
    private final int status;
    private final String code;

    public ChatException(int status, String code, String detail) {
        super(detail);
        this.status = status;
        this.code = code;
    }
}
