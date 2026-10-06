package com.homely.rental.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homely.rental.common.http.CachedBodyRequest;
import com.homely.rental.common.response.ApiProblems;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/** Counts every attempt, including invalid JSON and unsuccessful authentication. */
public final class AuthRateLimitFilter extends OncePerRequestFilter {
    private static final int MAX_BODY_BYTES = 16 * 1024;
    private final AuthRateLimitProperties properties;
    private final AuthRequestLimiter limiter;
    private final ObjectMapper mapper;
    private final String base;

    public AuthRateLimitFilter(AuthRateLimitProperties properties, AuthRequestLimiter limiter,
                               ObjectMapper mapper, String apiPrefix) {
        this.properties = properties;
        this.limiter = limiter;
        this.mapper = mapper;
        this.base = "/" + apiPrefix.replaceAll("^/+|/+$", "") + "/auth/";
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!properties.isEnabled() || !"POST".equals(request.getMethod()) || !path.startsWith(base)) {
            chain.doFilter(request, response);
            return;
        }
        String operation = path.substring(base.length());
        AuthRateLimitProperties.Limit limit = limitFor(operation);
        if (limit == null) {
            chain.doFilter(request, response);
            return;
        }
        // Never trust a client-supplied X-Forwarded-For header for rate-limit identity.
        long retry = limiter.acquire(operation + ":ip:" + request.getRemoteAddr(), limit.getPerIp());
        if (rejectIfLimited(request, response, retry)) return;

        byte[] body = request.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
        if (body.length > MAX_BODY_BYTES) {
            ApiProblems.write(mapper, request, response, 413, "REQUEST_TOO_LARGE", "Auth request body is too large");
            return;
        }
        String identity = identityFor(operation, body);
        if (identity != null) {
            retry = limiter.acquire(operation + ":identity:" + hash(identity), limit.getPerIdentity());
            if (rejectIfLimited(request, response, retry)) return;
        }
        chain.doFilter(new CachedBodyRequest(request, body), response);
    }

    private AuthRateLimitProperties.Limit limitFor(String operation) {
        return switch (operation) {
            case "login" -> properties.getLogin();
            case "register" -> properties.getRegister();
            case "refresh" -> properties.getRefresh();
            case "verify-email" -> properties.getVerifyEmail();
            case "verify-email/resend" -> properties.getResend();
            default -> null;
        };
    }

    private String identityFor(String operation, byte[] body) {
        if ("verify-email/resend".equals(operation)) {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            return auth != null && auth.isAuthenticated() ? auth.getName() : null;
        }
        try {
            var json = mapper.readTree(body);
            String field = switch (operation) {
                case "login", "register" -> "email";
                case "refresh" -> "refresh_token";
                default -> "token";
            };
            if (json == null || !json.path(field).isTextual()) return null;
            String value = json.path(field).asText();
            if (value.isBlank()) return null;
            return field.equals("email") ? value.strip().toLowerCase(Locale.ROOT) : value;
        } catch (IOException ex) {
            return null; // MVC retains responsibility for the INVALID_JSON response.
        }
    }

    private boolean rejectIfLimited(HttpServletRequest request, HttpServletResponse response, long retry)
            throws IOException {
        if (retry == 0) return false;
        response.setHeader("Retry-After", Long.toString(retry));
        ApiProblems.write(mapper, request, response, 429, "RATE_LIMITED", "Too many attempts. Please retry later.");
        return true;
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required", ex);
        }
    }
}
