package com.homely.rental.chat.websocket;

import com.homely.rental.chat.service.ChatException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Prevent idle sessions from receiving data after expiry or account deactivation. */
@Component
public class ChatWebSocketSessions implements ChannelInterceptor {
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final JwtAccessAuthenticator authenticator;

    public ChatWebSocketSessions(JwtAccessAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    WebSocketHandler decorate(WebSocketHandler handler) {
        return new WebSocketHandlerDecorator(handler) {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                sessions.put(session.getId(), session);
                try {
                    super.afterConnectionEstablished(session);
                } catch (Exception error) {
                    sessions.remove(session.getId());
                    throw error;
                }
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
                sessions.remove(session.getId());
                super.afterConnectionClosed(session, status);
            }
        };
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        if (SimpMessageHeaderAccessor.getMessageType(message.getHeaders()) != SimpMessageType.MESSAGE) {
            return message;
        }
        String sessionId = SimpMessageHeaderAccessor.getSessionId(message.getHeaders());
        WebSocketSession session = sessionId == null ? null : sessions.get(sessionId);
        if (session == null) {
            return null;
        }
        Object identity = session.getAttributes().get(JwtHandshakeInterceptor.AUTHENTICATION_ATTRIBUTE);
        try {
            authenticator.validate(identity instanceof JwtAuthenticationToken jwt ? jwt : null);
            return message;
        } catch (ChatProtocolException | ChatException invalidSession) {
            sessions.remove(sessionId);
            try {
                session.close(CloseStatus.POLICY_VIOLATION);
            } catch (IOException ignored) {
                // The transport may already have closed; the message remains suppressed.
            }
            return null;
        }
    }
}
