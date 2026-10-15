package com.homely.student14.service;

import com.homely.student14.dto.request.auth.EmailCodeDTO;
import com.homely.student14.dto.request.auth.ResetPasswordDTO;
import com.homely.student14.dto.request.auth.VerifyEmailDTO;
import com.homely.student14.dto.response.auth.EmailCodeResponse;
import com.homely.student14.dto.response.auth.UserAccountResponse;
import com.homely.student14.repository.AuthRepository;
import com.homely.student14.repository.DatabaseRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthChallengeService {
    private static final String EMAIL_VERIFICATION = "EMAIL_VERIFICATION";
    private static final String PASSWORD_RESET = "PASSWORD_RESET";

    private final AuthRepository authRepository;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${student14.mock-mail-enabled:false}")
    private boolean mockMailEnabled;

    @Value("${student14.mail-enabled:false}")
    private boolean mailEnabled;

    @Transactional
    public EmailCodeResponse requestVerificationCode(EmailCodeDTO request) {
        return issueCode(request, EMAIL_VERIFICATION);
    }

    @Transactional
    public EmailCodeResponse requestRecoveryCode(EmailCodeDTO request) {
        return issueCode(request, PASSWORD_RESET);
    }

    @Transactional
    public UserAccountResponse verifyEmail(VerifyEmailDTO request) {
        Map<String, Object> currentUser = findActiveUserForUpdate(request.getEmail());
        long userId = DatabaseRepository.id(currentUser.get("id"));
        Map<String, Object> storedCode =
                findValidCodeForUpdate(userId, EMAIL_VERIFICATION, request.getCode());

        authRepository.verifyEmail(userId);
        authRepository.consumeToken(DatabaseRepository.id(storedCode.get("id")));
        return authService.account(authRepository.findUserById(userId));
    }

    @Transactional
    public UserAccountResponse resetPassword(ResetPasswordDTO request) {
        Map<String, Object> currentUser = findActiveUserForUpdate(request.getEmail());
        long userId = DatabaseRepository.id(currentUser.get("id"));
        findValidCodeForUpdate(userId, PASSWORD_RESET, request.getCode());

        String newPassword = Objects.toString(request.getNewPassword(), "");
        AuthService.validatePassword(newPassword);
        authRepository.updatePassword(userId, passwordEncoder.encode(newPassword));

        // Mã đã dùng và toàn bộ phiên đăng nhập cũ đều bị thu hồi sau khi đổi mật khẩu.
        authRepository.revokeUnusedTokens(userId);
        return authService.account(authRepository.findUserById(userId));
    }

    private EmailCodeResponse issueCode(EmailCodeDTO request, String purpose) {
        if (!mockMailEnabled && !mailEnabled) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Chưa cấu hình gửi email thật hoặc email mô phỏng.");
        }

        Map<String, Object> currentUser = findActiveUserForUpdate(request.getEmail());
        long userId = DatabaseRepository.id(currentUser.get("id"));
        if (!mockMailEnabled) {
            checkResendLimit(userId, purpose);
        }
        authRepository.revokeUnusedCodes(userId, purpose);

        String code = Integer.toString(100000 + secureRandom.nextInt(900000));
        String tokenHash = AuthService.hash(userId + ":" + purpose + ":" + code);
        authRepository.createEmailCode(userId, purpose, tokenHash);

        EmailCodeResponse response = new EmailCodeResponse();
        if (mockMailEnabled) {
            response.setMessage("Email mô phỏng phục vụ báo cáo; mã có hiệu lực 10 phút.");
            response.setDemoCode(code);
        } else {
            String email = currentUser.get("email").toString();
            if (EMAIL_VERIFICATION.equals(purpose)) {
                emailService.sendVerificationCode(email, code);
            } else {
                emailService.sendRecoveryCode(email, code);
            }
            response.setMessage("Mã đã được gửi qua email và có hiệu lực trong 10 phút.");
        }
        return response;
    }

    private void checkResendLimit(long userId, String purpose) {
        // Đã khóa user trước khi đếm, nên hai yêu cầu đồng thời không thể vượt giới hạn.
        Map<String, Object> recentCodes = authRepository.findRecentEmailCodeStats(userId, purpose);
        long issuedCount = ((Number) recentCodes.get("issued_count")).longValue();
        if (issuedCount >= 5) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Bạn đã yêu cầu quá nhiều mã. Vui lòng thử lại sau một giờ.");
        }

        Object latestIssuedAt = recentCodes.get("latest_issued_at");
        if (latestIssuedAt != null) {
            Instant resendAllowedAt = Instant.parse(latestIssuedAt.toString()).plusSeconds(60);
            if (Instant.now().isBefore(resendAllowedAt)) {
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Vui lòng chờ 60 giây trước khi yêu cầu mã mới.");
            }
        }
    }

    private Map<String, Object> findActiveUserForUpdate(String email) {
        String normalizedEmail = AuthService.requiredText(email, "email", 255);
        return authRepository.findActiveUserByEmailForUpdate(normalizedEmail);
    }

    private Map<String, Object> findValidCodeForUpdate(
            long userId, String purpose, String enteredCode) {
        String code = AuthService.requiredText(enteredCode, "code", 6);
        String tokenHash = AuthService.hash(userId + ":" + purpose + ":" + code);
        Map<String, Object> storedCode =
                authRepository
                        .findUnusedEmailCodeForUpdate(userId, purpose, tokenHash)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST,
                                                "Mã không đúng hoặc đã hết hạn."));

        Instant expiresAt = Instant.parse(storedCode.get("expires_at").toString());
        if (!expiresAt.isAfter(Instant.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Mã không đúng hoặc đã hết hạn.");
        }
        return storedCode;
    }
}
