package com.homely.rental.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homely.rental.common.dto.ProblemDTO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/** Recheck account state and live admin membership before mutations or cached responses. */
public final class AccountStateFilter extends OncePerRequestFilter {
    private final AccountAccessService accounts;
    private final ObjectMapper mapper;
    private final String adminPath;

    public AccountStateFilter(AccountAccessService accounts, ObjectMapper mapper, String apiPrefix) {
        this.accounts = accounts;
        this.mapper = mapper;
        this.adminPath = "/" + apiPrefix.replaceAll("^/+|/+$", "") + "/admin";
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken && authentication.isAuthenticated()) {
            String path = request.getRequestURI().substring(request.getContextPath().length());
            try {
                if (path.equals(adminPath) || path.startsWith(adminPath + "/")) {
                    accounts.requireAdmin(authentication.getName());
                } else {
                    accounts.requireActive(authentication.getName());
                }
            } catch (AccountAccessException ex) {
                response.setStatus(ex.getStatus());
                response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                mapper.writeValue(response.getOutputStream(), ProblemDTO.builder()
                        .type("urn:problem:" + ex.getCode().toLowerCase(Locale.ROOT).replace('_', '-'))
                        .title(HttpStatus.valueOf(ex.getStatus()).getReasonPhrase()).status(ex.getStatus())
                        .code(ex.getCode()).detail(ex.getMessage()).instance(request.getRequestURI())
                        .traceId(UUID.randomUUID().toString()).timestamp(Instant.now()).build());
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
