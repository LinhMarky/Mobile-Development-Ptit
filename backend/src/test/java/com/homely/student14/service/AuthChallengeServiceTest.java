package com.homely.student14.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.homely.student14.dto.request.auth.EmailCodeDTO;
import com.homely.student14.dto.request.auth.ResetPasswordDTO;
import com.homely.student14.dto.request.auth.VerifyEmailDTO;
import com.homely.student14.dto.response.auth.EmailCodeResponse;
import com.homely.student14.repository.AuthRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AuthChallengeServiceTest {
    @Mock private AuthRepository authRepository;
    @Mock private AuthService authService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;

    private AuthChallengeService authChallengeService;

    @BeforeEach
    void setUp() {
        authChallengeService =
                new AuthChallengeService(
                        authRepository, authService, passwordEncoder, emailService);
        ReflectionTestUtils.setField(authChallengeService, "mailEnabled", true);
    }

    @Test
    void realVerificationSendsCodeByEmailAndDoesNotExposeItInJson() throws Exception {
        prepareUser();
        when(authRepository.findRecentEmailCodeStats(1L, "EMAIL_VERIFICATION"))
                .thenReturn(recentCodes(0, null));

        EmailCodeResponse response = authChallengeService.requestVerificationCode(emailRequest());
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendVerificationCode(eq("student@example.test"), codeCaptor.capture());
        String code = codeCaptor.getValue();
        assertTrue(code.matches("[0-9]{6}"));
        verify(authRepository)
                .createEmailCode(
                        1L, "EMAIL_VERIFICATION", AuthService.hash("1:EMAIL_VERIFICATION:" + code));

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        JsonNode json = objectMapper.valueToTree(response);
        assertEquals(1, json.size());
        assertTrue(json.has("message"));
        assertFalse(json.has("demo_code"));
        assertFalse(json.toString().contains(code));
    }

    @Test
    void realRecoveryUsesItsOwnPurposeAndSendsCodeByEmail() throws Exception {
        prepareUser();
        when(authRepository.findRecentEmailCodeStats(1L, "PASSWORD_RESET"))
                .thenReturn(recentCodes(1, Instant.now().minusSeconds(90)));

        EmailCodeResponse response = authChallengeService.requestRecoveryCode(emailRequest());
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendRecoveryCode(eq("student@example.test"), codeCaptor.capture());
        verify(authRepository)
                .createEmailCode(
                        1L,
                        "PASSWORD_RESET",
                        AuthService.hash("1:PASSWORD_RESET:" + codeCaptor.getValue()));
        JsonNode json = new ObjectMapper().valueToTree(response);
        assertEquals(1, json.size());
        assertTrue(json.has("message"));
        assertFalse(json.toString().contains(codeCaptor.getValue()));
    }

    @Test
    void disabledEmailDoesNotIssueRecoveryCode() {
        ReflectionTestUtils.setField(authChallengeService, "mailEnabled", false);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> authChallengeService.requestRecoveryCode(emailRequest()));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
        verifyNoInteractions(authRepository, emailService);
    }

    @Test
    void smtpFailureDoesNotReturnSuccessfulCodeResponse() {
        prepareUser();
        when(authRepository.findRecentEmailCodeStats(1L, "EMAIL_VERIFICATION"))
                .thenReturn(recentCodes(0, null));
        doThrow(new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE, "Không gửi được email lúc này."))
                .when(emailService).sendVerificationCode(anyString(), anyString());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> authChallengeService.requestVerificationCode(emailRequest()));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
        assertEquals("Không gửi được email lúc này.", exception.getReason());
    }

    @Test
    void resendWithinSixtySecondsDoesNotInvalidateExistingCodeOrSendAnotherEmail() {
        prepareUser();
        when(authRepository.findRecentEmailCodeStats(1L, "EMAIL_VERIFICATION"))
                .thenReturn(recentCodes(1, Instant.now().minusSeconds(30)));

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> authChallengeService.requestVerificationCode(emailRequest()));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatusCode());
        verify(authRepository, never()).revokeUnusedCodes(anyLong(), anyString());
        verify(authRepository, never()).createEmailCode(anyLong(), anyString(), anyString());
        verifyNoInteractions(emailService);
    }

    @Test
    void fiveCodesInOneHourPreventAnotherCodeEvenAfterResendCooldown() {
        prepareUser();
        when(authRepository.findRecentEmailCodeStats(1L, "PASSWORD_RESET"))
                .thenReturn(recentCodes(5, Instant.now().minusSeconds(120)));

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> authChallengeService.requestRecoveryCode(emailRequest()));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatusCode());
        verify(authRepository, never()).createEmailCode(anyLong(), anyString(), anyString());
        verifyNoInteractions(emailService);
    }

    @Test
    void disabledEmailDoesNotIssueVerificationCode() {
        ReflectionTestUtils.setField(authChallengeService, "mailEnabled", false);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> authChallengeService.requestVerificationCode(emailRequest()));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
        verifyNoInteractions(authRepository, emailService);
    }

    @Test
    void overlongCodeDoesNotQueryTokensOrVerifyEmail() {
        prepareUser();

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> authChallengeService.verifyEmail(verificationRequest("1234567")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(authRepository, never())
                .findUnusedEmailCodeForUpdate(anyLong(), anyString(), anyString());
        verify(authRepository, never()).verifyEmail(anyLong());
    }

    @Test
    void wrongCodeDoesNotVerifyEmailOrConsumeAnotherCode() {
        prepareUser();
        when(authRepository.findUnusedEmailCodeForUpdate(
                        1L, "EMAIL_VERIFICATION", AuthService.hash("1:EMAIL_VERIFICATION:123456")))
                .thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> authChallengeService.verifyEmail(verificationRequest("123456")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(authRepository, never()).verifyEmail(anyLong());
        verify(authRepository, never()).consumeToken(anyLong());
    }

    @Test
    void expiredRecoveryCodeDoesNotChangePasswordOrRevokeSessions() {
        prepareUser();
        when(authRepository.findUnusedEmailCodeForUpdate(
                        1L, "PASSWORD_RESET", AuthService.hash("1:PASSWORD_RESET:123456")))
                .thenReturn(
                        Optional.of(
                                Map.of(
                                        "id",
                                        7L,
                                        "expires_at",
                                        Instant.now().minusSeconds(60).toString())));
        ResetPasswordDTO request = new ResetPasswordDTO();
        request.setEmail("student@example.test");
        request.setCode("123456");
        request.setNewPassword("UpdatedPassword@123");

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> authChallengeService.resetPassword(request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(authRepository, never()).updatePassword(anyLong(), anyString());
        verify(authRepository, never()).revokeUnusedTokens(anyLong());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void consumedVerificationCodeCannotBeUsedAgain() {
        prepareUser();
        when(authRepository.findUnusedEmailCodeForUpdate(
                        1L, "EMAIL_VERIFICATION", AuthService.hash("1:EMAIL_VERIFICATION:123456")))
                .thenReturn(
                        Optional.of(
                                Map.of(
                                        "id",
                                        7L,
                                        "expires_at",
                                        Instant.now().plusSeconds(600).toString())))
                .thenReturn(Optional.empty());
        when(authRepository.findUserById(1L)).thenReturn(Map.of("id", 1L));

        authChallengeService.verifyEmail(verificationRequest("123456"));
        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> authChallengeService.verifyEmail(verificationRequest("123456")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(authRepository, times(1)).verifyEmail(1L);
        verify(authRepository, times(1)).consumeToken(7L);
    }

    private void prepareUser() {
        when(authRepository.findActiveUserByEmailForUpdate("student@example.test"))
                .thenReturn(Map.of("id", 1L, "email", "student@example.test"));
    }

    private EmailCodeDTO emailRequest() {
        EmailCodeDTO request = new EmailCodeDTO();
        request.setEmail("student@example.test");
        return request;
    }

    private VerifyEmailDTO verificationRequest(String code) {
        VerifyEmailDTO request = new VerifyEmailDTO();
        request.setEmail("student@example.test");
        request.setCode(code);
        return request;
    }

    private Map<String, Object> recentCodes(long count, Instant latestIssuedAt) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("issued_count", count);
        stats.put("latest_issued_at", latestIssuedAt == null ? null : latestIssuedAt.toString());
        return stats;
    }
}
