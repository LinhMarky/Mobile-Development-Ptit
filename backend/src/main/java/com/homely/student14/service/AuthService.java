package com.homely.student14.service;

import com.homely.student14.dto.request.auth.LoginDTO;
import com.homely.student14.dto.request.auth.RefreshTokenDTO;
import com.homely.student14.dto.request.auth.RegisterDTO;
import com.homely.student14.dto.request.auth.UpdateProfileDTO;
import com.homely.student14.dto.response.auth.LoginResponse;
import com.homely.student14.dto.response.auth.UserAccountResponse;
import com.homely.student14.repository.AuthRepository;
import com.homely.student14.repository.DatabaseRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final long ACCESS_TOKEN_SECONDS = 15 * 60;
    private static final long REFRESH_TOKEN_SECONDS = 30L * 24 * 60 * 60;

    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    public Map<String, Object> user(Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập.");
        }

        long userId = DatabaseRepository.id(jwt.getSubject());
        Map<String, Object> currentUser = authRepository.findUserById(userId);
        checkActiveAccount(currentUser);
        return currentUser;
    }

    public Map<String, Object> role(Jwt jwt, String requiredRole) {
        Map<String, Object> currentUser = user(jwt);
        if (!DatabaseRepository.flag(currentUser.get("is_" + requiredRole))) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Tài khoản không có quyền " + requiredRole + ".");
        }
        return currentUser;
    }

    public UserAccountResponse getCurrentAccount(Jwt jwt) {
        Map<String, Object> currentUser = user(jwt);
        return account(currentUser);
    }

    public UserAccountResponse account(Map<String, Object> currentUser) {
        UserAccountResponse response = new UserAccountResponse();
        response.setId(DatabaseRepository.id(currentUser.get("id")));
        response.setEmail(Objects.toString(currentUser.get("email"), null));
        response.setFullName(Objects.toString(currentUser.get("full_name"), null));
        response.setPhone(Objects.toString(currentUser.get("phone"), null));
        response.setAvatarUrl(Objects.toString(currentUser.get("avatar_url"), null));
        response.setEmailVerified(DatabaseRepository.flag(currentUser.get("email_verified")));
        response.setStatus(Objects.toString(currentUser.get("status"), null));

        List<String> roles = new ArrayList<>();
        if (DatabaseRepository.flag(currentUser.get("is_tenant"))) {
            roles.add("ROLE_TENANT");
        }
        if (DatabaseRepository.flag(currentUser.get("is_host"))) {
            roles.add("ROLE_HOST");
        }
        if (DatabaseRepository.flag(currentUser.get("is_admin"))) {
            roles.add("ROLE_ADMIN");
        }
        response.setRoles(roles);
        return response;
    }

    @Transactional
    public LoginResponse login(LoginDTO request) {
        String email = requiredText(request.getEmail(), "email", 255).toLowerCase(Locale.ROOT);
        Map<String, Object> currentUser =
                authRepository
                        .findUserByEmail(email)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.UNAUTHORIZED,
                                                "Email hoặc mật khẩu không đúng."));

        String password = Objects.toString(request.getPassword(), "");
        String encodedPassword = currentUser.get("password").toString();
        if (!passwordEncoder.matches(password, encodedPassword)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng.");
        }
        checkActiveAccount(currentUser);

        String installationId = requiredText(request.getInstallationId(), "installation_id", 36);
        String deviceName = requiredText(request.getDeviceName(), "device_name", 100);
        return createLoginResponse(currentUser, installationId, deviceName);
    }

    @Transactional
    public LoginResponse refresh(RefreshTokenDTO request) {
        String refreshToken = requiredText(request.getRefreshToken(), "refresh_token", 300);
        Map<String, Object> storedToken =
                authRepository
                        .findRefreshTokenForUpdate(hash(refreshToken))
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.UNAUTHORIZED,
                                                "Phiên đăng nhập không hợp lệ."));

        Instant expiresAt = Instant.parse(storedToken.get("expires_at").toString());
        if (storedToken.get("consumed_at") != null || !expiresAt.isAfter(Instant.now())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Phiên đăng nhập đã hết hạn.");
        }

        long userId = DatabaseRepository.id(storedToken.get("user_id"));
        String installationId = storedToken.get("installation_id").toString();
        if (authRepository.findActiveInstallationUser(installationId).orElse(0L) != userId) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Thiết bị đã đăng nhập tài khoản khác. Vui lòng đăng nhập lại.");
        }
        Map<String, Object> currentUser = authRepository.findUserById(userId);
        checkActiveAccount(currentUser);

        // Mỗi refresh token chỉ được dùng một lần, sau đó cấp cặp token mới.
        authRepository.consumeToken(DatabaseRepository.id(storedToken.get("id")));
        String deviceName = storedToken.get("device_name").toString();
        return createLoginResponse(currentUser, installationId, deviceName);
    }

    @Transactional
    public void logout(Jwt jwt) {
        Map<String, Object> currentUser = user(jwt);
        long userId = DatabaseRepository.id(currentUser.get("id"));
        String installationId = jwt.getClaimAsString("installation_id");

        // Thu hồi cả token mới nếu app vừa tự refresh trước khi đăng xuất.
        authRepository.revokeDeviceRefreshTokens(userId, installationId);
        authRepository.clearDeviceToken(userId, installationId);
    }

    @Transactional
    public UserAccountResponse register(RegisterDTO request) {
        String email = requiredText(request.getEmail(), "email", 255).toLowerCase(Locale.ROOT);
        if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Email không đúng định dạng.");
        }

        String password = Objects.toString(request.getPassword(), "");
        validatePassword(password);
        String fullName = requiredText(request.getFullName(), "full_name", 100);
        String phone = optionalText(request.getPhone(), "phone", 15);
        String encodedPassword = passwordEncoder.encode(password);

        long userId = authRepository.createUser(email, encodedPassword, fullName, phone);
        return account(authRepository.findUserById(userId));
    }

    @Transactional
    public UserAccountResponse updateProfile(Jwt jwt, UpdateProfileDTO request) {
        Map<String, Object> currentUser = user(jwt);
        long userId = DatabaseRepository.id(currentUser.get("id"));
        String fullName = requiredText(request.getFullName(), "full_name", 100);
        String phone = optionalText(request.getPhone(), "phone", 15);

        authRepository.updateProfile(userId, fullName, phone);
        return account(authRepository.findUserById(userId));
    }

    @Transactional
    public UserAccountResponse enableHost(Jwt jwt) {
        Map<String, Object> currentUser = user(jwt);
        if (!DatabaseRepository.flag(currentUser.get("email_verified"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cần xác minh email.");
        }

        long userId = DatabaseRepository.id(currentUser.get("id"));
        authRepository.enableHost(userId);
        return account(authRepository.findUserById(userId));
    }

    private LoginResponse createLoginResponse(
            Map<String, Object> currentUser, String installationId, String deviceName) {
        long userId = DatabaseRepository.id(currentUser.get("id"));
        Instant issuedAt = Instant.now();
        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .subject(Long.toString(userId))
                        .claim("installation_id", installationId)
                        .issuedAt(issuedAt)
                        .expiresAt(issuedAt.plusSeconds(ACCESS_TOKEN_SECONDS))
                        .issuer("roomly-student14")
                        .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String accessToken =
                jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        String refreshToken = java.util.UUID.randomUUID() + "." + java.util.UUID.randomUUID();

        authRepository.createRefreshToken(
                userId,
                hash(refreshToken),
                installationId,
                deviceName,
                issuedAt.plusSeconds(REFRESH_TOKEN_SECONDS));

        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn((int) ACCESS_TOKEN_SECONDS);
        response.setUser(account(currentUser));
        return response;
    }

    private void checkActiveAccount(Map<String, Object> currentUser) {
        if (!"ACTIVE".equals(currentUser.get("status"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản không hoạt động.");
        }
    }

    public static void validatePassword(String password) {
        if (password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Mật khẩu cần ít nhất 12 ký tự và tối đa 72 byte.");
        }
    }

    static String requiredText(String value, String fieldName, int maxLength) {
        String normalized = optionalText(value, fieldName, maxLength);
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Trường " + fieldName + " không hợp lệ (tối đa " + maxLength + " ký tự).");
        }
        return normalized;
    }

    private static String optionalText(String value, String fieldName, int maxLength) {
        String normalized = Objects.toString(value, "").trim();
        if (normalized.codePointCount(0, normalized.length()) > maxLength) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Trường " + fieldName + " không hợp lệ (tối đa " + maxLength + " ký tự).");
        }
        return normalized;
    }

    public static String hash(String value) {
        try {
            byte[] hashedBytes =
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashedBytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Không hỗ trợ SHA-256.", exception);
        }
    }
}
