package com.homely.rental.chat.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homely.rental.chat.service.ChatException;
import com.homely.rental.chat.service.ChatService;
import com.homely.rental.common.dto.ProblemDTO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Recheck account/membership before cached POST replay, and bound chat REST traffic per account. */
public final class ChatHttpGuardFilter extends OncePerRequestFilter {
    private static final Pattern MEMBER_PATH = Pattern.compile("/(\\d+)(?:/.*)?");
    private final ChatService service;
    private final ObjectMapper mapper;
    private final String base;
    private final int requestsPerMinute;
    private final Map<String, Window> windows = new HashMap<>();
    private long nextCleanup;

    public ChatHttpGuardFilter(ChatService service, ObjectMapper mapper, String apiPrefix, int requestsPerMinute) {
        if (requestsPerMinute < 1) throw new IllegalArgumentException("Chat REST rate limit must be positive");
        this.service = service;
        this.mapper = mapper;
        this.base = "/" + apiPrefix.replaceAll("^/+|/+$", "") + "/conversations";
        this.requestsPerMinute = requestsPerMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !path.equals(base) && !path.startsWith(base + "/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                throw new ChatException(401, "AUTHENTICATION_REQUIRED", "Authentication is required");
            }
            String email = authentication.getName();
            checkRate(email);
            var user = service.requireActiveUser(email);
            String path = request.getRequestURI().substring(request.getContextPath().length());
            if (path.equals(base) && "POST".equals(request.getMethod()) && !user.isEmailVerified()) {
                throw new ChatException(403, "EMAIL_NOT_VERIFIED", "Verify your email before creating a conversation");
            }
            Matcher member = MEMBER_PATH.matcher(path.substring(base.length()));
            if (member.matches()) {
                try {
                    service.requireMember(Long.valueOf(member.group(1)), email);
                } catch (NumberFormatException invalidId) {
                    throw new ChatException(400, "VALIDATION_FAILED", "Invalid conversation ID");
                }
            }
            chain.doFilter(request, response);
        } catch (ChatException failure) {
            response.setStatus(failure.getStatus());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            if (failure.getStatus() == 429) response.setHeader("Retry-After", "60");
            HttpStatus status = HttpStatus.valueOf(failure.getStatus());
            mapper.writeValue(response.getOutputStream(), ProblemDTO.builder()
                    .type("urn:problem:" + failure.getCode().toLowerCase(Locale.ROOT).replace('_', '-'))
                    .title(status.getReasonPhrase()).status(status.value()).code(failure.getCode())
                    .detail(failure.getMessage()).instance(request.getRequestURI()).traceId(UUID.randomUUID().toString())
                    .timestamp(Instant.now()).build());
        }
    }

    private synchronized void checkRate(String email) {
        long now = System.currentTimeMillis();
        if (now >= nextCleanup) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().start >= 60_000);
            nextCleanup = now + 60_000;
        }
        Window window = windows.get(email);
        if (window == null || now - window.start >= 60_000) {
            if (window == null && windows.size() >= 10_000) {
                throw new ChatException(429, "RATE_LIMITED", "Please retry shortly");
            }
            window = new Window(now);
            windows.put(email, window);
        }
        if (++window.count > requestsPerMinute) {
            throw new ChatException(429, "RATE_LIMITED", "Too many chat requests; retry in one minute");
        }
    }

    private static final class Window {
        private final long start;
        private int count;
        private Window(long start) { this.start = start; }
    }
}
