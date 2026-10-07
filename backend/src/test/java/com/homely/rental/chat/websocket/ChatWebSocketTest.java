package com.homely.rental.chat.websocket;

import com.homely.rental.auth.entity.User;
import com.homely.rental.auth.security.SecurityConfig;
import com.homely.rental.chat.dto.MessageDTO;
import com.homely.rental.chat.dto.MessageSendFrame;
import com.homely.rental.chat.entity.MessageContentType;
import com.homely.rental.chat.service.ChatMessageCreatedEvent;
import com.homely.rental.chat.service.ChatService;
import com.homely.rental.common.idempotency.IdempotencyKeyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = ChatWebSocketTest.Config.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"apiPrefix=api/v1", "homely.jwt.base64-secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA==",
                "logging.level.root=WARN", "debug=false"})
class ChatWebSocketTest {
    @Configuration
    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class, FlywayAutoConfiguration.class})
    @Import({SecurityConfig.class, StompConfig.class, JwtHandshakeInterceptor.class, JwtAccessAuthenticator.class,
            ChatStompInterceptor.class, ChatMessageRateLimiter.class, ChatWebSocketSessions.class,
            ChatStompErrorHandler.class, ChatStompHandler.class, ChatMessageBroadcaster.class})
    static class Config {
        @Bean TestTransactions transactions() { return new TestTransactions(); }
    }
    static class TestTransactions extends AbstractPlatformTransactionManager {
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) { }
        @Override protected void doCommit(DefaultTransactionStatus status) { }
        @Override protected void doRollback(DefaultTransactionStatus status) { }
    }

    @LocalServerPort int port;
    @MockBean ChatService service;
    @MockBean JwtDecoder decoder;
    @MockBean com.homely.rental.auth.security.AccountAccessService accounts;
    @MockBean IdempotencyKeyRepository keys;
    // The unrelated springfilter auto-configuration requires this even in a transport-only context.
    @MockBean jakarta.persistence.EntityManager entityManager;
    @Autowired ApplicationEventPublisher events;
    @Autowired TestTransactions transactions;
    private WebSocketStompClient client;
    private final List<StompSession> sessions = new ArrayList<>();

    @BeforeEach void setUp() {
        client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new MappingJackson2MessageConverter());
        client.setDefaultHeartbeat(new long[]{0, 0});
        when(decoder.decode(anyString())).thenAnswer(call -> {
            String token = call.getArgument(0);
            return Jwt.withTokenValue(token).header("alg", "HS256").subject(token + "@example.test")
                    .claim("token_type", token.equals("refresh") ? "refresh" : "access")
                    .claim("user_id", 1L)
                    .claim("roles", List.of("ROLE_TENANT"))
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600)).build();
        });
        User active = new User(); active.setId(1L); active.setEmailVerified(true);
        when(service.requireActiveUser(anyString())).thenReturn(active);
        when(accounts.requireActive(anyString())).thenReturn(active);
        when(service.sendMessage(any(), eq("tenant@example.test"))).thenAnswer(call -> {
            MessageSendFrame frame = call.getArgument(0);
            MessageDTO stored = MessageDTO.builder().id(7L).conversationId(frame.getConversationId())
                    .senderId(1L).clientMessageId(frame.getClientMessageId()).content(frame.getContent())
                    .contentType(MessageContentType.TEXT).sequence(1L).createdAt(Instant.parse("2026-09-27T00:00:00Z")).build();
            return new TransactionTemplate(transactions).execute(tx -> {
                events.publishEvent(new ChatMessageCreatedEvent(stored, "tenant@example.test", "host@example.test"));
                return stored;
            });
        });
    }

    @AfterEach void close() {
        for (StompSession session : sessions) if (session.isConnected()) session.disconnect();
        client.stop();
    }

    @Test void actualHandshakeRejectsMissingAndRefreshTokens() {
        assertThatThrownBy(() -> connect(null)).isInstanceOf(ExecutionException.class);
        assertThatThrownBy(() -> connect("refresh")).isInstanceOf(ExecutionException.class);
    }

    @Test void actualStompRoutesCommittedMessageOnlyToItsMembersAndAckToSender() throws Exception {
        StompSession tenant = connect("tenant");
        StompSession host = connect("host");
        StompSession other = connect("other");
        BlockingQueue<Map<String, Object>> ack = subscribe(tenant, "/user/queue/chat.acks");
        BlockingQueue<Map<String, Object>> mine = subscribe(tenant, "/user/queue/chat.messages");
        BlockingQueue<Map<String, Object>> theirs = subscribe(host, "/user/queue/chat.messages");
        BlockingQueue<Map<String, Object>> outside = subscribe(other, "/user/queue/chat.messages");
        // A round trip per receiver also establishes that preceding SUBSCRIBE frames were handled.
        BlockingQueue<Map<String, Object>> hostErrors = subscribe(host, "/user/queue/chat.errors");
        BlockingQueue<Map<String, Object>> otherErrors = subscribe(other, "/user/queue/chat.errors");
        host.send("/app/chat.send", Map.of("content", ""));
        other.send("/app/chat.send", Map.of("content", ""));
        assertThat(hostErrors.poll(5, TimeUnit.SECONDS)).isNotNull();
        assertThat(otherErrors.poll(5, TimeUnit.SECONDS)).isNotNull();
        tenant.send("/app/chat.send", Map.of("conversation_id", 10, "content", "hello",
                "client_message_id", "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        assertThat(ack.poll(5, TimeUnit.SECONDS)).containsEntry("id", 7).containsEntry("sequence", 1)
                .containsEntry("created_at", "2026-09-27T00:00:00Z");
        assertThat(mine.poll(5, TimeUnit.SECONDS)).containsEntry("content", "hello");
        assertThat(theirs.poll(5, TimeUnit.SECONDS)).containsEntry("content", "hello");
        assertThat(outside.poll(300, TimeUnit.MILLISECONDS)).isNull();
        verify(service).sendMessage(any(), eq("tenant@example.test"));
    }

    @Test void validationReturnsPrivateProblemWithoutCallingService() throws Exception {
        StompSession tenant = connect("tenant");
        BlockingQueue<Map<String, Object>> errors = subscribe(tenant, "/user/queue/chat.errors");
        tenant.send("/app/chat.send", Map.of("content", " ", "client_message_id", "not-a-uuid"));
        assertThat(errors.poll(5, TimeUnit.SECONDS)).containsEntry("status", 400).containsKey("field_errors");
        verify(service, never()).sendMessage(any(), anyString());
    }

    private StompSession connect(String token) throws Exception {
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        if (token != null) headers.setBearerAuth(token);
        StompSession session = client.connectAsync("ws://127.0.0.1:" + port + "/ws", headers,
                new StompHeaders(), new StompSessionHandlerAdapter() { }).get(8, TimeUnit.SECONDS);
        sessions.add(session);
        return session;
    }

    @SuppressWarnings("unchecked")
    private BlockingQueue<Map<String, Object>> subscribe(StompSession session, String destination) {
        BlockingQueue<Map<String, Object>> received = new LinkedBlockingQueue<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders headers) { return Map.class; }
            @Override public void handleFrame(StompHeaders headers, Object payload) { received.add((Map<String, Object>) payload); }
        });
        return received;
    }
}
