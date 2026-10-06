package com.homely.rental.common.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homely.rental.common.dto.FieldErrorDTO;
import com.homely.rental.common.dto.ProblemDTO;
import com.homely.rental.common.dto.RestResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Shared public error shape for MVC, security filters and the chat transport. */
public final class ApiProblems {
    private ApiProblems() { }

    public static ProblemDTO create(int status, String code, String detail, String instance) {
        return create(status, code, detail, instance, List.of());
    }

    public static ProblemDTO create(int status, String code, String detail, String instance, List<FieldErrorDTO> fields) {
        HttpStatus httpStatus = HttpStatus.resolve(status);
        return ProblemDTO.builder()
                .type("urn:problem:" + code.toLowerCase(Locale.ROOT).replace('_', '-'))
                .title(httpStatus == null ? "Request failed" : httpStatus.getReasonPhrase())
                .status(status).code(code).detail(detail).instance(instance)
                .fieldErrors(fields == null ? List.of() : List.copyOf(fields))
                .traceId(UUID.randomUUID().toString()).timestamp(Instant.now()).build();
    }

    public static void write(ObjectMapper mapper, HttpServletRequest request, HttpServletResponse response,
                             int status, String code, String detail) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Cache-Control", "no-store");
        mapper.writeValue(response.getOutputStream(), RestResponse.of(status, create(status, code, detail, request.getRequestURI())));
    }
}
