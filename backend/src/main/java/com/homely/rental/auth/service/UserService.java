package com.homely.rental.auth.service;

import com.homely.rental.auth.entity.User;
import com.homely.rental.auth.entity.DeviceToken;
import com.homely.rental.auth.entity.NotificationPreference;
import com.homely.rental.auth.dto.request.*;
import com.homely.rental.auth.dto.response.LoginResponse;
import com.homely.rental.auth.dto.response.UserResponse;
import com.homely.rental.auth.constant.UserStatus;
import com.homely.rental.auth.repository.DeviceTokenRepository;
import com.homely.rental.auth.repository.NotificationPreferenceRepository;
import com.homely.rental.auth.repository.UserRepository;
import com.homely.rental.auth.security.UserResolver;
import com.homely.rental.common.exception.ConflictException;
import com.homely.rental.common.exception.IdInvalidException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserResolver userResolver;
    private final PasswordEncoder passwordEncoder;
    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final com.homely.rental.auth.repository.RoleRepository roleRepository;

    // ===== AUTH01: Register =====

    @Transactional
    public UserResponse registerUser(RegisterRequest dto) throws IdInvalidException {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IdInvalidException("Email already exists");
        }

        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setFullName(dto.getFullName());
        user.setStatus(UserStatus.ACTIVE);

        // Assign default ROLE_TENANT
        com.homely.rental.auth.entity.Role tenantRole = roleRepository.findByName(com.homely.rental.auth.entity.RoleName.ROLE_TENANT)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        user.getRoles().add(tenantRole);

        User saved = this.userRepository.save(user);

        // Create default notification preferences
        NotificationPreference prefs = new NotificationPreference();
        prefs.setUser(saved);
        notificationPreferenceRepository.save(prefs);

        return convertToUserResponse(saved);
    }

    // ===== User lookup =====

    public User handleGetUserByUsername(String username) {
        return this.userRepository.findByEmail(username);
    }

    /**
     * Save a user entity (used for email verification, profile updates, etc.)
     */
    @Transactional
    public User saveUser(User user) {
        return this.userRepository.save(user);
    }

    // ===== AUTH07: Get profile =====

    public UserResponse fetchCurrentUserProfile() throws IdInvalidException {
        User user = getCurrentActiveUser();
        return convertToUserResponse(user);
    }

    // ===== AUTH08: Update profile =====

    @Transactional
    public UserResponse updateProfile(ProfileUpdateRequest dto) throws IdInvalidException {
        User user = getCurrentActiveUser();

        if (dto.getFullName() != null) {
            user.setFullName(dto.getFullName());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
        if (dto.getAvatarUrl() != null) {
            user.setAvatarUrl(dto.getAvatarUrl());
        }

        return convertToUserResponse(this.userRepository.save(user));
    }

    // ===== AUTH10: Device token =====

    @Transactional
    public void registerDeviceToken(DeviceTokenRequest dto) throws IdInvalidException {
        User user = getCurrentActiveUser();
        deviceTokenRepository.deactivatePreviousOwner(user.getId(), dto.getInstallationId(), dto.getToken());

        Optional<DeviceToken> existingOpt = deviceTokenRepository
                .findByUserIdAndInstallationId(user.getId(), dto.getInstallationId());

        if (existingOpt.isPresent()) {
            DeviceToken existing = existingOpt.get();
            existing.setToken(dto.getToken());
            existing.setDeviceName(dto.getDeviceName());
            existing.setActive(true);
            existing.setUpdatedAt(Instant.now());
            deviceTokenRepository.save(existing);
        } else {
            DeviceToken newToken = new DeviceToken();
            newToken.setUser(user);
            newToken.setToken(dto.getToken());
            newToken.setPlatform(DeviceToken.Platform.ANDROID);
            newToken.setDeviceName(dto.getDeviceName());
            newToken.setInstallationId(dto.getInstallationId());
            deviceTokenRepository.save(newToken);
        }
    }

    // ===== AUTH11: Notification preferences =====
    @Transactional
    public void unregisterDevice(String installationId) throws IdInvalidException {
        deviceTokenRepository.deactivateInstallation(getCurrentActiveUser().getId(), installationId);
    }

    public NotificationPreference getNotificationPreferences() throws IdInvalidException {
        User user = getCurrentActiveUser();
        return notificationPreferenceRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    NotificationPreference prefs = new NotificationPreference();
                    prefs.setUser(user);
                    return notificationPreferenceRepository.save(prefs);
                });
    }

    @Transactional
    public NotificationPreference updateNotificationPreferences(NotificationPreferencesRequest dto) throws IdInvalidException {
        User user = getCurrentActiveUser();
        NotificationPreference prefs = notificationPreferenceRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    NotificationPreference p = new NotificationPreference();
                    p.setUser(user);
                    return notificationPreferenceRepository.save(p);
                });

        // Optimistic locking check
        if (dto.getExpectedVersion() != null && prefs.getVersion() != dto.getExpectedVersion()) {
            throw new ConflictException("VERSION_MISMATCH",
                    "Notification preferences have been modified. Expected version: " + dto.getExpectedVersion()
                            + ", actual: " + prefs.getVersion());
        }

        if (dto.getTransactionPush() != null) prefs.setTransactionPush(dto.getTransactionPush());
        if (dto.getTransactionEmail() != null) prefs.setTransactionEmail(dto.getTransactionEmail());
        if (dto.getChatPush() != null) prefs.setChatPush(dto.getChatPush());
        if (dto.getRecommendationPush() != null) prefs.setRecommendationPush(dto.getRecommendationPush());
        prefs.setUpdatedAt(Instant.now());

        return notificationPreferenceRepository.save(prefs);
    }

    // ===== Enable host role =====

    @Transactional
    public UserResponse enableHostRole() throws IdInvalidException {
        User user = getCurrentVerifiedUser();
        
        com.homely.rental.auth.entity.Role hostRole = roleRepository.findByName(com.homely.rental.auth.entity.RoleName.ROLE_HOST)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        user.getRoles().add(hostRole);

        return convertToUserResponse(this.userRepository.save(user));
    }

    // ===== Helpers =====

    private User getCurrentActiveUser() throws IdInvalidException {
        return userResolver.requireCurrent();
    }

    public User getCurrentVerifiedUser() throws IdInvalidException {
        User user = getCurrentActiveUser();
        if (!user.isEmailVerified()) {
            throw new IdInvalidException("Email must be verified before performing this action");
        }
        return user;
    }

    public static LoginResponse.UserLogin convertToUserLogin(User user) {
        LoginResponse.UserLogin userLogin = new LoginResponse.UserLogin();
        userLogin.setId(user.getId());
        userLogin.setEmail(user.getEmail());
        userLogin.setFullName(user.getFullName());
        userLogin.setAvatarUrl(user.getAvatarUrl());
        userLogin.setHost(user.isHost());
        userLogin.setRoles(user.getRoles().stream().map(role -> role.getName().name()).sorted().toList());
        userLogin.setEmailVerified(user.isEmailVerified());
        userLogin.setStatus(user.getStatus().name());
        return userLogin;
    }

    public static UserResponse convertToUserResponse(User user) {
        UserResponse res = new UserResponse();
        res.setId(user.getId());
        res.setEmail(user.getEmail());
        res.setFullName(user.getFullName());
        res.setPhone(user.getPhone());
        res.setAvatarUrl(user.getAvatarUrl());
        res.setHost(user.isHost());
        res.setEmailVerified(user.isEmailVerified());
        res.setStatus(user.getStatus().name());
        res.setCreatedAt(user.getCreatedAt());
        return res;
    }
}
