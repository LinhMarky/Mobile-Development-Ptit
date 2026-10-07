package com.homely.rental.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Problem details carried in HTTP RestResponse.data, or directly in STOMP error frames.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemDTO {

    /** URI reference: urn:problem:<code-lowercase-with-dashes> */
    private String type;

    /** Short, stable error title */
    private String title;

    /** HTTP status code (mirrors response status) */
    private int status;

    /** Human-readable explanation (no stack traces/SQL/tokens) */
    private String detail;

    /** Request path (no query params containing tokens) */
    private String instance;

    /** Machine-readable error code in UPPER_SNAKE_CASE */
    private String code;

    /** Always present; empty array if no field-level errors */
    @JsonProperty("field_errors")
    @Builder.Default
    private List<FieldErrorDTO> fieldErrors = new ArrayList<>();

    /** Server-generated trace ID */
    @JsonProperty("trace_id")
    private String traceId;

    /** UTC server timestamp */
    private Instant timestamp;
}
