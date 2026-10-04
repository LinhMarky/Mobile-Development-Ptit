package com.homely.rental.auth.service;

import com.homely.rental.auth.entity.OneTimeToken;
import com.homely.rental.auth.entity.User;
import com.homely.rental.auth.repository.OneTimeTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Email verification service (AUTH05–06, SEC-03).
 * Generates one-time tokens and sends verification emails.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final OneTimeTokenRepository oneTimeTokenRepository;
    private final org.springframework.mail.javamail.JavaMailSender mailSender;
    @org.springframework.beans.factory.annotation.Value("${homely.mail.from:noreply@homely.local}")
    private String from;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTE_LENGTH = 32;
    private static final long TOKEN_VALIDITY_HOURS = 24;

    /**
     * Generate a new email verification token for the user.
     * Returns the raw token to be sent via email.
     */
    @Transactional
    public String generateVerificationToken(User user) {
        byte[] tokenBytes = new byte[TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        String tokenHash = hashToken(rawToken);

        OneTimeToken token = new OneTimeToken();
        token.setTokenHash(tokenHash);
        token.setUser(user);
        token.setPurpose(OneTimeToken.OneTimeTokenPurpose.EMAIL_VERIFICATION);
        token.setExpiresAt(Instant.now().plus(TOKEN_VALIDITY_HOURS, ChronoUnit.HOURS));
        oneTimeTokenRepository.save(token);

        org.springframework.mail.SimpleMailMessage message = new org.springframework.mail.SimpleMailMessage();
        message.setFrom(from);
        message.setTo(user.getEmail());
        message.setSubject("Homely — Xác minh email");
        message.setText("Nhập mã sau vào ứng dụng Homely để xác minh email (hiệu lực 24 giờ):\n\n" + rawToken);
        mailSender.send(message);

        return rawToken;
    }

    /**
     * Verify a token submitted by the user (AUTH06).
     */
    @Transactional
    public Optional<OneTimeToken> verifyToken(String rawToken) {
        String tokenHash = hashToken(rawToken);
        Optional<OneTimeToken> tokenOpt = oneTimeTokenRepository.findByTokenHashAndUsedFalse(tokenHash);

        if (tokenOpt.isPresent()) {
            OneTimeToken token = tokenOpt.get();
            if (token.isUsed() || token.getPurpose() != OneTimeToken.OneTimeTokenPurpose.EMAIL_VERIFICATION
                    || !token.getExpiresAt().isAfter(Instant.now())) {
                return Optional.empty(); // Expired
            }
            token.setUsed(true);
            token.getUser().setEmailVerified(true);
            oneTimeTokenRepository.save(token);
            return Optional.of(token);
        }
        return Optional.empty();
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
