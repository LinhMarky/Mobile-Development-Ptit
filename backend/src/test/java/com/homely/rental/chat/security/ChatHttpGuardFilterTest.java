package com.homely.rental.chat.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homely.rental.auth.entity.User;
import com.homely.rental.chat.service.ChatService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ChatHttpGuardFilterTest {
    @AfterEach void resetSecurity() { SecurityContextHolder.clearContext(); }

    @Test void rateLimitIsSharedByAccountAndReturnsRetryAfter() throws Exception {
        ChatService service = mock(ChatService.class);
        when(service.requireActiveUser(anyString())).thenReturn(new User());
        ChatHttpGuardFilter filter = new ChatHttpGuardFilter(service, new ObjectMapper().findAndRegisterModules(), "api/v1", 2);
        AtomicInteger calls = new AtomicInteger();
        authenticate("one@example.test");
        assertThat(request(filter, calls).getStatus()).isEqualTo(200);
        assertThat(request(filter, calls).getStatus()).isEqualTo(200);
        MockHttpServletResponse limited = request(filter, calls);
        assertThat(limited.getStatus()).isEqualTo(429);
        assertThat(limited.getHeader("Retry-After")).isEqualTo("60");
        assertThat(limited.getContentAsString()).contains("RATE_LIMITED", "field_errors", "trace_id");
        authenticate("two@example.test");
        assertThat(request(filter, calls).getStatus()).isEqualTo(200);
        assertThat(calls).hasValue(3);
    }

    @Test void unrelatedEndpointsAreUnaffected() throws Exception {
        ChatService service = mock(ChatService.class);
        ChatHttpGuardFilter filter = new ChatHttpGuardFilter(service, new ObjectMapper(), "api/v1", 2);
        AtomicInteger calls = new AtomicInteger();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/listings"), new MockHttpServletResponse(),
                (request, response) -> calls.incrementAndGet());
        assertThat(calls).hasValue(1);
        verifyNoInteractions(service);
    }

    private MockHttpServletResponse request(ChatHttpGuardFilter filter, AtomicInteger calls) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/conversations"), response,
                (request, result) -> calls.incrementAndGet());
        return response;
    }

    private void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "unused", List.of()));
    }
}
