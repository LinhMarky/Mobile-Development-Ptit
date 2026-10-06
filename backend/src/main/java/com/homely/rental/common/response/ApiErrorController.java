package com.homely.rental.common.response;

import com.homely.rental.common.dto.ProblemDTO;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Servlet error dispatches also use ProblemDTO instead of Spring Boot's default error map. */
@Hidden
@RestController
public class ApiErrorController implements ErrorController {
    @RequestMapping("${server.error.path:${error.path:/error}}")
    public ResponseEntity<ProblemDTO> error(HttpServletRequest request) {
        Object errorStatus = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = errorStatus instanceof Integer value && value >= 400 && value <= 599 ? value : 500;
        String code = switch (status) {
            case 400 -> "VALIDATION_FAILED";
            case 401 -> "AUTHENTICATION_REQUIRED";
            case 403 -> "FORBIDDEN";
            case 404 -> "RESOURCE_NOT_FOUND";
            case 405 -> "METHOD_NOT_ALLOWED";
            case 406 -> "NOT_ACCEPTABLE";
            case 415 -> "UNSUPPORTED_MEDIA_TYPE";
            default -> status >= 500 ? "INTERNAL_ERROR" : "REQUEST_FAILED";
        };
        Object originalUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        String instance = originalUri instanceof String value ? value.split("\\?", 2)[0] : request.getRequestURI();
        String detail = status >= 500 ? "An unexpected error occurred" : "The request could not be completed";
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON)
                .header("Cache-Control", "no-store").body(ApiProblems.create(status, code, detail, instance));
    }
}
