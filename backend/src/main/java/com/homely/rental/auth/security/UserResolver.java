package com.homely.rental.auth.security;

import com.homely.rental.auth.entity.User;
import com.homely.rental.common.exception.IdInvalidException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Central component for resolving the signed-in user.
 * Replaces all duplicated getCurrentUser/getVerifiedUser/getVerifiedHost
 * private methods scattered across services.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserResolver {
    private final AccountAccessService accounts;

    /** Get current authenticated user, or throw if not authenticated / suspended. */
    public User requireCurrent() {
        return accounts.requireCurrent();
    }

    /** Get current user only if email verified, or throw. */
    public User requireVerified() {
        User user = requireCurrent();
        if (!user.isEmailVerified()) {
            throw new AccountAccessException(403, "EMAIL_NOT_VERIFIED", "Email must be verified");
        }
        return user;
    }

    /** Get current user only if verified + has HOST role, or throw. */
    public User requireHost() {
        User user = requireVerified();
        if (!user.isHost()) {
            throw new AccountAccessException(403, "HOST_REQUIRED", "User does not have HOST role");
        }
        return user;
    }

    /** Get current user only if has ADMIN role, or throw. */
    public User requireAdmin() {
        return accounts.requireAdmin();
    }

    /** Public reads may be anonymous; an authenticated but restricted account is still rejected. */
    public Optional<User> currentIfAuthenticated() {
        return SecurityUtils.getCurrentUserLogin().isEmpty()
                ? Optional.empty() : Optional.of(requireCurrent());
    }
}

