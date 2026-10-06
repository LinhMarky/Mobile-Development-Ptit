package com.homely.rental.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.assertj.core.api.Assertions.*;

class AuthRateLimitFilterTest {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final AuthRateLimitProperties properties = new AuthRateLimitProperties();
    private final AuthRateLimitFilter filter = new AuthRateLimitFilter(properties,
            new AuthRequestLimiter(properties), mapper, "/custom/v2");

    @AfterEach void clearSecurityContext() { SecurityContextHolder.clearContext(); }

    @Test void accountLimitSurvivesIpChangesAndNormalizesEmail() throws Exception {
        properties.getLogin().setPerIdentity(1);
        assertThat(call("login", "{\"email\":\"User@Example.test\"}", "192.0.2.1").getStatus()).isEqualTo(200);
        var blocked = call("login", "{\"email\":\"user@example.test\"}", "192.0.2.2");
        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isEqualTo("60");
        var json = mapper.readTree(blocked.getContentAsByteArray());
        assertThat(json.size()).isEqualTo(3);
        assertThat(json.path("statusCode").asInt()).isEqualTo(429);
        assertThat(json.path("data").path("code").asText()).isEqualTo("RATE_LIMITED");
        assertThat(json.path("data").path("trace_id").asText()).isNotBlank();
    }

    @Test void ipLimitCountsMalformedRequestsAndIgnoresSpoofedForwardingHeaders() throws Exception {
        properties.getLogin().setPerIp(1);
        call("login", "{broken", "192.0.2.1");
        var blocked = call("login", "{\"email\":\"new@example.test\"}", "192.0.2.1");
        assertThat(blocked.getStatus()).isEqualTo(429);
    }

    @Test void bodyRemainsReadableByTheController() throws Exception {
        String body = "{\"email\":\"hello@example.test\",\"password\":\"do-not-log\"}";
        var request = request("login", body, "192.0.2.1");
        AtomicBoolean reached = new AtomicBoolean();
        filter.doFilter(request, new MockHttpServletResponse(), (wrapped, response) -> {
            assertThat(new String(wrapped.getInputStream().readAllBytes(), StandardCharsets.UTF_8)).isEqualTo(body);
            reached.set(true);
        });
        assertThat(reached).isTrue();
    }

    @Test void resendLimitIsPerAuthenticatedAccountEvenOnAnotherIp() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user@example.test", "unused", java.util.List.of()));
        assertThat(call("verify-email/resend", "", "192.0.2.1").getStatus()).isEqualTo(200);
        assertThat(call("verify-email/resend", "", "192.0.2.2").getStatus()).isEqualTo(429);
    }

    @Test void rejectsOversizedAuthBodyBeforeParsing() throws Exception {
        var response = call("login", "x".repeat(16 * 1024 + 1), "192.0.2.1");
        assertThat(response.getStatus()).isEqualTo(413);
    }

    @Test void nonAuthEndpointsRemainUnaffected() throws Exception {
        var request = request("account", "", "192.0.2.1");
        request.setMethod("GET");
        AtomicBoolean reached = new AtomicBoolean();
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> reached.set(true));
        assertThat(reached).isTrue();
    }

    private MockHttpServletResponse call(String operation, String body, String ip) throws Exception {
        var response = new MockHttpServletResponse();
        filter.doFilter(request(operation, body, ip), response, (req, res) -> { });
        return response;
    }

    private MockHttpServletRequest request(String operation, String body, String ip) {
        var request = new MockHttpServletRequest("POST", "/custom/v2/auth/" + operation);
        request.setRemoteAddr(ip);
        request.addHeader("X-Forwarded-For", java.util.UUID.randomUUID().toString());
        request.setContentType("application/json");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }
}
