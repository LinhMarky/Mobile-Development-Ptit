package com.homely.rental.chat.websocket;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class ChatStompInterceptor implements ChannelInterceptor {
    static final int MAX_MESSAGE_BYTES = 32 * 1024;
    private static final Set<String> SUBSCRIPTIONS = Set.of(
            "/user/queue/chat.messages", "/user/queue/chat.acks", "/user/queue/chat.errors");
    private static final Set<String> IDENTITY_HEADERS = Set.of("user", "simpuser", "login", "passcode", "authorization");
    private final JwtAccessAuthenticator authenticator;
    private final ChatMessageRateLimiter rateLimiter;

    public ChatStompInterceptor(JwtAccessAuthenticator authenticator, ChatMessageRateLimiter rateLimiter) {
        this.authenticator = authenticator;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        // Use the original accessor so Spring retains its CONNECT user-change callback.
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            accessor = StompHeaderAccessor.wrap(message);
        }
        if (accessor.getMessageType() == SimpMessageType.HEARTBEAT) {
            return message;
        }
        StompCommand command = accessor.getCommand();
        // DISCONNECT is also synthesized by Spring when a transport closes; always allow cleanup.
        if (command == StompCommand.DISCONNECT) {
            return message;
        }
        if (command != StompCommand.CONNECT && command != StompCommand.STOMP
                && command != StompCommand.SEND && command != StompCommand.SUBSCRIBE
                && command != StompCommand.UNSUBSCRIBE) {
            throw forbidden();
        }
        for (String header : accessor.toNativeHeaderMap().keySet()) {
            if (IDENTITY_HEADERS.contains(header.toLowerCase(Locale.ROOT))) {
                throw forbidden();
            }
        }
        Map<String, Object> attributes = accessor.getSessionAttributes();
        Object original = attributes == null ? null : attributes.get(JwtHandshakeInterceptor.AUTHENTICATION_ATTRIBUTE);
        if (!(original instanceof JwtAuthenticationToken authentication)) {
            throw new ChatProtocolException(401, "TOKEN_EXPIRED_OR_INVALID", "A valid access token is required");
        }
        authenticator.validate(authentication);
        if (accessor.getUser() != null && (!(accessor.getUser() instanceof JwtAuthenticationToken current)
                || !authentication.getToken().getTokenValue().equals(current.getToken().getTokenValue()))) {
            throw forbidden();
        }
        accessor.setUser(authentication);
        if (command == StompCommand.SUBSCRIBE && !SUBSCRIPTIONS.contains(accessor.getDestination())) {
            throw forbidden();
        }
        if (command == StompCommand.SEND) {
            if (!"/app/chat.send".equals(accessor.getDestination())) {
                throw forbidden();
            }
            int length = message.getPayload() instanceof byte[] bytes ? bytes.length
                    : message.getPayload().toString().getBytes(StandardCharsets.UTF_8).length;
            if (length > MAX_MESSAGE_BYTES) {
                throw new ChatProtocolException(413, "MESSAGE_TOO_LARGE", "The message exceeds the supported size");
            }
            rateLimiter.acquire(authentication.getName());
        }
        return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
    }

    private ChatProtocolException forbidden() {
        return new ChatProtocolException(403, "FORBIDDEN", "This STOMP operation is not permitted");
    }
}
