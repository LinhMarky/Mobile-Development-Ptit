package com.homely.rental.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homely.rental.auth.entity.User;
import com.homely.rental.common.response.ApiProblems;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Recheck account state and live admin membership before mutations or cached responses. */
public final class AccountStateFilter extends OncePerRequestFilter {
    private final AccountAccessService accounts;
    private final ObjectMapper mapper;
    private final String adminPath;
    private final String authPath;

    public AccountStateFilter(AccountAccessService accounts, ObjectMapper mapper, String apiPrefix) {
        this.accounts = accounts;
        this.mapper = mapper;
        this.adminPath = "/" + apiPrefix.replaceAll("^/+|/+$", "") + "/admin";
        this.authPath = "/" + apiPrefix.replaceAll("^/+|/+$", "") + "/auth";
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwt && authentication.isAuthenticated()) {
            String path = request.getRequestURI().substring(request.getContextPath().length());
            try {
                User account;
                if (path.equals(adminPath) || path.startsWith(adminPath + "/")
                        || (path.startsWith("/actuator/") && !path.equals("/actuator/health"))) {
                    account = accounts.requireAdmin(authentication.getName());
                } else if (path.equals(authPath + "/delete-account") || path.equals(authPath + "/logout")) {
                    account = accounts.requireActiveIgnoringDeletion(authentication.getName());
                } else {
                    account = accounts.requireActive(authentication.getName());
                }
                if (!AccessTokenIdentity.matches(jwt.getToken(), account.getId())) {
                    throw new AccountAccessException(401, "INVALID_ACCESS_TOKEN", "A valid access token is required");
                }
            } catch (AccountAccessException ex) {
                ApiProblems.write(mapper, request, response, ex.getStatus(), ex.getCode(), ex.getMessage());
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
