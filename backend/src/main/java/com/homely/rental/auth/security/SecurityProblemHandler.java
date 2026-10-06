package com.homely.rental.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homely.rental.common.response.ApiProblems;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

/** Security errors occur before MVC advice, but still use the same public error schema. */
public final class SecurityProblemHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final ObjectMapper mapper;

    public SecurityProblemHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException failure)
            throws IOException {
        boolean hasToken = request.getHeader("Authorization") != null;
        response.setHeader("WWW-Authenticate", "Bearer");
        write(request, response, HttpStatus.UNAUTHORIZED,
                hasToken ? "INVALID_ACCESS_TOKEN" : "AUTHENTICATION_REQUIRED",
                hasToken ? "The access token is invalid or expired" : "Authentication is required");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException failure)
            throws IOException {
        write(request, response, HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission to perform this action");
    }

    private void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
                       String code, String detail) throws IOException {
        ApiProblems.write(mapper, request, response, status.value(), code, detail);
    }
}
