package com.homely.student14.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Base64;
import java.util.Map;

import javax.crypto.spec.SecretKeySpec;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtEncoder jwtEncoder(@Value("${student14.jwt-secret}") String secret) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key(secret)));
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${student14.jwt-secret}") String secret) {
        return NimbusJwtDecoder.withSecretKey(key(secret)).macAlgorithm(MacAlgorithm.HS256).build();
    }

    private SecretKeySpec key(String value) {
        byte[] bytes = Base64.getDecoder().decode(value);
        if (bytes.length < 32)
            throw new IllegalArgumentException("Student JWT secret requires 32 bytes");
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    SecurityFilterChain studentChain(HttpSecurity http, ObjectMapper json) throws Exception {
        var entry =
                (org.springframework.security.web.AuthenticationEntryPoint)
                        (req, res, ex) -> {
                            res.setStatus(401);
                            res.setContentType("application/json;charset=UTF-8");
                            json.writeValue(
                                    res.getOutputStream(),
                                    Map.of(
                                            "statusCode",
                                            401,
                                            "message",
                                            "Bạn cần đăng nhập.",
                                            "data",
                                            Map.of(
                                                    "code",
                                                    "UNAUTHORIZED",
                                                    "detail",
                                                    "Bạn cần đăng nhập.")));
                        };
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        a ->
                                a.requestMatchers(
                                                "/actuator/health",
                                                "/v3/api-docs/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html")
                                        .permitAll()
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/api/v1/auth/login",
                                                "/api/v1/auth/register",
                                                "/api/v1/auth/refresh",
                                                "/api/v1/auth/verification-code",
                                                "/api/v1/auth/recovery-code",
                                                "/api/v1/auth/verify-email",
                                                "/api/v1/auth/reset-password")
                                        .permitAll()
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/api/v1/listings",
                                                "/api/v1/listings/*",
                                                "/api/v1/amenities",
                                                "/api/v1/rooms/*/reviews",
                                                "/api/v1/media/*/content")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(entry))
                .oauth2ResourceServer(o -> o.jwt(j -> {}).authenticationEntryPoint(entry))
                .build();
    }
}
