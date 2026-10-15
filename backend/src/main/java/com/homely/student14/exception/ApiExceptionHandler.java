package com.homely.student14.exception;

import com.homely.student14.dto.ApiResponse;
import com.homely.student14.dto.response.ErrorResponse;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleRequestError(
            ResponseStatusException exception) {
        int statusCode = exception.getStatusCode().value();
        String errorCode = "REQUEST_FAILED";
        if (statusCode == 403) {
            errorCode = "FORBIDDEN";
        } else if (statusCode == 409) {
            errorCode = "CONFLICT";
        }
        return error(statusCode, errorCode, exception.getReason());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleConflictingData(
            DataIntegrityViolationException exception) {
        return error(409, "CONFLICT", "Dữ liệu trùng hoặc không phù hợp. Hãy tải lại.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleValidationError(
            MethodArgumentNotValidException exception) {
        FieldError firstError = exception.getBindingResult().getFieldError();
        String message = "Dữ liệu nhập không hợp lệ.";
        if (firstError != null) {
            message = firstError.getField() + ": " + firstError.getDefaultMessage();
        }
        return error(400, "BAD_REQUEST", message);
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse<ErrorResponse>> handleInvalidInput(Exception exception) {
        return error(400, "BAD_REQUEST", "Dữ liệu nhập không hợp lệ.");
    }

    private ResponseEntity<ApiResponse<ErrorResponse>> error(
            int statusCode, String code, String message) {
        ErrorResponse details = new ErrorResponse(code, message);
        ApiResponse<ErrorResponse> response = new ApiResponse<>(statusCode, message, details);
        return ResponseEntity.status(statusCode).body(response);
    }
}
