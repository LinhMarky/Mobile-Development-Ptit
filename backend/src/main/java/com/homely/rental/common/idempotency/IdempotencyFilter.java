package com.homely.rental.common.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homely.rental.common.dto.ProblemDTO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Catalog/chat JSON mutations, registered after HTTP authorization; never cache authentication responses. */
@Slf4j
public class IdempotencyFilter extends OncePerRequestFilter {
    private static final int MAX_BODY_BYTES = 1_048_576;
    private final IdempotencyKeyRepository repository;
    private final ObjectMapper objectMapper;
    private final String apiPrefix;

    public IdempotencyFilter(IdempotencyKeyRepository repository, ObjectMapper objectMapper, String apiPrefix) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.apiPrefix = "/" + apiPrefix.replaceAll("^/+|/+$", "");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) return true;
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !(matchesResource(path, "rooms") || matchesResource(path, "listings")
                || matchesResource(path, "wishlist") || matchesResource(path, "conversations")
                || matchesResource(path, "admin") || matchesResource(path, "notifications")
                || matchesResource(path, "bookings") || matchesResource(path, "payments")
                || matchesResource(path, "viewings") || matchesResource(path, "viewing-slots")
                || matchesResource(path, "reviews") || matchesResource(path, "reports"));
    }

    private boolean matchesResource(String path, String resource) {
        String base = apiPrefix + "/" + resource;
        return path.equals(base) || path.startsWith(base + "/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            problem(response, request, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Authentication is required");
            return;
        }
        String clientKey = request.getHeader("Idempotency-Key");
        if (clientKey == null || clientKey.isBlank() || clientKey.length() > 128) {
            problem(response, request, HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REQUIRED",
                    "A nonblank Idempotency-Key of at most 128 characters is required");
            return;
        }
        byte[] body = request.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
        if (body.length > MAX_BODY_BYTES) {
            problem(response, request, HttpStatus.PAYLOAD_TOO_LARGE, "REQUEST_TOO_LARGE", "Request body is too large");
            return;
        }
        Map<String, Object> user = jwt.getClaimAsMap("user");
        String principal = user != null && user.get("id") != null
                ? "id:" + user.get("id") : "subject:" + jwt.getSubject();
        String scope = hash((principal + "\nPOST\n" + request.getRequestURI() + "\n" + clientKey)
                .getBytes(StandardCharsets.UTF_8));
        String fingerprint = hash((String.valueOf(request.getQueryString()) + "\n"
                + String.valueOf(request.getContentType()) + "\n" + hash(body)).getBytes(StandardCharsets.UTF_8));

        Optional<IdempotencyKeyEntity> existing = repository.findByIdempotencyKey(scope);
        if (existing.isPresent() && existing.get().getStatus() == IdempotencyKeyEntity.IdempotencyStatus.COMPLETED
                && !existing.get().getExpiresAt().isAfter(Instant.now())) {
            repository.deleteExpiredKey(scope, IdempotencyKeyEntity.IdempotencyStatus.COMPLETED, Instant.now());
            existing = repository.findByIdempotencyKey(scope);
        }
        if (existing.isPresent()) {
            replayOrReject(existing.get(), fingerprint, request, response);
            return;
        }

        IdempotencyKeyEntity claim = new IdempotencyKeyEntity();
        claim.setIdempotencyKey(scope);
        claim.setRequestHash(fingerprint);
        claim.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        try {
            // The unique constraint arbitrates concurrent attempts to reserve the same key.
            repository.saveAndFlush(claim);
        } catch (DataIntegrityViolationException collision) {
            Optional<IdempotencyKeyEntity> winner = repository.findByIdempotencyKey(scope);
            if (winner.isPresent()) replayOrReject(winner.get(), fingerprint, request, response);
            else throw collision;
            return;
        }

        ContentCachingResponseWrapper wrapped = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(new ReplayableRequest(request, body), wrapped);
            if (wrapped.getStatus() >= 200 && wrapped.getStatus() < 300) {
                claim.setResponseStatusCode(wrapped.getStatus());
                claim.setResponseContentType(wrapped.getContentType());
                claim.setResponseLocation(wrapped.getHeader("Location"));
                claim.setResponseBody(new String(wrapped.getContentAsByteArray(), wrapped.getCharacterEncoding()));
                claim.setStatus(IdempotencyKeyEntity.IdempotencyStatus.COMPLETED);
                try {
                    repository.saveAndFlush(claim);
                } catch (RuntimeException storeFailure) {
                    // Domain work may already be committed: keep the database PROCESSING claim.
                    log.error("Unable to cache idempotent response; claim requires reconciliation", storeFailure);
                }
            } else if (wrapped.getStatus() >= 400 && wrapped.getStatus() < 500) {
                repository.delete(claim);
            }
            // 5xx / thrown failures retain the claim because the domain outcome may be ambiguous.
        } finally {
            wrapped.copyBodyToResponse();
        }
    }

    private void replayOrReject(IdempotencyKeyEntity claim, String fingerprint,
                                HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!fingerprint.equals(claim.getRequestHash())) {
            problem(response, request, HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED",
                    "This idempotency key was used with a different request");
        } else if (claim.getStatus() != IdempotencyKeyEntity.IdempotencyStatus.COMPLETED) {
            problem(response, request, HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT",
                    "A request with this idempotency key is already being processed");
        } else {
            response.setStatus(claim.getResponseStatusCode());
            Charset charset = StandardCharsets.UTF_8;
            if (claim.getResponseContentType() != null) {
                response.setContentType(claim.getResponseContentType());
                Charset declared = MediaType.parseMediaType(claim.getResponseContentType()).getCharset();
                if (declared != null) charset = declared;
            }
            if (claim.getResponseLocation() != null) response.setHeader("Location", claim.getResponseLocation());
            if (claim.getResponseBody() != null) response.getOutputStream().write(claim.getResponseBody().getBytes(charset));
        }
    }

    private void problem(HttpServletResponse response, HttpServletRequest request, HttpStatus status,
                         String code, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ProblemDTO.builder()
                .type("urn:problem:" + code.toLowerCase(java.util.Locale.ROOT).replace('_', '-'))
                .title(status.getReasonPhrase()).status(status.value()).detail(detail)
                .instance(request.getRequestURI()).code(code).traceId(UUID.randomUUID().toString())
                .timestamp(Instant.now()).build());
    }

    private static String hash(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
