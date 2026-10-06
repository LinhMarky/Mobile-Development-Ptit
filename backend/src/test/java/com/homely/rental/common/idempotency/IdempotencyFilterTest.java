package com.homely.rental.common.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class IdempotencyFilterTest {
    private IdempotencyKeyRepository repository;
    private IdempotencyFilter filter;
    private Map<String, IdempotencyKeyEntity> records;
    private final AtomicInteger mutations = new AtomicInteger();

    @BeforeEach
    void setUp() {
        records = new HashMap<>();
        repository = mock(IdempotencyKeyRepository.class);
        when(repository.findByIdempotencyKey(anyString())).thenAnswer(call -> Optional.ofNullable(records.get(call.getArgument(0))));
        when(repository.saveAndFlush(any())).thenAnswer(call -> {
            IdempotencyKeyEntity value = call.getArgument(0);
            if (value.getId() == null && records.containsKey(value.getIdempotencyKey())) {
                throw new DataIntegrityViolationException("duplicate");
            }
            value.setId(1L);
            IdempotencyKeyEntity snapshot = new IdempotencyKeyEntity();
            BeanUtils.copyProperties(value, snapshot);
            records.put(value.getIdempotencyKey(), snapshot);
            return value;
        });
        doAnswer(call -> { records.remove(((IdempotencyKeyEntity) call.getArgument(0)).getIdempotencyKey()); return null; })
                .when(repository).delete(any());
        when(repository.deleteExpiredKey(anyString(), any(), any())).thenAnswer(call -> {
            IdempotencyKeyEntity value = records.get(call.getArgument(0));
            if (value != null && value.getStatus() == call.getArgument(1)
                    && !value.getExpiresAt().isAfter(call.getArgument(2))) {
                records.remove(value.getIdempotencyKey());
                return 1;
            }
            return 0;
        });
        filter = new IdempotencyFilter(repository, new ObjectMapper().findAndRegisterModules(), "api/v1");
        authenticate(1L);
    }

    @AfterEach void clearSecurity() { SecurityContextHolder.clearContext(); }

    @Test void replayPreservesBodyStatusContentTypeAndLocationWithoutSecondMutation() throws Exception {
        MockHttpServletResponse first = perform("{\"title\":\"Phòng\"}", this::create);
        MockHttpServletResponse replay = perform("{\"title\":\"Phòng\"}", this::create);
        assertThat(mutations).hasValue(1);
        assertThat(replay.getStatus()).isEqualTo(201);
        assertThat(replay.getContentAsByteArray()).isEqualTo(first.getContentAsByteArray());
        assertThat(replay.getContentType()).isEqualTo(first.getContentType());
        assertThat(replay.getHeader("Location")).isEqualTo("/api/v1/rooms/7");
    }

    @Test void differentBodyWithSameKeyConflicts() throws Exception {
        perform("{\"title\":\"first\"}", this::create);
        MockHttpServletResponse result = perform("{\"title\":\"second\"}", this::create);
        assertThat(result.getStatus()).isEqualTo(409);
        assertThat(result.getContentAsString()).contains("IDEMPOTENCY_KEY_REUSED");
        assertThat(mutations).hasValue(1);
    }

    @Test void legacySnapshotIsWrappedWithoutRepeatingMutation() throws Exception {
        perform("{}", this::create);
        records.values().iterator().next().setResponseBody("{\"id\":7,\"title\":\"Legacy room\"}");
        MockHttpServletResponse replay = perform("{}", this::create);
        var envelope = new ObjectMapper().readTree(replay.getContentAsString());
        assertThat(envelope.path("statusCode").asInt()).isEqualTo(201);
        assertThat(envelope.path("data").path("id").asLong()).isEqualTo(7);
        assertThat(envelope.path("data").path("title").asText()).isEqualTo("Legacy room");
        assertThat(envelope.size()).isEqualTo(3);
        assertThat(mutations).hasValue(1);
    }

    @Test void legacyPageAndEmptySuccessKeepTheirMeaningInsideEnvelope() throws Exception {
        perform("{}", this::create);
        var stored = records.values().iterator().next();
        stored.setResponseStatusCode(200);
        stored.setResponseBody("{\"data\":[],\"total_elements\":0,\"page_size\":20}");
        var page = new ObjectMapper().readTree(perform("{}", this::create).getContentAsString());
        assertThat(page.path("data").path("data").isArray()).isTrue();
        assertThat(page.path("data").path("page_size").asInt()).isEqualTo(20);
        stored.setResponseBody("");
        var empty = new ObjectMapper().readTree(perform("{}", this::create).getContentAsString());
        assertThat(empty.get("data").isNull()).isTrue();
        assertThat(empty.path("statusCode").asInt()).isEqualTo(200);
        assertThat(mutations).hasValue(1);
    }

    @Test void scopeSeparatesUsersEvenWithSameKeyAndBody() throws Exception {
        perform("{}", this::create);
        authenticate(2L);
        perform("{}", this::create);
        assertThat(mutations).hasValue(2);
        assertThat(records).hasSize(2);
    }

    @Test void scopeSeparatesPathsAndFingerprintSeparatesQueries() throws Exception {
        perform("{}", this::create);
        MockHttpServletRequest otherPath = request("{}");
        otherPath.setRequestURI("/api/v1/listings/room/7");
        filter.doFilter(otherPath, new MockHttpServletResponse(), this::create);
        MockHttpServletRequest otherQuery = request("{}");
        otherQuery.setQueryString("mode=changed");
        MockHttpServletResponse result = new MockHttpServletResponse();
        filter.doFilter(otherQuery, result, this::create);
        assertThat(result.getStatus()).isEqualTo(409);
        assertThat(mutations).hasValue(2);
    }

    @Test void requestBodyRemainsReadableByController() throws Exception {
        perform("{\"value\":42}", (request, response) -> {
            assertThat(request.getInputStream().readAllBytes()).isEqualTo("{\"value\":42}".getBytes(StandardCharsets.UTF_8));
            ((HttpServletResponse) response).setStatus(201);
        });
    }

    @Test void unauthenticatedRequestCannotReplayOrEvenReadTheStore() throws Exception {
        SecurityContextHolder.clearContext();
        MockHttpServletResponse result = perform("{}", this::create);
        assertThat(result.getStatus()).isEqualTo(401);
        verifyNoInteractions(repository);
        assertThat(mutations).hasValue(0);
    }

    @Test void missingKeyIsRejectedBeforeMutation() throws Exception {
        MockHttpServletRequest request = request("{}");
        request.removeHeader("Idempotency-Key");
        MockHttpServletResponse result = new MockHttpServletResponse();
        filter.doFilter(request, result, this::create);
        assertThat(result.getStatus()).isEqualTo(409);
        assertThat(result.getContentAsString()).contains("IDEMPOTENCY_KEY_REQUIRED", "field_errors", "trace_id");
        verifyNoInteractions(repository);
    }

    @Test void authenticationResponsesAreNeverStoredEvenWhenKeyIsPresent() throws Exception {
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = request("{}");
        request.setRequestURI("/api/v1/auth/login");
        filter.doFilter(request, new MockHttpServletResponse(), this::create);
        assertThat(mutations).hasValue(1);
        verifyNoInteractions(repository);
    }

    @Test void duplicateRequestDuringMutationConflicts() throws Exception {
        perform("{}", (request, response) -> {
            MockHttpServletResponse second = perform("{}", this::create);
            assertThat(second.getStatus()).isEqualTo(409);
            assertThat(second.getContentAsString()).contains("IDEMPOTENCY_CONFLICT");
            create(request, response);
        });
        assertThat(mutations).hasValue(1);
    }

    @Test void expiredCompletedClaimCanBeReusedButExpiredProcessingClaimCannot() throws Exception {
        perform("{}", this::create);
        IdempotencyKeyEntity old = records.values().iterator().next();
        old.setExpiresAt(Instant.now().minusSeconds(1));
        perform("{}", this::create);
        assertThat(mutations).hasValue(2);
        old = records.values().iterator().next();
        old.setStatus(IdempotencyKeyEntity.IdempotencyStatus.PROCESSING);
        old.setExpiresAt(Instant.now().minusSeconds(1));
        assertThat(perform("{}", this::create).getStatus()).isEqualTo(409);
        assertThat(mutations).hasValue(2);
    }

    @Test void validationFailureCanBeCorrectedAndRetried() throws Exception {
        perform("invalid", (request, response) -> ((HttpServletResponse) response).setStatus(400));
        assertThat(records).isEmpty();
        assertThat(perform("{}", this::create).getStatus()).isEqualTo(201);
    }

    @Test void unexpectedFailureRetainsClaimToAvoidDuplicatingUnknownOutcome() throws Exception {
        assertThatThrownBy(() -> perform("{}", (request, response) -> { throw new IllegalStateException("failure"); }))
                .isInstanceOf(IllegalStateException.class);
        assertThat(perform("{}", this::create).getStatus()).isEqualTo(409);
        assertThat(mutations).hasValue(0);
    }

    @Test void responseCacheFailurePreservesSuccessfulResponseAndProcessingClaim() throws Exception {
        doAnswer(call -> {
            IdempotencyKeyEntity claim = call.getArgument(0);
            if (claim.getStatus() == IdempotencyKeyEntity.IdempotencyStatus.COMPLETED) {
                throw new org.springframework.dao.DataAccessResourceFailureException("store unavailable");
            }
            claim.setId(1L);
            IdempotencyKeyEntity snapshot = new IdempotencyKeyEntity();
            BeanUtils.copyProperties(claim, snapshot);
            records.put(claim.getIdempotencyKey(), snapshot);
            return claim;
        }).when(repository).saveAndFlush(any());
        assertThat(perform("{}", this::create).getStatus()).isEqualTo(201);
        assertThat(records.values().iterator().next().getStatus()).isEqualTo(IdempotencyKeyEntity.IdempotencyStatus.PROCESSING);
        assertThat(perform("{}", this::create).getStatus()).isEqualTo(409);
        assertThat(mutations).hasValue(1);
    }

    private MockHttpServletResponse perform(String body, FilterChain chain) throws jakarta.servlet.ServletException, java.io.IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request(body), response, chain);
        return response;
    }

    private MockHttpServletRequest request(String body) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/rooms");
        request.setContentType("application/json");
        request.setCharacterEncoding("UTF-8");
        request.addHeader("Idempotency-Key", "retry-key");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }

    private void create(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) throws java.io.IOException {
        mutations.incrementAndGet();
        HttpServletResponse result = (HttpServletResponse) response;
        result.setStatus(201);
        result.setContentType("application/json;charset=UTF-8");
        result.setHeader("Location", "/api/v1/rooms/7");
        result.getWriter().write("{\"statusCode\":201,\"message\":\"Created\",\"data\":{\"id\":7,\"title\":\"Phòng đẹp\"}}");
    }

    private void authenticate(long id) {
        Jwt jwt = Jwt.withTokenValue("test").header("alg", "HS256").subject("user" + id + "@test.local")
                .claim("user", Map.of("id", id)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, java.util.List.of()));
    }
}
