package com.homely.rental.auth.security;

import com.homely.rental.auth.constant.UserStatus;
import com.homely.rental.auth.entity.User;
import com.homely.rental.auth.repository.UserRepository;
import com.homely.rental.auth.repository.AccountDeletionRequestRepository;
import com.homely.rental.auth.entity.AccountDeletionRequest.DeletionStatus;
import com.homely.rental.auth.service.AccountLifecycleGuard;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountAccessService {
    private final UserRepository users;
    private final AccountDeletionRequestRepository deletions;
    private final EntityManager entities;
    private final AccountLifecycleGuard lifecycle;

    public User requireActive(String email) {
        User user = requireActiveIgnoringDeletion(email);
        if (deletions.existsByUserIdAndStatusIn(user.getId(), List.of(DeletionStatus.PENDING, DeletionStatus.PROCESSING))) {
            throw new AccountAccessException(403, "ACCOUNT_DELETION_PENDING", "Account deletion is pending");
        }
        return user;
    }

    /** Pending accounts may inspect their own deletion request and log out. */
    public User requireActiveIgnoringDeletion(String email) {
        boolean mutation = TransactionSynchronizationManager.isActualTransactionActive()
                && !TransactionSynchronizationManager.isCurrentTransactionReadOnly();
        User user = email == null ? null : users.findByEmail(email);
        if (user == null) throw new AccountAccessException(401, "AUTHENTICATION_REQUIRED", "Authentication is required");
        if (mutation) {
            // The separate lifecycle row avoids conflicting with other users' FK inserts.
            lifecycle.lock(user.getId());
            entities.refresh(user);
        }
        // Recheck inside the service transaction: deletion can commit after the HTTP filter ran.
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwt && email.equals(jwt.getName())
                && !AccessTokenIdentity.matches(jwt.getToken(), user.getId())) {
            throw new AccountAccessException(401, "INVALID_ACCESS_TOKEN", "A valid access token is required");
        }
        if (user.getStatus() != UserStatus.ACTIVE || user.isSuspended()) {
            throw new AccountAccessException(403, "ACCOUNT_INACTIVE", "Your account is not active");
        }
        return user;
    }

    public User requireCurrent() {
        return requireActive(SecurityUtils.getCurrentUserLogin().orElse(null));
    }

    public User requireAdmin(String email) {
        User user = requireActive(email);
        if (!user.isAdmin()) throw new AccountAccessException(403, "FORBIDDEN", "Admin role is required");
        return user;
    }

    public User requireAdmin() {
        return requireAdmin(SecurityUtils.getCurrentUserLogin().orElse(null));
    }
}
