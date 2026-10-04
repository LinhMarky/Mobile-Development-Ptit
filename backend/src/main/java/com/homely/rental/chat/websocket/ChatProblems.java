package com.homely.rental.chat.websocket;

import com.homely.rental.chat.service.ChatException;
import com.homely.rental.common.dto.ProblemDTO;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

final class ChatProblems {
    private ChatProblems() { }

    static ProblemDTO from(Throwable error) {
        Throwable cause = error;
        // Spring wraps inbound channel exceptions in MessageDeliveryException.
        for (int depth = 0; cause != null && depth < 12; depth++, cause = cause.getCause()) {
            if (cause instanceof ChatProtocolException problem) {
                return create(problem.status(), problem.code(), problem.getMessage());
            }
            if (cause instanceof ChatException problem) {
                return create(problem.getStatus(), problem.getCode(), problem.getMessage());
            }
            if (cause instanceof MethodArgumentNotValidException) {
                return create(400, "VALIDATION_FAILED", "One or more message fields are invalid");
            }
            if (cause instanceof MessageConversionException) {
                return create(400, "INVALID_JSON", "The message body is malformed or contains an invalid value");
            }
        }
        return create(500, "INTERNAL_ERROR", "The message could not be processed. Please try again later.");
    }

    static ProblemDTO create(int status, String code, String detail) {
        HttpStatus httpStatus = HttpStatus.resolve(status);
        return ProblemDTO.builder()
                .type("urn:problem:" + code.toLowerCase(Locale.ROOT).replace('_', '-'))
                .title(httpStatus == null ? "Chat Error" : httpStatus.getReasonPhrase())
                .status(status).detail(detail).instance("/ws").code(code)
                .fieldErrors(List.of()).traceId(UUID.randomUUID().toString()).timestamp(Instant.now())
                .build();
    }
}
