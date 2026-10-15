package com.homely.student14.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
public class ApiResponse<T> {
    @JsonProperty("statusCode")
    private int statusCode;

    private String message;
    private T data;

    public static ApiResponse<?> ok(Object data) {
        return new ApiResponse<>(200, "OK", data == null ? Map.of() : data);
    }
}
