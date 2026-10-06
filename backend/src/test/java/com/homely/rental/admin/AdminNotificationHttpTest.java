package com.homely.rental.admin;

import com.homely.rental.admin.controller.AdminController;
import com.homely.rental.admin.service.AdminService;
import com.homely.rental.auth.security.*;
import com.homely.rental.chat.service.ChatService;
import com.homely.rental.common.dto.PageResponse;
import com.homely.rental.common.exception.GlobalException;
import com.homely.rental.common.idempotency.IdempotencyKeyRepository;
import com.homely.rental.notification.controller.NotificationController;
import com.homely.rental.notification.dto.NotificationDTO;
import com.homely.rental.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AdminController.class, NotificationController.class})
@Import({SecurityConfig.class, GlobalException.class})
class AdminNotificationHttpTest {
    @Autowired MockMvc mvc;
    @MockBean AdminService admin;
    @MockBean NotificationService inbox;
    @MockBean AccountAccessService accounts;
    @MockBean ChatService chat;
    @MockBean IdempotencyKeyRepository keys;
    @MockBean JwtDecoder decoder;

    @BeforeEach void tokens() {
        when(decoder.decode(anyString())).thenAnswer(call -> {
            String token = call.getArgument(0);
            return Jwt.withTokenValue(token).header("alg", "HS256").subject(token + "@example.test")
                    .claim("token_type", "access").claim("user_id", 1L)
                    .claim("roles", List.of(token.equals("admin") ? "ROLE_ADMIN" : "ROLE_TENANT"))
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
        });
        var account = new com.homely.rental.auth.entity.User(); account.setId(1L);
        when(accounts.requireActive(anyString())).thenReturn(account);
        when(accounts.requireAdmin(anyString())).thenReturn(account);
    }

    @Test void roleAndLiveAccountChecksRunBeforeReplayOrMutation() throws Exception {
        mvc.perform(post("/api/v1/admin/listings/1/approve").header("Authorization", "Bearer tenant"))
                .andExpect(status().isForbidden());
        when(accounts.requireAdmin("admin@example.test")).thenThrow(new AccountAccessException(403, "FORBIDDEN", "Admin role is required"));
        mvc.perform(post("/api/v1/admin/listings/1/approve").header("Authorization", "Bearer admin").header("Idempotency-Key", "old"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.data.code").value("FORBIDDEN"));
        verifyNoInteractions(keys, admin);
    }

    @Test void suspendedJwtIsDeniedOnOtherProtectedApis() throws Exception {
        when(accounts.requireActive("tenant@example.test")).thenThrow(new AccountAccessException(403, "ACCOUNT_INACTIVE", "Account inactive"));
        mvc.perform(get("/api/v1/notifications").header("Authorization", "Bearer tenant"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.data.code").value("ACCOUNT_INACTIVE"));
        mvc.perform(get("/api/v1/bookings").header("Authorization", "Bearer tenant"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(inbox, keys);
    }

    @Test void adminMutationsRequireIdempotencyAndValidatedReason() throws Exception {
        mvc.perform(post("/api/v1/admin/listings/1/approve").header("Authorization", "Bearer admin"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.data.code").value("IDEMPOTENCY_KEY_REQUIRED"));
        mvc.perform(post("/api/v1/admin/listings/1/reject").header("Authorization", "Bearer admin")
                        .header("Idempotency-Key", "reject").param("reason", " "))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.data.code").value("VALIDATION_FAILED"));
        verifyNoInteractions(admin);
    }

    @Test void suspendRouteIsAvailableForAdminWithReason() throws Exception {
        mvc.perform(post("/api/v1/admin/listings/1/suspend").header("Authorization", "Bearer admin")
                        .header("Idempotency-Key", "suspend").param("reason", "Policy violation"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.statusCode").value(200)).andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
        verify(admin).suspendListing(1L, "Policy violation");
    }

    @Test void notificationPaginationIsBoundedInsideEnvelope() throws Exception {
        when(inbox.getMyNotifications(any())).thenReturn(PageResponse.<NotificationDTO>builder()
                .data(List.of()).totalElements(0).totalPages(0).currentPage(0).pageSize(20).build());
        mvc.perform(get("/api/v1/notifications").header("Authorization", "Bearer tenant"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.data").isArray()).andExpect(jsonPath("$.data.page_size").value(20));
        mvc.perform(get("/api/v1/notifications").header("Authorization", "Bearer tenant").param("size", "51"))
                .andExpect(status().isBadRequest());
    }

    @Test void canonicalReadAllAndLegacyAliasRequireIdempotency() throws Exception {
        mvc.perform(post("/api/v1/notifications/read-all").header("Authorization", "Bearer tenant"))
                .andExpect(status().isConflict());
        when(inbox.markAllAsRead()).thenReturn(2);
        for (String route : List.of("read-all", "mark-all-read")) {
            mvc.perform(post("/api/v1/notifications/" + route).header("Authorization", "Bearer tenant")
                            .header("Idempotency-Key", route)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.updated").value(2)).andExpect(jsonPath("$.data.data").doesNotExist());
        }
        verify(inbox, times(2)).markAllAsRead();
    }
}
