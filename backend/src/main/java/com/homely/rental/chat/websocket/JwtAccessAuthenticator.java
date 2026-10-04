package com.homely.rental.chat.websocket;

import com.homely.rental.auth.service.TokenService;
import com.homely.rental.chat.service.ChatService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class JwtAccessAuthenticator {
    private final JwtDecoder decoder;
    private final ChatService chatService;

    public JwtAccessAuthenticator(JwtDecoder decoder, ChatService chatService) {
        this.decoder = decoder;
        this.chatService = chatService;
    }

    JwtAuthenticationToken authenticate(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)
                || authorization.substring(7).isBlank()) {
            throw unauthorized();
        }
        try {
            JwtAuthenticationToken authentication = new JwtAuthenticationToken(decoder.decode(authorization.substring(7)), java.util.List.of());
            validate(authentication);
            return authentication;
        } catch (JwtException | IllegalArgumentException ex) {
            throw unauthorized();
        }
    }

    void validate(JwtAuthenticationToken authentication) {
        if (authentication == null) {
            throw unauthorized();
        }
        Jwt jwt = authentication.getToken();
        Instant now = Instant.now();
        if (!TokenService.TOKEN_TYPE_ACCESS.equals(jwt.getClaimAsString(TokenService.TOKEN_TYPE_CLAIM))
                || jwt.getSubject() == null || jwt.getSubject().isBlank()
                || jwt.getExpiresAt() == null || !jwt.getExpiresAt().isAfter(now)
                || jwt.getNotBefore() != null && jwt.getNotBefore().isAfter(now)) {
            throw unauthorized();
        }
        chatService.requireActiveUser(jwt.getSubject());
    }

    private ChatProtocolException unauthorized() {
        return new ChatProtocolException(401, "TOKEN_EXPIRED_OR_INVALID", "A valid access token is required");
    }
}
