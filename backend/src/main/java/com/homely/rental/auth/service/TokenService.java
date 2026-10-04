package com.homely.rental.auth.service;

import com.homely.rental.auth.dto.response.LoginResponse;
import com.homely.rental.auth.entity.RefreshToken;
import com.homely.rental.auth.entity.User;
import com.homely.rental.auth.repository.RefreshTokenRepository;
import com.homely.rental.auth.security.SecurityUtils;
import com.homely.rental.auth.security.AccountAccessException;
import com.homely.rental.auth.constant.UserStatus;
import com.homely.rental.common.exception.IdInvalidException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Token service handling JWT creation and refresh token lifecycle.
 *
 * Security design (SEC-02, B3 fix):
 * - Access tokens carry claim "token_type":"access" + user info + roles.
 * - Refresh tokens carry claim "token_type":"refresh" ONLY (no roles/user info).
 * - JwtDecoder validates signature+expiry for both; SecurityConfig's customizer
 *   rejects tokens where token_type != "access" on resource server endpoints.
 * - Refresh tokens are stored as SHA-256 hashes in the refresh_tokens table,
 *   supporting multi-device sessions and per-device revocation.
 */
@Service
@RequiredArgsConstructor
public class TokenService {
    private static final MacAlgorithm JWT_ALGORITHM = SecurityUtils.JWT_ALGORITHM;
    public static final String TOKEN_TYPE_CLAIM = "token_type";
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${homely.jwt.access-token-validity-in-seconds}")
    private long accessTokenExpiration;

    @Value("${homely.jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpiration;

    /**
     * Create an access token with user info and roles embedded.
     */
    public String createAccessToken(String email, LoginResponse loginResponse) {
        LoginResponse.UserInsideToken userToken = new LoginResponse.UserInsideToken();
        userToken.setId(loginResponse.getUser().getId());
        userToken.setFullName(loginResponse.getUser().getFullName());
        userToken.setEmail(loginResponse.getUser().getEmail());

        Instant now = Instant.now();
        Instant validity = now.plus(this.accessTokenExpiration, ChronoUnit.SECONDS);

        List<String> roles = List.copyOf(loginResponse.getUser().getRoles());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(validity)
                .subject(email)
                .claim(TOKEN_TYPE_CLAIM, TOKEN_TYPE_ACCESS)
                .claim("roles", roles)
                .claim("user", userToken)
                .build();

        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    /**
     * Create a refresh token (opaque JWT with no roles/user data).
     * The token value is hashed and stored in the refresh_tokens table for session tracking.
     *
     * @return the raw JWT refresh token string (to be returned to the client)
     */
    @Transactional
    public String createRefreshToken(String email, LoginResponse loginResponse, User user,
                                     String installationId, String deviceName) {
        Instant now = Instant.now();
        Instant validity = now.plus(this.refreshTokenExpiration, ChronoUnit.SECONDS);

        // Unique JTI to prevent collision
        String jti = UUID.randomUUID().toString();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(validity)
                .subject(email)
                .id(jti)
                .claim(TOKEN_TYPE_CLAIM, TOKEN_TYPE_REFRESH)
                .build();

        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        String tokenValue = this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();

        // Revoke any existing session for same device
        refreshTokenRepository.revokeByUserIdAndInstallationId(user.getId(), installationId);

        // Store hashed token in DB
        RefreshToken entity = new RefreshToken();
        entity.setTokenHash(hashToken(tokenValue));
        entity.setUser(user);
        entity.setInstallationId(installationId);
        entity.setDeviceName(deviceName);
        entity.setExpiresAt(validity);
        refreshTokenRepository.save(entity);

        return tokenValue;
    }

    /**
     * Validate a refresh token: check JWT signature, expiry, token_type, and DB record.
     *
     * @return the User associated with this refresh token session
     * @throws IdInvalidException if the token is invalid, expired, or revoked
     */
    @Transactional(readOnly = true)
    public User validateRefreshToken(String rawToken) throws IdInvalidException {
        Jwt decoded;
        try {
            decoded = jwtDecoder.decode(rawToken);
        } catch (JwtException e) {
            throw new IdInvalidException("Refresh token is invalid or expired");
        }

        // Must be a refresh token, not an access token
        String tokenType = decoded.getClaimAsString(TOKEN_TYPE_CLAIM);
        if (!TOKEN_TYPE_REFRESH.equals(tokenType)) {
            throw new IdInvalidException("Invalid token type for refresh");
        }

        // Look up in DB by hash
        String hash = hashToken(rawToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHashAndRevokedFalse(hash)
                .orElseThrow(() -> new IdInvalidException("Refresh token is revoked or not found"));

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new IdInvalidException("Refresh token has expired");
        }

        User user = stored.getUser();
        if (user.getStatus() != UserStatus.ACTIVE || user.isSuspended()) {
            throw new AccountAccessException(403, "ACCOUNT_INACTIVE", "Your account is not active");
        }
        return user;
    }

    /**
     * Revoke all sessions for a user (used on logout-all or account deletion).
     */
    @Transactional
    public void revokeAllSessions(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    /**
     * Revoke a specific refresh token (used on single-device logout).
     */
    @Transactional
    public void revokeRefreshToken(String rawToken) {
        String hash = hashToken(rawToken);
        refreshTokenRepository.findByTokenHashAndRevokedFalse(hash)
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                });
    }

    public long getAccessTokenExpiration() {
        return this.accessTokenExpiration;
    }

    @Transactional(rollbackFor = Exception.class)
    public LoginResponse rotate(com.homely.rental.auth.dto.request.RefreshRequest request) throws IdInvalidException {
        RefreshToken stored = refreshTokenRepository.lockByHash(hashToken(request.getRefreshToken()))
                .orElseThrow(() -> new IdInvalidException("Refresh token is invalid"));
        if (stored.isRevoked() || !stored.getExpiresAt().isAfter(Instant.now()))
            throw new IdInvalidException("Refresh token is expired or revoked");
        User user = validateRefreshToken(request.getRefreshToken());
        if (request.getInstallationId() != null && !request.getInstallationId().equals(stored.getInstallationId()))
            throw new IdInvalidException("Refresh token belongs to another installation");
        stored.setRevoked(true);
        refreshTokenRepository.flush();
        LoginResponse response = new LoginResponse();
        response.setUser(UserService.convertToUserLogin(user));
        response.setAccessToken(createAccessToken(user.getEmail(), response));
        response.setRefreshToken(createRefreshToken(user.getEmail(), response, user, stored.getInstallationId(), stored.getDeviceName()));
        response.setExpiresIn(accessTokenExpiration);
        return response;
    }

    @Transactional
    public String revokeOwnedSession(String rawToken, Long userId) {
        RefreshToken session = refreshTokenRepository.lockByHash(hashToken(rawToken)).orElse(null);
        if (session == null || !session.getUser().getId().equals(userId)) return null;
        session.setRevoked(true);
        return session.getInstallationId();
    }

    private static String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
