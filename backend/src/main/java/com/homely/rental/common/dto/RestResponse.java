package com.homely.rental.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RestResponse<T> {
    private int statusCode;
    private String error;
    private Object message;
    private T data;
}
