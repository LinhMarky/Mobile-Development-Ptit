package com.homely.rental.chat.websocket;

import com.homely.rental.chat.service.ChatService;
import com.homely.rental.auth.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatChannelSecurityTest {
    private final ChatService service = mock(ChatService.class);
    private final JwtAccessAuthenticator auth = new JwtAccessAuthenticator(mock(JwtDecoder.class), service);
    private final ChatStompInterceptor inbound = new ChatStompInterceptor(auth, new ChatMessageRateLimiter(2));

    @BeforeEach void activeAccount() {
        User user = new User(); user.setId(1L);
        when(service.requireActiveUser("member@example.test")).thenReturn(user);
    }

    @Test void reusedEmailCannotKeepAnOldSocketAuthenticated() {
        User replacement = new User(); replacement.setId(2L);
        when(service.requireActiveUser("member@example.test")).thenReturn(replacement);
        assertThatThrownBy(() -> inbound.preSend(frame(StompCommand.SEND, "/app/chat.send", "access", false), null))
                .isInstanceOfSatisfying(ChatProtocolException.class, error -> assertThat(error.status()).isEqualTo(401));
    }

    @Test void deniesRawQueuesTopicsForgedIdentityAndUnexpectedCommands() {
        for (String destination : List.of("/topic/all", "/queue/chat.messages", "/user/another/queue/chat.messages")) {
            assertThatThrownBy(() -> inbound.preSend(frame(StompCommand.SUBSCRIBE, destination, "access", false), null))
                    .isInstanceOf(ChatProtocolException.class);
        }
        assertThatThrownBy(() -> inbound.preSend(frame(StompCommand.SEND, "/queue/chat.messages", "access", false), null))
                .isInstanceOf(ChatProtocolException.class);
        assertThatThrownBy(() -> inbound.preSend(frame(StompCommand.SUBSCRIBE, "/user/queue/chat.messages", "access", true), null))
                .isInstanceOf(ChatProtocolException.class);
        assertThatThrownBy(() -> inbound.preSend(frame(StompCommand.MESSAGE, "/user/queue/chat.messages", "access", false), null))
                .isInstanceOf(ChatProtocolException.class);
    }

    @Test void onlyAccessTokensCanConnectAndRateLimitIsEnforced() {
        assertThatThrownBy(() -> inbound.preSend(frame(StompCommand.CONNECT, null, "refresh", false), null))
                .isInstanceOf(ChatProtocolException.class);
        assertThat(inbound.preSend(frame(StompCommand.CONNECT, null, "access", false), null)).isNotNull();
        inbound.preSend(frame(StompCommand.SEND, "/app/chat.send", "access", false), null);
        inbound.preSend(frame(StompCommand.SEND, "/app/chat.send", "access", false), null);
        assertThatThrownBy(() -> inbound.preSend(frame(StompCommand.SEND, "/app/chat.send", "access", false), null))
                .isInstanceOfSatisfying(ChatProtocolException.class, e -> assertThat(e.status()).isEqualTo(429));
    }

    @Test void idleExpiredSessionCannotReceiveMessages() throws Exception {
        ChatWebSocketSessions sessions = new ChatWebSocketSessions(auth);
        WebSocketSession socket = mock(WebSocketSession.class);
        when(socket.getId()).thenReturn("expired");
        when(socket.getAttributes()).thenReturn(Map.of(JwtHandshakeInterceptor.AUTHENTICATION_ATTRIBUTE,
                identity("access", Instant.now().minusSeconds(1))));
        sessions.decorate(new TextWebSocketHandler()).afterConnectionEstablished(socket);
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headers.setSessionId("expired");
        assertThat(sessions.preSend(MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders()), null)).isNull();
        verify(socket).close(CloseStatus.POLICY_VIOLATION);
    }

    private Message<byte[]> frame(StompCommand command, String destination, String type, boolean forged) {
        StompHeaderAccessor headers = StompHeaderAccessor.create(command);
        headers.setSessionId("test-session");
        headers.setSessionAttributes(new HashMap<>(Map.of(JwtHandshakeInterceptor.AUTHENTICATION_ATTRIBUTE,
                identity(type, Instant.now().plusSeconds(600)))));
        if (destination != null) headers.setDestination(destination);
        if (forged) headers.setNativeHeader("simpUser", "admin");
        headers.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
    }

    private JwtAuthenticationToken identity(String type, Instant expiry) {
        Jwt jwt = Jwt.withTokenValue("session-token").header("alg", "HS256").subject("member@example.test")
                .claim("token_type", type).claim("user_id", 1L).expiresAt(expiry).build();
        return new JwtAuthenticationToken(jwt, List.of());
    }
}
