package com.homely.rental.auth.controller;

import com.homely.rental.auth.dto.request.*;
import com.homely.rental.auth.dto.response.LoginResponse;
import com.homely.rental.auth.dto.response.UserResponse;
import com.homely.rental.auth.entity.AccountDeletionRequest;
import com.homely.rental.auth.entity.OneTimeToken;
import com.homely.rental.auth.entity.User;
import com.homely.rental.auth.constant.UserStatus;
import com.homely.rental.auth.security.SecurityUtils;
import com.homely.rental.auth.service.AccountDeletionService;
import com.homely.rental.auth.service.EmailService;
import com.homely.rental.auth.service.TokenService;
import com.homely.rental.auth.service.UserService;
import com.homely.rental.common.annotation.ApiMessage;
import com.homely.rental.common.exception.IdInvalidException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * Authentication controller (AUTH01–AUTH06, AUTH09, AUTH12).
 * Handles registration, login, refresh, logout, email verification, and account deletion.
 */
@RequestMapping(path = "${apiPrefix}/auth")
@RestController
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final TokenService tokenService;
    private final UserService userService;
    private final EmailService emailService;
    private final AccountDeletionService accountDeletionService;
    private final com.homely.rental.auth.repository.DeviceTokenRepository devices;

    // AUTH01: Register
    @PostMapping("/register")
    @ApiMessage("Register a new user")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest dto) throws Exception {
        UserResponse newUser = this.userService.registerUser(dto);

        // Generate email verification token
        User user = this.userService.handleGetUserByUsername(dto.getEmail());
        if (user != null) {
            emailService.generateVerificationToken(user);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
    }

    // AUTH02: Login
    @PostMapping("/login")
    @ApiMessage("Login by credential")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginDTO) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginDTO.getEmail(), loginDTO.getPassword());

        Authentication authentication = this.authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        User currentUserDB = this.userService.handleGetUserByUsername(loginDTO.getEmail());
        if (currentUserDB == null) {
            throw new RuntimeException("User not found after authentication");
        }

        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setUser(UserService.convertToUserLogin(currentUserDB));

        String accessToken = this.tokenService.createAccessToken(authentication.getName(), loginResponse);

        // Create refresh token using the proper refresh_tokens table (B4 fix)
        String installationId = loginDTO.getInstallationId() != null
                ? loginDTO.getInstallationId() : java.util.UUID.randomUUID().toString();
        String deviceName = loginDTO.getDeviceName() != null
                ? loginDTO.getDeviceName() : "Unknown Device";
        String refreshToken = this.tokenService.createRefreshToken(
                authentication.getName(), loginResponse, currentUserDB, installationId, deviceName);

        loginResponse.setAccessToken(accessToken);
        loginResponse.setRefreshToken(refreshToken);
        loginResponse.setExpiresIn(this.tokenService.getAccessTokenExpiration());

        return ResponseEntity.ok(loginResponse);
    }

    // AUTH03: Refresh token — now validates via refresh_tokens table
    @PostMapping("/refresh")
    @ApiMessage("Refresh access token")
    public ResponseEntity<LoginResponse> refreshToken(@Valid @RequestBody RefreshRequest request) throws IdInvalidException {
        return ResponseEntity.ok(tokenService.rotate(request));
    }

    // AUTH04: Logout — revoke the refresh token for this session
    @PostMapping("/logout")
    @ApiMessage("Logout user")
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshRequest request) throws IdInvalidException {
        String email = SecurityUtils.getCurrentUserLogin().orElse("");
        if (email.isEmpty()) {
            throw new IdInvalidException("Access token is not valid");
        }

        User user = userService.handleGetUserByUsername(email);
        if (request != null && request.getRefreshToken() != null) {
            String installation = tokenService.revokeOwnedSession(request.getRefreshToken(), user.getId());
            if (installation != null) devices.deactivateInstallation(user.getId(), installation);
        } else {
            tokenService.revokeAllSessions(user.getId());
            devices.deactivateAllByUserId(user.getId());
        }
        return ResponseEntity.ok(null);
    }

    // AUTH05: Request email verification (resend)
    @PostMapping("/verify-email/resend")
    @ApiMessage("Resend email verification")
    public ResponseEntity<Map<String, String>> resendVerification() throws IdInvalidException {
        String email = SecurityUtils.getCurrentUserLogin().orElse("");
        if (email.isEmpty()) {
            throw new IdInvalidException("Access token is not valid");
        }

        User user = this.userService.handleGetUserByUsername(email);
        if (user == null) {
            throw new IdInvalidException("User not found");
        }
        if (user.isEmailVerified()) {
            throw new IdInvalidException("Email is already verified");
        }

        emailService.generateVerificationToken(user);
        return ResponseEntity.ok(Map.of("message", "Verification email sent"));
    }

    // AUTH06: Verify email
    @PostMapping("/verify-email")
    @ApiMessage("Verify email address")
    public ResponseEntity<Map<String, String>> verifyEmail(@Valid @RequestBody OneTimeTokenRequest request) throws IdInvalidException {
        Optional<OneTimeToken> tokenOpt = emailService.verifyToken(request.getToken());

        if (tokenOpt.isEmpty()) {
            throw new IdInvalidException("Token is invalid or expired");
        }

        OneTimeToken token = tokenOpt.get();
        if (token.getPurpose() != OneTimeToken.OneTimeTokenPurpose.EMAIL_VERIFICATION) {
            throw new IdInvalidException("Invalid token purpose");
        }

        // Token consumption and verification were committed atomically by EmailService.

        return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
    }

    // Get current account info
    @GetMapping("/account")
    @ApiMessage("Get user information")
    public ResponseEntity<LoginResponse.UserLogin> getAccount() throws IdInvalidException {
        String email = SecurityUtils.getCurrentUserLogin().orElse("");
        User currentUserDB = this.userService.handleGetUserByUsername(email);

        if (currentUserDB == null || currentUserDB.getStatus() != UserStatus.ACTIVE) {
            throw new IdInvalidException("Account is inactive");
        }

        return ResponseEntity.ok(UserService.convertToUserLogin(currentUserDB));
    }

    // AUTH09: Enable host role
    @PostMapping("/enable-host")
    @ApiMessage("Enable host role for current user")
    public ResponseEntity<UserResponse> enableHost() throws IdInvalidException {
        return ResponseEntity.ok(this.userService.enableHostRole());
    }

    // AUTH12: Request account deletion
    @PostMapping("/delete-account")
    @ApiMessage("Request account deletion")
    public ResponseEntity<Map<String, String>> requestDeletion(@Valid @RequestBody DeletionRequest dto) throws IdInvalidException {
        AccountDeletionRequest deletionRequest = accountDeletionService.requestDeletion(dto);
        return ResponseEntity.ok(Map.of(
                "message", "Account deletion request submitted",
                "status", deletionRequest.getStatus().name()
        ));
    }
}
