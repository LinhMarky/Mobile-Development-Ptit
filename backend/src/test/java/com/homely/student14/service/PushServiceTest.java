package com.homely.student14.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.homely.student14.dto.request.push.RegisterDeviceTokenDTO;
import com.homely.student14.dto.request.push.RemoveDeviceTokenDTO;
import com.homely.student14.repository.PushRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class PushServiceTest {
    private static final long USER_ID = 11L;
    private static final String INSTALLATION_ID = "bf7611b2-676a-4cf7-b649-90a2a4545192";
    private static final String OTHER_INSTALLATION_ID = "9e50523b-eec5-4b80-9d39-c195ef2a9b08";
    private static final String DEVICE_TOKEN = "fcm:test-current-device";

    @Mock private PushRepository pushRepository;
    @Mock private AuthService authService;

    private PushService pushService;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        pushService = new PushService(pushRepository, authService);
        jwt =
                Jwt.withTokenValue("test-access-token")
                        .header("alg", "HS256")
                        .subject(Long.toString(USER_ID))
                        .claim("installation_id", INSTALLATION_ID)
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(900))
                        .build();
        when(authService.user(jwt)).thenReturn(Map.of("id", USER_ID));
    }

    @Test
    void registrationLocksAndChecksInstallationOwnerBeforeMovingDeviceBinding() {
        prepareOwnInstallation();

        Map<String, Object> response = pushService.registerDevice(jwt, registration());

        ArgumentCaptor<String> tokenHash = ArgumentCaptor.forClass(String.class);
        InOrder updates = inOrder(pushRepository);
        updates.verify(pushRepository)
                .lockDeviceBindings(eq(USER_ID), tokenHash.capture(), eq(INSTALLATION_ID));
        updates.verify(pushRepository).findActiveInstallationUser(INSTALLATION_ID);
        updates.verify(pushRepository).hasDeviceToken(USER_ID);
        updates.verify(pushRepository)
                .clearOtherAccountBindings(USER_ID, tokenHash.getValue(), INSTALLATION_ID);
        updates.verify(pushRepository)
                .registerDeviceToken(USER_ID, DEVICE_TOKEN, INSTALLATION_ID, "Pixel 8");
        assertTrue(tokenHash.getValue().matches("[0-9a-f]{64}"));
        assertEquals(true, response.get("registered"));
        assertEquals(INSTALLATION_ID, response.get("installation_id"));
    }

    @Test
    void registrationRejectsInstallationDifferentFromJwtBeforeReadingOrUpdatingBindings() {
        RegisterDeviceTokenDTO request = registration();
        request.setInstallationId(OTHER_INSTALLATION_ID);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> pushService.registerDevice(jwt, request));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verifyNoInteractions(pushRepository);
    }

    @Test
    void staleAccountCannotReplaceDeviceTokenOwnedByLatestSession() {
        when(pushRepository.findActiveInstallationUser(INSTALLATION_ID))
                .thenReturn(Optional.of(22L));

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> pushService.registerDevice(jwt, registration()));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(pushRepository).lockDeviceBindings(eq(USER_ID), anyString(), eq(INSTALLATION_ID));
        verify(pushRepository, never()).hasDeviceToken(anyLong());
        verify(pushRepository, never())
                .clearOtherAccountBindings(anyLong(), anyString(), anyString());
        verify(pushRepository, never())
                .registerDeviceToken(anyLong(), anyString(), anyString(), anyString());
        verify(pushRepository, never()).skipOlderPendingNotifications(anyLong());
    }

    @Test
    void firstRegistrationWithPushEnabledSkipsOlderNotificationsAfterBindingDevice() {
        prepareOwnInstallation();
        ReflectionTestUtils.setField(pushService, "pushEnabled", true);

        Map<String, Object> response = pushService.registerDevice(jwt, registration());

        InOrder updates = inOrder(pushRepository);
        updates.verify(pushRepository)
                .registerDeviceToken(USER_ID, DEVICE_TOKEN, INSTALLATION_ID, "Pixel 8");
        updates.verify(pushRepository).skipOlderPendingNotifications(USER_ID);
        assertEquals(true, response.get("push_enabled"));
    }

    @Test
    void updatingAnExistingDeviceKeepsNewPendingNotifications() {
        prepareOwnInstallation();
        when(pushRepository.hasDeviceToken(USER_ID)).thenReturn(true);
        ReflectionTestUtils.setField(pushService, "pushEnabled", true);

        pushService.registerDevice(jwt, registration());

        verify(pushRepository)
                .registerDeviceToken(USER_ID, DEVICE_TOKEN, INSTALLATION_ID, "Pixel 8");
        verify(pushRepository, never()).skipOlderPendingNotifications(anyLong());
    }

    @Test
    void disabledPushRegistersDeviceWithoutAccessingDeliveryStateColumns() {
        prepareOwnInstallation();

        Map<String, Object> response = pushService.registerDevice(jwt, registration());

        assertEquals(false, response.get("push_enabled"));
        verify(pushRepository)
                .registerDeviceToken(USER_ID, DEVICE_TOKEN, INSTALLATION_ID, "Pixel 8");
        verify(pushRepository, never()).skipOlderPendingNotifications(anyLong());
    }

    @Test
    void removalClearsOnlyCurrentUserAndInstallation() {
        RemoveDeviceTokenDTO request = new RemoveDeviceTokenDTO();
        request.setInstallationId(INSTALLATION_ID);

        pushService.removeDevice(jwt, request);

        verify(pushRepository).clearDeviceToken(USER_ID, INSTALLATION_ID);
        verify(pushRepository, never())
                .clearOtherAccountBindings(anyLong(), anyString(), anyString());
    }

    @Test
    void removalRejectsAnotherInstallationWithoutClearingAnyDevice() {
        RemoveDeviceTokenDTO request = new RemoveDeviceTokenDTO();
        request.setInstallationId(OTHER_INSTALLATION_ID);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> pushService.removeDevice(jwt, request));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verifyNoInteractions(pushRepository);
    }

    private void prepareOwnInstallation() {
        when(pushRepository.findActiveInstallationUser(INSTALLATION_ID))
                .thenReturn(Optional.of(USER_ID));
    }

    private RegisterDeviceTokenDTO registration() {
        RegisterDeviceTokenDTO request = new RegisterDeviceTokenDTO();
        request.setToken(DEVICE_TOKEN);
        request.setInstallationId(INSTALLATION_ID);
        request.setDeviceName("Pixel 8");
        return request;
    }
}
