package com.homely.rental.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homely.rental.chat.security.ChatHttpGuardFilter;
import com.homely.rental.chat.service.ChatService;
import com.homely.rental.common.idempotency.IdempotencyFilter;
import com.homely.rental.common.idempotency.IdempotencyKeyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import javax.crypto.SecretKey;

@Configuration
@org.springframework.boot.context.properties.EnableConfigurationProperties(AuthRateLimitProperties.class)
@org.springframework.context.annotation.Import(AuthRequestLimiter.class)
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {

    @Value("${homely.jwt.base64-secret}")
    private String jwtKey;

    @Value("${apiPrefix:api/v1}")
    private String apiPrefix;

    @Value("${homely.chat.rest-requests-per-minute:120}")
    private int chatRestRequestsPerMinute;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private SecretKey getSecretKey() {
        return JwtSigningKey.decode(jwtKey);
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(getSecretKey())
                .macAlgorithm(SecurityUtils.JWT_ALGORITHM).build();
        return decoder;
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(getSecretKey()));
    }

    /**
     * Custom JWT authentication converter that:
     * 1. Extracts roles from the "roles" claim
     * 2. Rejects refresh tokens (token_type != "access") from being used as bearer tokens
     */
    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return new AccessJwtAuthenticationConverter();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, ObjectMapper mapper,
                                           IdempotencyKeyRepository idempotencyRepository, ChatService chatService,
                                           AccountAccessService accounts, AuthRateLimitProperties authLimits,
                                           AuthRequestLimiter authLimiter) throws Exception {
        String base = "/" + apiPrefix.replaceAll("^/+|/+$", "");
        String[] PUBLIC_URLS = {
                base + "/auth/login",
                base + "/auth/register",
                base + "/auth/refresh",
                base + "/auth/verify-email",
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/actuator/health"
        };

        SecurityProblemHandler problems = new SecurityProblemHandler(mapper);

        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR).permitAll()
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .requestMatchers(HttpMethod.POST, base + "/rooms", base + "/rooms/**",
                                base + "/listings", base + "/listings/**").hasRole("HOST")
                        // Public discovery: anyone can view listings, search, amenities
                        .requestMatchers(HttpMethod.GET, base + "/listings/my").hasRole("HOST")
                        .requestMatchers(HttpMethod.GET, base + "/listings/**").permitAll()
                        .requestMatchers(HttpMethod.GET, base + "/amenities/**").permitAll()
                        .requestMatchers(HttpMethod.GET, base + "/media/room/**").permitAll()
                        .requestMatchers(HttpMethod.GET, base + "/media/*/content").permitAll()
                        // Public interaction: viewing slots, reviews
                        .requestMatchers(HttpMethod.GET, base + "/viewing-slots/**").permitAll()
                        .requestMatchers(HttpMethod.GET, base + "/reviews/room/**").permitAll()
                        // Public Webhooks
                        .requestMatchers(HttpMethod.POST, "/webhooks/**").permitAll()
                        // Admin endpoints
                        .requestMatchers(base + "/admin/**").hasRole("ADMIN")
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors.authenticationEntryPoint(problems).accessDeniedHandler(problems))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(problems).accessDeniedHandler(problems)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .addFilterAfter(new AccountStateFilter(accounts, mapper, base), AuthorizationFilter.class)
                .addFilterAfter(new AuthRateLimitFilter(authLimits, authLimiter, mapper, base), AccountStateFilter.class)
                .addFilterAfter(new ChatHttpGuardFilter(chatService, mapper, base, chatRestRequestsPerMinute),
                        AuthRateLimitFilter.class)
                .addFilterAfter(new IdempotencyFilter(idempotencyRepository, mapper, base), ChatHttpGuardFilter.class);

        return http.build();
    }
}
