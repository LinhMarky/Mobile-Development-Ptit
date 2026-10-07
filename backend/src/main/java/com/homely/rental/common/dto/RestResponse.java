package com.homely.rental.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpStatus;

/** Common HTTP JSON envelope. DTOs inside data retain their snake_case field names. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record RestResponse<T>(@JsonProperty("statusCode") int statusCode,
                              @JsonProperty("message") String message,
                              @JsonProperty("data") T data) {
    public static <T> RestResponse<T> of(int status, T data) {
        HttpStatus httpStatus = HttpStatus.resolve(status);
        String message = data instanceof ProblemDTO problem ? problem.getDetail()
                : httpStatus == null ? "Request completed" : httpStatus.getReasonPhrase();
        return new RestResponse<>(status, message, data);
    }
}
