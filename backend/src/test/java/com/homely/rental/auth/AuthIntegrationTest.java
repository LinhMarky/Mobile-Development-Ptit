package com.homely.rental.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homely.rental.auth.constant.UserStatus;
import com.homely.rental.auth.entity.*;
import com.homely.rental.auth.repository.*;
import io.minio.MinioClient;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Real filters, controllers, JWT signatures, BCrypt and committed JPA transactions. */
@SpringBootTest(properties={
        "spring.datasource.url=jdbc:h2:mem:auth-integration;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false", "homely.demo.enabled=false", "homely.demo.seed=false", "homely.fcm.enabled=false",
        "homely.payment.mock-enabled=false", "logging.level.org.hibernate.SQL=OFF", "management.health.mail.enabled=false",
        "logging.level.org.springframework.web=INFO", "logging.level.org.springframework.security=INFO", "debug=false"})
@AutoConfigureMockMvc
class AuthIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired RefreshTokenRepository sessions;
    @Autowired OneTimeTokenRepository tokens;
    @Autowired DeviceTokenRepository devices;
    @Autowired NotificationPreferenceRepository preferences;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtDecoder decoder;
    @Autowired PlatformTransactionManager manager;
    @Autowired com.homely.rental.auth.service.AccountDeletionProcessor deletionProcessor;
    @Autowired com.homely.rental.auth.repository.AccountDeletionRequestRepository deletionRequests;
    @Autowired com.homely.rental.auth.security.AccountAccessService accountAccess;
    @MockBean JavaMailSender mail;
    // Media is not part of this test; prevent bucket initialization from contacting external storage.
    @MockBean MinioClient storage;
    private static final String PASSWORD = "Auth-test-password-123";
    String email;
    String installation;
    String verificationCode;

    @BeforeEach void fixture() throws Exception {
        email = UUID.randomUUID() + "@example.test"; installation = UUID.randomUUID().toString();
        for (RoleName name : RoleName.values()) if (roles.findByName(name).isEmpty()) {
            Role role = new Role(); role.setName(name); roles.save(role);
        }
        doAnswer(call -> {
            SimpleMailMessage message = call.getArgument(0);
            if (Arrays.asList(message.getTo()).contains(email)) verificationCode = message.getText().strip().substring(message.getText().strip().lastIndexOf('\n') + 1);
            return null;
        }).when(mail).send(any(SimpleMailMessage.class));
        expect(postJson("/auth/register", Map.of("email", email, "password", PASSWORD, "full_name", "Auth Test")), 201);
    }

    private MvcResult postJson(String path, Object body) throws Exception {
        return request(post("/api/v1" + path), body, null);
    }
    private MvcResult request(MockHttpServletRequestBuilder builder, Object body, String access) throws Exception {
        if (access != null) builder.header("Authorization", "Bearer " + access);
        if (body != null) builder.contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(body));
        return mvc.perform(builder).andReturn();
    }
    private JsonNode expect(MvcResult result, int status) throws Exception {
        assertThat(result.getResponse().getStatus()).as("HTTP status").isEqualTo(status);
        String content = result.getResponse().getContentAsString();
        if (content.isBlank()) return mapper.createObjectNode();
        JsonNode envelope = mapper.readTree(content);
        assertThat(envelope.size()).isEqualTo(3);
        assertThat(envelope.path("statusCode").asInt()).isEqualTo(status);
        assertThat(envelope.has("message")).isTrue();
        assertThat(envelope.has("data")).isTrue();
        assertThat(result.getResponse().getContentType()).startsWith("application/json");
        return envelope.get("data");
    }
    private JsonNode login(String device) throws Exception {
        return expect(postJson("/auth/login", Map.of("email", email, "password", PASSWORD,
                "installation_id", device, "device_name", "Integration test")), 200);
    }
    private String access(JsonNode login) { return login.path("access_token").asText(); }
    private String refresh(JsonNode login) { return login.path("refresh_token").asText(); }
    private void verifyEmail() throws Exception { expect(postJson("/auth/verify-email", Map.of("token", verificationCode)), 200); }
    private void setState(UserStatus state, boolean suspended) {
        new TransactionTemplate(manager).executeWithoutResult(tx -> {
            User user = users.findByEmail(email); user.setStatus(state); user.setSuspended(suspended);
        });
    }
    private void device(JsonNode login, String id) throws Exception {
        expect(request(put("/api/v1/profile/device-token"), Map.of("token", "test-fcm-" + id,
                "installation_id", id, "device_name", "Integration device", "platform", "ANDROID"), access(login)), 200);
    }

    @Test void registrationPersistsOnlyPasswordHashAndDefaultTenantPreferences() throws Exception {
        User user = users.findByEmail(email);
        assertThat(user.getPassword()).isNotEqualTo(PASSWORD);
        assertThat(passwords.matches(PASSWORD, user.getPassword())).isTrue();
        assertThat(user.isEmailVerified()).isFalse(); assertThat(user.isHost()).isFalse();
        assertThat(user.getRoles()).extracting(Role::getName).containsExactly(RoleName.ROLE_TENANT);
        assertThat(preferences.findByUserId(user.getId())).isPresent();
        var response = expect(postJson("/auth/register", Map.of("email", email, "password", PASSWORD, "full_name", "Duplicate")), 400);
        assertThat(response.has("password")).isFalse();
        assertThat(users.findByEmail(email).getId()).isEqualTo(user.getId());
        expect(postJson("/auth/register", Map.of("email", "invalid", "password", "short", "full_name", "X")), 400);
    }

    @Test void verificationIsOneTimeAndEnablesHostWithoutClientSuppliedRole() throws Exception {
        var initial = login(installation);
        expect(request(post("/api/v1/auth/enable-host"), null, access(initial)), 400);
        verifyEmail(); expect(postJson("/auth/verify-email", Map.of("token", verificationCode)), 400);
        expect(request(post("/api/v1/auth/enable-host"), null, access(initial)), 200);
        var updated = login(installation);
        assertThat(updated.path("user").path("roles")).contains(mapper.valueToTree("ROLE_HOST"));
        assertThat(decoder.decode(access(updated)).getClaimAsStringList("roles")).contains("ROLE_TENANT", "ROLE_HOST").doesNotContain("ROLE_ADMIN");
        var profile = expect(request(get("/api/v1/profile"), null, access(updated)), 200);
        assertThat(profile.path("email_verified").asBoolean()).isTrue(); assertThat(profile.has("password")).isFalse();
    }

    @Test void smtpFailureRollsBackRegistrationAndTheSameEmailCanRetry() throws Exception {
        String newEmail = UUID.randomUUID() + "@example.test";
        doThrow(new org.springframework.mail.MailSendException("SMTP unavailable")).when(mail).send(any(SimpleMailMessage.class));
        var failed = expect(postJson("/auth/register", Map.of("email", newEmail, "password", PASSWORD, "full_name", "Retry User")), 503);
        assertThat(failed.path("code").asText()).isEqualTo("EMAIL_UNAVAILABLE");
        assertThat(users.findByEmail(newEmail)).isNull();
        doNothing().when(mail).send(any(SimpleMailMessage.class));
        expect(postJson("/auth/register", Map.of("email", newEmail, "password", PASSWORD, "full_name", "Retry User")), 201);
        assertThat(users.findByEmail(newEmail)).isNotNull();
    }

    @Test void failedResendKeepsTheOriginalVerificationTokenUsable() throws Exception {
        var login = login(installation);
        doThrow(new org.springframework.mail.MailSendException("SMTP unavailable")).when(mail).send(any(SimpleMailMessage.class));
        var failed = expect(request(post("/api/v1/auth/verify-email/resend"), null, access(login)), 503);
        assertThat(failed.path("code").asText()).isEqualTo("EMAIL_UNAVAILABLE");
        verifyEmail();
        assertThat(users.findByEmail(email).isEmailVerified()).isTrue();
    }

    @Test void pendingDeletionBlocksProfileAndRefreshButAllowsStatusAndLogout() throws Exception {
        var login = login(installation);
        var request = expect(request(post("/api/v1/auth/delete-account"), Map.of("current_password", PASSWORD), access(login)), 200);
        assertThat(request.path("status").asText()).isEqualTo("PENDING");
        var state = expect(request(get("/api/v1/auth/delete-account"), null, access(login)), 200);
        assertThat(state.path("status").asText()).isEqualTo("PENDING");
        var blocked = expect(request(get("/api/v1/profile"), null, access(login)), 403);
        assertThat(blocked.path("code").asText()).isEqualTo("ACCOUNT_DELETION_PENDING");
        expect(postJson("/auth/refresh", Map.of("refresh_token", refresh(login))), 403);
        expect(request(post("/api/v1/auth/logout"), Map.of("refresh_token", refresh(login)), access(login)), 200);
    }

    @Test void deletingAndRegisteringTheSameEmailDoesNotReviveOldAccessTokens() throws Exception {
        var original = login(installation);
        Long oldId = users.findByEmail(email).getId();
        expect(request(post("/api/v1/auth/delete-account"), Map.of("current_password", PASSWORD), access(original)), 200);
        deletionProcessor.process(deletionRequests.findByUserId(oldId).orElseThrow().getId());
        expect(postJson("/auth/register", Map.of("email", email, "password", PASSWORD, "full_name", "Replacement")), 201);
        var replacement = login(UUID.randomUUID().toString());
        assertThat(users.findByEmail(email).getId()).isNotEqualTo(oldId);
        var rejected = expect(request(get("/api/v1/profile"), null, access(original)), 401);
        assertThat(rejected.path("code").asText()).isEqualTo("INVALID_ACCESS_TOKEN");
        expect(request(get("/api/v1/profile"), null, access(replacement)), 200);
        expect(postJson("/auth/refresh", Map.of("refresh_token", refresh(original))), 400);
        // Simulate a filter having accepted the old token before deletion/re-registration committed.
        var context = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(decoder.decode(access(original))));
        org.springframework.security.core.context.SecurityContextHolder.setContext(context);
        try {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> new TransactionTemplate(manager)
                    .execute(tx -> accountAccess.requireActive(email)))
                    .isInstanceOf(com.homely.rental.auth.security.AccountAccessException.class)
                    .hasMessage("A valid access token is required");
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @Test void resendSendsUsableCodeButVerifiedAccountCannotResend() throws Exception {
        var login = login(installation); String first = verificationCode;
        expect(request(post("/api/v1/auth/verify-email/resend"), null, access(login)), 200);
        assertThat(verificationCode).isNotEqualTo(first); verifyEmail();
        expect(request(post("/api/v1/auth/verify-email/resend"), null, access(login)), 400);
    }

    @Test void expiredAndWrongPurposeTokensCannotVerifyEmail() throws Exception {
        new TransactionTemplate(manager).executeWithoutResult(tx -> tokens.findByTokenHashAndUsedFalse(hash(verificationCode)).orElseThrow()
                .setExpiresAt(Instant.now().minusSeconds(60)));
        expect(postJson("/auth/verify-email", Map.of("token", verificationCode)), 400);
        String wrongPurpose = UUID.randomUUID().toString(); OneTimeToken token = new OneTimeToken();
        token.setUser(users.findByEmail(email)); token.setTokenHash(hash(wrongPurpose));
        token.setPurpose(OneTimeToken.OneTimeTokenPurpose.PASSWORD_RESET); token.setExpiresAt(Instant.now().plusSeconds(600)); tokens.save(token);
        expect(postJson("/auth/verify-email", Map.of("token", wrongPurpose)), 400);
        assertThat(users.findByEmail(email).isEmailVerified()).isFalse();
        new TransactionTemplate(manager).executeWithoutResult(tx -> assertThat(tokens.findByTokenHashAndUsedFalse(hash(wrongPurpose))).isPresent());
    }

    @Test void wrongCredentialsAndRefreshAsBearerAreRejected() throws Exception {
        var invalid = expect(postJson("/auth/login", Map.of("email", email, "password", "wrong-password",
                "installation_id", installation, "device_name", "Test")), 401);
        assertThat(invalid.path("code").asText()).isEqualTo("INVALID_CREDENTIALS");
        var login = login(installation);
        expect(request(get("/api/v1/profile"), null, refresh(login)), 401);
        expect(request(get("/api/v1/profile"), null, "not-a-jwt"), 401);
        expect(request(get("/api/v1/profile"), null, null), 401);
    }

    @Test void refreshRotatesAndReplayFailsWhileInstallationIsPreserved() throws Exception {
        var login = login(installation);
        expect(postJson("/auth/refresh", Map.of("refresh_token", refresh(login), "installation_id", UUID.randomUUID().toString())), 400);
        var rotated = expect(postJson("/auth/refresh", Map.of("refresh_token", refresh(login))), 200);
        assertThat(refresh(rotated)).isNotEqualTo(refresh(login));
        expect(postJson("/auth/refresh", Map.of("refresh_token", refresh(login))), 400);
        var session = sessions.findByTokenHashAndRevokedFalse(hash(refresh(rotated))).orElseThrow();
        assertThat(session.getInstallationId()).isEqualTo(installation);
        assertThat(session.getDeviceName()).isEqualTo("Integration test");
        expect(request(get("/api/v1/profile"), null, access(rotated)), 200);
        expect(postJson("/auth/refresh", Map.of("refresh_token", access(rotated))), 400);
    }

    @Test void secondLoginOnSameInstallationRevokesPriorSession() throws Exception {
        var first = login(installation); var second = login(installation);
        expect(postJson("/auth/refresh", Map.of("refresh_token", refresh(first))), 400);
        expect(postJson("/auth/refresh", Map.of("refresh_token", refresh(second))), 200);
    }

    @Test void concurrentRefreshConsumesSessionOnlyOnce() throws Exception {
        var login = login(installation);
        var start = new java.util.concurrent.CountDownLatch(1);
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            List<java.util.concurrent.Future<Integer>> results = new ArrayList<>();
            for (int index = 0; index < 2; index++) results.add(pool.submit(() -> {
                start.await(); return postJson("/auth/refresh", Map.of("refresh_token", refresh(login))).getResponse().getStatus();
            }));
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (var result : results) statuses.add(result.get(20, java.util.concurrent.TimeUnit.SECONDS));
            assertThat(statuses).containsExactlyInAnyOrder(200, 400);
        } finally { pool.shutdownNow(); }
    }

    @Test void concurrentVerificationConsumesCodeOnlyOnce() throws Exception {
        var start = new java.util.concurrent.CountDownLatch(1);
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            List<java.util.concurrent.Future<Integer>> results = new ArrayList<>();
            for (int index = 0; index < 2; index++) results.add(pool.submit(() -> {
                start.await(); return postJson("/auth/verify-email", Map.of("token", verificationCode)).getResponse().getStatus();
            }));
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (var result : results) statuses.add(result.get(20, java.util.concurrent.TimeUnit.SECONDS));
            assertThat(statuses).containsExactlyInAnyOrder(200, 400);
            assertThat(users.findByEmail(email).isEmailVerified()).isTrue();
        } finally { pool.shutdownNow(); }
    }

    @Test void singleLogoutRevokesOnlyOwnInstallationAndItsFcmDevice() throws Exception {
        String secondInstallation = UUID.randomUUID().toString();
        var first = login(installation); var second = login(secondInstallation); device(first, installation); device(second, secondInstallation);
        expect(request(post("/api/v1/auth/logout"), Map.of("refresh_token", refresh(first)), access(first)), 200);
        expect(postJson("/auth/refresh", Map.of("refresh_token", refresh(first))), 400);
        expect(postJson("/auth/refresh", Map.of("refresh_token", refresh(second))), 200);
        Long userId = users.findByEmail(email).getId();
        assertThat(devices.findByUserIdAndInstallationId(userId, installation).orElseThrow().isActive()).isFalse();
        assertThat(devices.findByUserIdAndInstallationId(userId, secondInstallation).orElseThrow().isActive()).isTrue();
    }

    @Test void logoutCannotRevokeAnotherUsersSession() throws Exception {
        var owner = login(installation); String originalEmail = email;
        email = UUID.randomUUID() + "@example.test";
        expect(postJson("/auth/register", Map.of("email", email, "password", PASSWORD, "full_name", "Other")), 201);
        var other = login(UUID.randomUUID().toString());
        expect(request(post("/api/v1/auth/logout"), Map.of("refresh_token", refresh(owner)), access(other)), 200);
        email = originalEmail;
        expect(postJson("/auth/refresh", Map.of("refresh_token", refresh(owner))), 200);
    }

    @Test void logoutAllRevokesEveryRefreshSessionAndDevice() throws Exception {
        String secondInstallation = UUID.randomUUID().toString();
        var first = login(installation); var second = login(secondInstallation); device(first, installation); device(second, secondInstallation);
        expect(request(post("/api/v1/auth/logout"), null, access(first)), 200);
        expect(postJson("/auth/refresh", Map.of("refresh_token", refresh(first))), 400);
        expect(postJson("/auth/refresh", Map.of("refresh_token", refresh(second))), 400);
        assertThat(devices.findByUserIdAndActiveTrue(users.findByEmail(email).getId())).isEmpty();
    }

    @Test void suspendedAndDeletedAccountsCannotUsePreviouslyIssuedTokens() throws Exception {
        var login = login(installation); setState(UserStatus.ACTIVE, true);
        expect(request(get("/api/v1/profile"), null, access(login)), 403);
        expect(postJson("/auth/refresh", Map.of("refresh_token", refresh(login))), 403);
        expect(postJson("/auth/login", Map.of("email", email, "password", PASSWORD, "installation_id", installation, "device_name", "Test")), 401);
        setState(UserStatus.DELETED, false);
        expect(request(get("/api/v1/profile"), null, access(login)), 403);
        expect(postJson("/auth/refresh", Map.of("refresh_token", refresh(login))), 403);
    }

    @Test void servletErrorFallbackUsesProblemDtoWithoutExposingErrorAttributes() throws Exception {
        var login = login(installation);
        expect(request(get("/error"), null, null), 401);
        expect(request(get("/error"), null, access(login)), 500);
        for (int status : List.of(404, 500, 503)) {
            var body = expect(request(get("/error")
                    .with(req -> { req.setDispatcherType(jakarta.servlet.DispatcherType.ERROR); return req; })
                    .requestAttr(jakarta.servlet.RequestDispatcher.ERROR_STATUS_CODE, status)
                    .requestAttr(jakarta.servlet.RequestDispatcher.ERROR_REQUEST_URI, "/api/v1/original?token=sensitive")
                    .requestAttr(jakarta.servlet.RequestDispatcher.ERROR_MESSAGE, "SELECT password sensitive")
                    .requestAttr(jakarta.servlet.RequestDispatcher.ERROR_EXCEPTION, new IllegalStateException("sensitive")), null, access(login)), status);
            assertThat(body.size()).isEqualTo(9);
            assertThat(body.path("code").asText()).isEqualTo(status == 404 ? "RESOURCE_NOT_FOUND" : "INTERNAL_ERROR");
            assertThat(body.path("instance").asText()).isEqualTo("/api/v1/original");
            assertThat(body.toString()).doesNotContain("sensitive", "SELECT", "password", "stackTrace");
            assertThat(body.path("field_errors").isArray()).isTrue();
        }
        var anonymousError = expect(request(get("/error").with(req -> { req.setDispatcherType(jakarta.servlet.DispatcherType.ERROR); return req; })
                .requestAttr(jakarta.servlet.RequestDispatcher.ERROR_STATUS_CODE, 503), null, null), 503);
        assertThat(anonymousError.path("code").asText()).isEqualTo("INTERNAL_ERROR");
    }

    private static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception error) { throw new IllegalStateException(error); }
    }
}
