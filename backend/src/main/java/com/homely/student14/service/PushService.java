package com.homely.student14.service;

import com.homely.student14.dto.request.push.RegisterDeviceTokenDTO;
import com.homely.student14.dto.request.push.RemoveDeviceTokenDTO;
import com.homely.student14.repository.DatabaseRepository;
import com.homely.student14.repository.PushRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PushService {
    private final PushRepository pushRepository;
    private final AuthService authService;

    @Value("${student14.push-enabled:false}")
    private boolean pushEnabled;

    @Transactional
    public Map<String, Object> registerDevice(Jwt jwt, RegisterDeviceTokenDTO request) {
        long userId = DatabaseRepository.id(authService.user(jwt).get("id"));
        String installationId = checkInstallation(jwt, request.getInstallationId());
        String token = request.getToken().trim();
        DatabaseRepository.require(
                !token.isEmpty() && !token.matches(".*\\s.*"), 400, "Token thiết bị không hợp lệ.");
        String deviceName = Objects.toString(request.getDeviceName(), "").trim();
        String tokenHash = tokenHash(token);

        pushRepository.lockDeviceBindings(userId, tokenHash, installationId);
        // Đọc chủ phiên mới nhất sau khi lấy khóa để JWT cũ không chiếm lại token thiết bị.
        if (pushRepository.findActiveInstallationUser(installationId).orElse(0L)
                != userId) {
            throw DatabaseRepository.fail(403, "Phiên đăng nhập trên thiết bị đã thay đổi.");
        }
        boolean previouslyRegistered = pushRepository.hasDeviceToken(userId);
        pushRepository.clearOtherAccountBindings(userId, tokenHash, installationId);
        pushRepository.registerDeviceToken(userId, token, installationId, deviceName);
        if (pushEnabled && !previouslyRegistered) {
            pushRepository.skipOlderPendingNotifications(userId);
        }
        return Map.of(
                "registered", true, "installation_id", installationId, "push_enabled", pushEnabled);
    }

    @Transactional
    public void removeDevice(Jwt jwt, RemoveDeviceTokenDTO request) {
        long userId = DatabaseRepository.id(authService.user(jwt).get("id"));
        String installationId = checkInstallation(jwt, request.getInstallationId());
        pushRepository.clearDeviceToken(userId, installationId);
    }

    private String checkInstallation(Jwt jwt, String installationId) {
        String normalizedId = Objects.toString(installationId, "").trim();
        String sessionInstallation = jwt.getClaimAsString("installation_id");
        DatabaseRepository.require(
                !normalizedId.isEmpty() && normalizedId.equals(sessionInstallation),
                403,
                "Thiết bị phải trùng với phiên đăng nhập hiện tại.");
        return normalizedId;
    }

    private String tokenHash(String token) {
        try {
            byte[] digest =
                    MessageDigest.getInstance("SHA-256")
                            .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 không có sẵn.");
        }
    }
}
