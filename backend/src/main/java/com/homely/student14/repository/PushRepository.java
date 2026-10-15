package com.homely.student14.repository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PushRepository {
    private final DatabaseRepository databaseRepository;

    public Optional<Long> findActiveInstallationUser(String installationId) {
        return databaseRepository
                .all(
                        """
                        SELECT user_id FROM auth_tokens
                        WHERE purpose = 'REFRESH' AND installation_id = ?
                          AND consumed_at IS NULL AND expires_at > UTC_TIMESTAMP(3)
                        ORDER BY created_at DESC, id DESC LIMIT 1
                        """,
                        installationId)
                .stream()
                .findFirst()
                .map(row -> DatabaseRepository.id(row.get("user_id")));
    }

    public void lockDeviceBindings(long userId, String tokenHash, String installationId) {
        databaseRepository.all(
                """
                SELECT id FROM users
                WHERE id = ? OR fcm_token_hash = ? OR fcm_installation_id = ?
                ORDER BY id FOR UPDATE
                """,
                userId,
                tokenHash,
                installationId);
    }

    public boolean hasDeviceToken(long userId) {
        Map<String, Object> user =
                databaseRepository.one("SELECT fcm_token FROM users WHERE id = ?", userId);
        return user.get("fcm_token") != null;
    }

    public void clearOtherAccountBindings(long userId, String tokenHash, String installationId) {
        databaseRepository.update(
                """
                UPDATE users
                SET fcm_token = NULL, fcm_installation_id = NULL, fcm_device_name = NULL
                WHERE id <> ? AND (fcm_token_hash = ? OR fcm_installation_id = ?)
                """,
                userId,
                tokenHash,
                installationId);
    }

    public void registerDeviceToken(
            long userId, String token, String installationId, String deviceName) {
        databaseRepository.update(
                """
                UPDATE users SET fcm_token = ?, fcm_installation_id = ?, fcm_device_name = ?
                WHERE id = ?
                """,
                token,
                installationId,
                deviceName,
                userId);
    }

    public void clearDeviceToken(long userId, String installationId) {
        databaseRepository.update(
                """
                UPDATE users
                SET fcm_token = NULL, fcm_installation_id = NULL, fcm_device_name = NULL
                WHERE id = ? AND fcm_installation_id = ?
                """,
                userId,
                installationId);
    }

    public void clearInvalidDeviceToken(long userId, String installationId, String tokenHash) {
        databaseRepository.update(
                """
                UPDATE users
                SET fcm_token = NULL, fcm_installation_id = NULL, fcm_device_name = NULL
                WHERE id = ? AND fcm_installation_id = ? AND fcm_token_hash = ?
                """,
                userId,
                installationId,
                tokenHash);
    }

    public void skipOlderPendingNotifications(long userId) {
        databaseRepository.update(
                """
                UPDATE notifications SET push_status = 'SKIPPED', push_next_attempt_at = NULL
                WHERE user_id = ? AND push_status IN ('PENDING', 'PROCESSING')
                  AND created_at < UTC_TIMESTAMP(3)
                """,
                userId);
    }

    public List<Map<String, Object>> findDueNotifications() {
        return databaseRepository.all(
                """
                SELECT id FROM notifications
                WHERE push_status IN ('PENDING', 'PROCESSING')
                  AND (push_next_attempt_at IS NULL OR push_next_attempt_at <= UTC_TIMESTAMP(3))
                ORDER BY id LIMIT 25
                """);
    }

    public Map<String, Object> lockNotification(long notificationId) {
        return databaseRepository.one(
                "SELECT * FROM notifications WHERE id = ? FOR UPDATE", notificationId);
    }

    public Optional<Map<String, Object>> findCurrentDeviceBinding(long userId) {
        List<Map<String, Object>> users =
                databaseRepository.all(
                        """
                        SELECT u.id, u.fcm_token, u.fcm_token_hash, u.fcm_installation_id,
                               u.transaction_push, u.chat_push, u.recommendation_push
                        FROM users u
                        WHERE u.id = ? AND u.status = 'ACTIVE'
                          AND u.fcm_token IS NOT NULL AND u.fcm_installation_id IS NOT NULL
                          AND u.id = (
                              SELECT latest_token.user_id FROM auth_tokens latest_token
                              WHERE latest_token.purpose = 'REFRESH'
                                AND latest_token.installation_id = u.fcm_installation_id
                                AND latest_token.consumed_at IS NULL
                                AND latest_token.expires_at > UTC_TIMESTAMP(3)
                              ORDER BY latest_token.created_at DESC, latest_token.id DESC LIMIT 1
                          )
                        """,
                        userId);
        return users.stream().findFirst();
    }

    public void markProcessing(long notificationId, int attemptNumber) {
        databaseRepository.update(
                """
                UPDATE notifications SET push_status = 'PROCESSING', push_attempts = ?,
                    push_next_attempt_at = DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 180 SECOND)
                WHERE id = ?
                """,
                attemptNumber,
                notificationId);
    }

    public void skipNotification(long notificationId, String status) {
        databaseRepository.update(
                """
                UPDATE notifications SET push_status = ?, push_next_attempt_at = NULL
                WHERE id = ?
                """,
                status,
                notificationId);
    }

    public void markSent(long notificationId, int attemptNumber) {
        databaseRepository.update(
                """
                UPDATE notifications SET push_status = 'SENT', pushed_at = UTC_TIMESTAMP(3),
                    push_next_attempt_at = NULL
                WHERE id = ? AND push_status = 'PROCESSING' AND push_attempts = ?
                """,
                notificationId,
                attemptNumber);
    }

    public void finishAttempt(
            long notificationId, int attemptNumber, String status, long retrySeconds) {
        databaseRepository.update(
                """
                UPDATE notifications SET push_status = ?, push_next_attempt_at =
                    IF(? = 'PENDING', DATE_ADD(UTC_TIMESTAMP(3), INTERVAL ? SECOND), NULL)
                WHERE id = ? AND push_status = 'PROCESSING' AND push_attempts = ?
                """,
                status,
                status,
                retrySeconds,
                notificationId,
                attemptNumber);
    }
}
