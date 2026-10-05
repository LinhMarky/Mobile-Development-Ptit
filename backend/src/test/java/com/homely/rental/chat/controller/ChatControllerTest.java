package com.homely.rental.chat.controller;

import com.homely.rental.auth.entity.User;
import com.homely.rental.auth.security.SecurityConfig;
import com.homely.rental.chat.dto.ConversationDTO;
import com.homely.rental.chat.dto.MessagePageDTO;
import com.homely.rental.chat.service.ChatException;
import com.homely.rental.chat.service.ChatService;
import com.homely.rental.common.exception.GlobalException;
import com.homely.rental.common.idempotency.IdempotencyKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
@Import({SecurityConfig.class, GlobalException.class})
@TestPropertySource(properties = {
        "apiPrefix=api/v1",
        "homely.jwt.base64-secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
})
class ChatControllerTest {
    @Autowired private MockMvc mvc;
    @MockBean private ChatService service;
    @MockBean private IdempotencyKeyRepository idempotency;
    @MockBean private JwtDecoder decoder;
    @MockBean private com.homely.rental.auth.security.AccountAccessService accounts;

    @BeforeEach void setUp() {
        when(decoder.decode("access")).thenReturn(token("access"));
        when(decoder.decode("refresh")).thenReturn(token("refresh"));
        User user = new User();
        user.setId(5L);
        user.setEmail("tenant@example.test");
        user.setEmailVerified(true);
        when(service.requireActiveUser("tenant@example.test")).thenReturn(user);
    }

    @Test void unauthenticatedCannotListOrHandshake() throws Exception {
        mvc.perform(get("/api/v1/conversations")).andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        mvc.perform(get("/ws")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service, idempotency);
    }

    @Test void refreshTokenCannotReadChatOrAuthenticateWebSocket() throws Exception {
        mvc.perform(get("/api/v1/conversations").header("Authorization", "Bearer refresh"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"));
        mvc.perform(get("/ws").header("Authorization", "Bearer refresh")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service, idempotency);
    }

    @Test void createUsesAuthenticatedIdentityAndReturnsDirectDto() throws Exception {
        when(service.createConversation(7L, "tenant@example.test"))
                .thenReturn(ConversationDTO.builder().id(10L).roomId(7L).tenantId(5L).build());
        mvc.perform(post("/api/v1/conversations").header("Authorization", "Bearer access")
                        .header("Idempotency-Key", "conversation-create").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"room_id\":7,\"tenant_id\":999}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.tenant_id").value(5)).andExpect(jsonPath("$.data").doesNotExist());
        verify(service).createConversation(7L, "tenant@example.test");
        verify(idempotency, times(2)).saveAndFlush(any());
    }

    @Test void postRequiresIdempotencyKey() throws Exception {
        mvc.perform(post("/api/v1/conversations").header("Authorization", "Bearer access")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"room_id\":7}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REQUIRED"));
        verify(service, never()).createConversation(anyLong(), anyString());
    }

    @Test void nonMemberIsRejectedBeforeAnyCachedResponse() throws Exception {
        when(service.requireMember(10L, "tenant@example.test"))
                .thenThrow(new ChatException(404, "RESOURCE_NOT_FOUND", "Conversation not found"));
        mvc.perform(post("/api/v1/conversations/10/read-marker").header("Authorization", "Bearer access")
                        .header("Idempotency-Key", "read-key").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"last_read_sequence\":3}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        verifyNoInteractions(idempotency);
        verify(service, never()).markRead(anyLong(), anyString(), anyLong());
    }

    @Test void inactiveUserCannotReplayAStoredConversation() throws Exception {
        when(service.requireActiveUser("tenant@example.test"))
                .thenThrow(new ChatException(403, "ACCOUNT_INACTIVE", "Account is inactive"));
        mvc.perform(post("/api/v1/conversations").header("Authorization", "Bearer access")
                        .header("Idempotency-Key", "old-key").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"room_id\":7}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCOUNT_INACTIVE"));
        verifyNoInteractions(idempotency);
    }

    @Test void unverifiedUserCannotCreateButMayReadExistingConversations() throws Exception {
        User unverified = new User();
        when(service.requireActiveUser("tenant@example.test")).thenReturn(unverified);
        when(service.getConversation(10L, "tenant@example.test")).thenReturn(ConversationDTO.builder().id(10L).build());
        mvc.perform(post("/api/v1/conversations").header("Authorization", "Bearer access")
                        .header("Idempotency-Key", "new-key").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"room_id\":7}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
        mvc.perform(get("/api/v1/conversations/10").header("Authorization", "Bearer access"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(10));
    }

    @Test void historyMapsExplicitCursorsAndUsesAuthenticatedEmail() throws Exception {
        when(service.getMessages(10L, "tenant@example.test", 3, null, 12L))
                .thenReturn(MessagePageDTO.builder().data(List.of()).hasMore(false).build());
        mvc.perform(get("/api/v1/conversations/10/messages").header("Authorization", "Bearer access")
                        .param("limit", "3").param("after_sequence", "12"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.has_more").value(false));
        verify(service).getMessages(10L, "tenant@example.test", 3, null, 12L);
    }

    @Test void rejectsInvalidPageSizeAndReadMarkerBody() throws Exception {
        mvc.perform(get("/api/v1/conversations/10/messages").header("Authorization", "Bearer access")
                        .param("limit", "51"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(post("/api/v1/conversations/10/read-marker").header("Authorization", "Bearer access")
                        .header("Idempotency-Key", "bad-marker").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"last_read_sequence\":-1}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        verify(service, never()).getMessages(anyLong(), anyString(), anyInt(), any(), any());
        verify(service, never()).markRead(anyLong(), anyString(), anyLong());
    }

    private Jwt token(String type) {
        return Jwt.withTokenValue(type).header("alg", "HS256").subject("tenant@example.test")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600))
                .claim("token_type", type).claim("roles", List.of("ROLE_TENANT"))
                .claim("user", Map.of("id", 5L)).build();
    }
}
