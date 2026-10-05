package com.homely.rental.auth.security;

import com.homely.rental.auth.constant.UserStatus;
import com.homely.rental.auth.entity.User;
import com.homely.rental.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountAccessService {
    private final UserRepository users;

    public User requireActive(String email) {
        User user = email == null ? null : users.findByEmail(email);
        if (user == null) throw new AccountAccessException(401, "AUTHENTICATION_REQUIRED", "Authentication is required");
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
