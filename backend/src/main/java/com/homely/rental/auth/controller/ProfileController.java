package com.homely.rental.auth.controller;

import com.homely.rental.auth.dto.request.*;
import com.homely.rental.auth.dto.response.UserResponse;
import com.homely.rental.auth.entity.NotificationPreference;
import com.homely.rental.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import com.homely.rental.common.exception.IdInvalidException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Profile management controller (AUTH07–AUTH11).
 * Handles user profile, device tokens, and notification preferences.
 */
@RestController
@RequestMapping(path = "${apiPrefix}/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;
    @DeleteMapping("/device-token/{installationId}")
    public ResponseEntity<Void> unregisterDevice(@PathVariable String installationId) throws IdInvalidException {
        userService.unregisterDevice(installationId);
        return ResponseEntity.noContent().build();
    }

    // AUTH07: Get profile
    @GetMapping
    @Operation(summary = "Get full user profile")
    public ResponseEntity<UserResponse> getProfile() throws IdInvalidException {
        return ResponseEntity.ok(userService.fetchCurrentUserProfile());
    }

    // AUTH08: Update profile
    @PatchMapping
    @Operation(summary = "Update user profile")
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody ProfileUpdateRequest dto) throws IdInvalidException {
        return ResponseEntity.ok(userService.updateProfile(dto));
    }

    // AUTH10: Register/update device token (FCM)
    @PutMapping("/device-token")
    @Operation(summary = "Register or update device token")
    public ResponseEntity<Map<String, String>> registerDeviceToken(@Valid @RequestBody DeviceTokenRequest dto) throws IdInvalidException {
        userService.registerDeviceToken(dto);
        return ResponseEntity.ok(Map.of("status", "registered"));
    }

    // AUTH11: Get notification preferences
    @GetMapping("/notification-preferences")
    @Operation(summary = "Get notification preferences")
    public ResponseEntity<NotificationPreference> getNotificationPreferences() throws IdInvalidException {
        return ResponseEntity.ok(userService.getNotificationPreferences());
    }

    // AUTH11: Update notification preferences
    @PatchMapping("/notification-preferences")
    @Operation(summary = "Update notification preferences")
    public ResponseEntity<NotificationPreference> updateNotificationPreferences(
            @Valid @RequestBody NotificationPreferencesRequest dto) throws IdInvalidException {
        return ResponseEntity.ok(userService.updateNotificationPreferences(dto));
    }
}
