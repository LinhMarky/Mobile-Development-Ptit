package com.homely.student14.repository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AuthRepository {
    private final DatabaseRepository databaseRepository;

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

    public Map<String, Object> findUserById(long userId) {
        return databaseRepository.one("SELECT * FROM users WHERE id = ?", userId);
    }

    public Optional<Map<String, Object>> findUserByEmail(String email) {
        List<Map<String, Object>> users =
                databaseRepository.all("SELECT * FROM users WHERE email = ?", email);
        return users.stream().findFirst();
    }

    public Map<String, Object> findActiveUserByEmailForUpdate(String email) {
        return databaseRepository.one(
                "SELECT * FROM users WHERE email = ? AND status = 'ACTIVE' FOR UPDATE", email);
    }

    public long createUser(String email, String password, String fullName, String phone) {
        return databaseRepository.insert(
                "INSERT INTO users(email, password, full_name, phone) VALUES (?, ?, ?, ?)",
                email,
                password,
                fullName,
                phone);
    }

    public void updateProfile(long userId, String fullName, String phone) {
        databaseRepository.update(
                "UPDATE users SET full_name = ?, phone = ? WHERE id = ?", fullName, phone, userId);
    }

    public void enableHost(long userId) {
        databaseRepository.update("UPDATE users SET is_host = TRUE WHERE id = ?", userId);
    }

    public void verifyEmail(long userId) {
        databaseRepository.update("UPDATE users SET email_verified = TRUE WHERE id = ?", userId);
    }

    public void updatePassword(long userId, String encodedPassword) {
        databaseRepository.update(
                "UPDATE users SET password = ? WHERE id = ?", encodedPassword, userId);
    }

    public Optional<Map<String, Object>> findRefreshTokenForUpdate(String tokenHash) {
        List<Map<String, Object>> tokens =
                databaseRepository.all(
                        """
                        SELECT * FROM auth_tokens
                        WHERE token_hash = ? AND purpose = 'REFRESH'
                        FOR UPDATE
                        """,
                        tokenHash);
        return tokens.stream().findFirst();
    }

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

    public void createRefreshToken(
            long userId,
            String tokenHash,
            String installationId,
            String deviceName,
            Instant expiresAt) {
        databaseRepository.insert(
                """
INSERT INTO auth_tokens(user_id, purpose, token_hash, installation_id, device_name, expires_at)
VALUES (?, 'REFRESH', ?, ?, ?, ?)
""",
                userId,
                tokenHash,
                installationId,
                deviceName,
                Timestamp.from(expiresAt));
    }

    public void consumeToken(long tokenId) {
        databaseRepository.update(
                "UPDATE auth_tokens SET consumed_at = UTC_TIMESTAMP(3) WHERE id = ?", tokenId);
    }

    public void revokeDeviceRefreshTokens(long userId, String installationId) {
        databaseRepository.update(
                """
                UPDATE auth_tokens SET consumed_at = UTC_TIMESTAMP(3)
                WHERE user_id = ? AND installation_id = ? AND purpose = 'REFRESH'
                  AND consumed_at IS NULL
                """,
                userId,
                installationId);
    }

    public void revokeUnusedTokens(long userId) {
        databaseRepository.update(
                """
                UPDATE auth_tokens SET consumed_at = UTC_TIMESTAMP(3)
                WHERE user_id = ? AND consumed_at IS NULL
                """,
                userId);
    }

    public void revokeUnusedCodes(long userId, String purpose) {
        databaseRepository.update(
                """
                UPDATE auth_tokens SET consumed_at = UTC_TIMESTAMP(3)
                WHERE user_id = ? AND purpose = ? AND consumed_at IS NULL
                """,
                userId,
                purpose);
    }

    public void createEmailCode(long userId, String purpose, String tokenHash) {
        databaseRepository.insert(
                """
                INSERT INTO auth_tokens(user_id, purpose, token_hash, expires_at)
                VALUES (?, ?, ?, DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 10 MINUTE))
                """,
                userId,
                purpose,
                tokenHash);
    }

    public Map<String, Object> findRecentEmailCodeStats(long userId, String purpose) {
        return databaseRepository.one(
                """
                SELECT COUNT(*) AS issued_count, MAX(created_at) AS latest_issued_at
                FROM auth_tokens
                WHERE user_id = ? AND purpose = ?
                  AND created_at > DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 1 HOUR)
                """,
                userId,
                purpose);
    }

    public Optional<Map<String, Object>> findUnusedEmailCodeForUpdate(
            long userId, String purpose, String tokenHash) {
        List<Map<String, Object>> tokens =
                databaseRepository.all(
                        """
SELECT * FROM auth_tokens
WHERE user_id = ? AND purpose = ? AND token_hash = ? AND consumed_at IS NULL
FOR UPDATE
""",
                        userId,
                        purpose,
                        tokenHash);
        return tokens.stream().findFirst();
    }
}
