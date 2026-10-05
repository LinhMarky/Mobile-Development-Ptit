package com.homely.rental.common.exception;

/**
 * Thrown on domain-level conflicts (e.g., room not available, booking already exists).
 * Maps to HTTP 409.
 */
public class ConflictException extends RuntimeException {
    private final String code;

    public ConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
