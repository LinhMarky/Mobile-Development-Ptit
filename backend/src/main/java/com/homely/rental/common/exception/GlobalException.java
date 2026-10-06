package com.homely.rental.common.exception;

import com.homely.rental.chat.service.ChatException;
import com.homely.rental.auth.security.AccountAccessException;
import com.homely.rental.common.dto.FieldErrorDTO;
import com.homely.rental.common.dto.ProblemDTO;
import com.homely.rental.common.response.ApiProblems;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalException {
    @ExceptionHandler({UsernameNotFoundException.class, BadCredentialsException.class})
    public ResponseEntity<ProblemDTO> authentication(Exception ex, HttpServletRequest request) {
        return problem(401, "INVALID_CREDENTIALS", "Invalid email or password", request, List.of());
    }

    @ExceptionHandler({ResourceNotFoundException.class, NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ProblemDTO> notFound(Exception ex, HttpServletRequest request) {
        return problem(404, "RESOURCE_NOT_FOUND", "Resource not found", request, List.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDTO> unreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return problem(400, "INVALID_JSON", "Request body is missing or invalid", request, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDTO> methodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        var response = problem(405, "METHOD_NOT_ALLOWED", "This HTTP method is not supported for this resource", request, List.of());
        return ResponseEntity.status(405).headers(ex.getHeaders()).headers(response.getHeaders()).body(response.getBody());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDTO> unsupportedMediaType(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return problem(415, "UNSUPPORTED_MEDIA_TYPE", "The request Content-Type is not supported", request, List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ProblemDTO> notAcceptable(HttpMediaTypeNotAcceptableException ex, HttpServletRequest request) {
        return problem(406, "NOT_ACCEPTABLE", "The requested response media type is not supported", request, List.of());
    }

    // MethodArgumentNotValidException extends BindException. Never reflect rejected binding values.
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ProblemDTO> binding(BindException ex, HttpServletRequest request) {
        var fields = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorDTO(error.getField(), error.isBindingFailure()
                        ? "Invalid value" : error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage()))
                .toList();
        return validation(request, fields);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDTO> wrongType(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return validation(request, List.of(new FieldErrorDTO(ex.getName(), "Invalid value")));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDTO> missingParameter(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return validation(request, List.of(new FieldErrorDTO(ex.getParameterName(), "This parameter is required")));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ProblemDTO> methodValidation(HandlerMethodValidationException ex, HttpServletRequest request) {
        if (ex.isForReturnValue()) return unexpected(ex, request);
        var fields = ex.getAllValidationResults().stream().map(result -> {
            String name = result.getMethodParameter().getParameterName();
            return new FieldErrorDTO(name == null ? "request" : name, "Invalid value");
        }).toList();
        return validation(request, fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDTO> constraint(ConstraintViolationException ex, HttpServletRequest request) {
        var fields = ex.getConstraintViolations().stream()
                .map(error -> new FieldErrorDTO(error.getPropertyPath().toString(), "Invalid value")).toList();
        return validation(request, fields);
    }

    @ExceptionHandler({IdInvalidException.class, IllegalArgumentException.class})
    public ResponseEntity<ProblemDTO> invalid(Exception ex, HttpServletRequest request) {
        return problem(400, "VALIDATION_FAILED", "Invalid request", request, List.of());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ProblemDTO> conflict(ConflictException ex, HttpServletRequest request) {
        return problem(409, ex.getCode(), ex.getMessage(), request, List.of());
    }

    @ExceptionHandler({DataIntegrityViolationException.class, ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<ProblemDTO> dataConflict(Exception ex, HttpServletRequest request) {
        return problem(409, "CONFLICT", "The resource conflicts with the current state", request, List.of());
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ProblemDTO> storage(StorageException ex, HttpServletRequest request) {
        return problem(400, "VALIDATION_FAILED", "File upload failed", request, List.of());
    }

    @ExceptionHandler({PermissionException.class, AccessDeniedException.class})
    public ResponseEntity<ProblemDTO> forbidden(Exception ex, HttpServletRequest request) {
        return problem(403, "FORBIDDEN", "You do not have permission to perform this action", request, List.of());
    }

    @ExceptionHandler(ChatException.class)
    public ResponseEntity<ProblemDTO> chat(ChatException ex, HttpServletRequest request) {
        return problem(ex.getStatus(), ex.getCode(), ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(AccountAccessException.class)
    public ResponseEntity<ProblemDTO> account(AccountAccessException ex, HttpServletRequest request) {
        return problem(ex.getStatus(), ex.getCode(), ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(org.springframework.mail.MailException.class)
    public ResponseEntity<ProblemDTO> mailUnavailable(org.springframework.mail.MailException ex, HttpServletRequest request) {
        log.warn("Verification email delivery failed ({})", ex.getClass().getSimpleName());
        return problem(503, "EMAIL_UNAVAILABLE", "Email service is unavailable. Please retry later.", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDTO> unexpected(Exception ex, HttpServletRequest request) {
        var response = problem(500, "INTERNAL_ERROR", "An unexpected error occurred", request, List.of());
        log.error("Unhandled {} (trace {})", ex.getClass().getSimpleName(), response.getBody().getTraceId());
        return response;
    }

    private ResponseEntity<ProblemDTO> validation(HttpServletRequest request, List<FieldErrorDTO> fields) {
        return problem(400, "VALIDATION_FAILED", "Request validation failed", request, fields);
    }

    private ResponseEntity<ProblemDTO> problem(int status, String code, String detail,
                                              HttpServletRequest request, List<FieldErrorDTO> fields) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON)
                .header("Cache-Control", "no-store")
                .body(ApiProblems.create(status, code, detail, request.getRequestURI(), fields));
    }
}
