package com.homely.student14.dto.response.auth;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailCodeResponse {
    private String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String demoCode;
}
