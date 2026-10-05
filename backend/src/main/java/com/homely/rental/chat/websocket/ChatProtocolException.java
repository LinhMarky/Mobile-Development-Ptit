package com.homely.rental.chat.websocket;

final class ChatProtocolException extends RuntimeException {
    private final int status;
    private final String code;

    ChatProtocolException(int status, String code, String detail) {
        super(detail);
        this.status = status;
        this.code = code;
    }

    int status() { return status; }
    String code() { return code; }
}
