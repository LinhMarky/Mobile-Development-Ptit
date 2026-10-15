package com.homely.student14.service;

import com.homely.student14.dto.request.admin.UpdateAccountStatusDTO;
import com.homely.student14.dto.response.auth.UserAccountResponse;
import com.homely.student14.repository.AdminRepository;
import com.homely.student14.repository.DatabaseRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final AdminRepository adminRepository;
    private final AuthService authService;
    private final CatalogService catalogService;

    public List<Map<String, Object>> listings(Jwt jwt) {
        authService.role(jwt, "admin");
        return adminRepository.findAllListings().stream().map(catalogService::decorate).toList();
    }

    public List<UserAccountResponse> users(Jwt jwt) {
        authService.role(jwt, "admin");
        return adminRepository.findAllUsers().stream().map(authService::account).toList();
    }

    public Map<String, Object> summary(Jwt jwt) {
        authService.role(jwt, "admin");
        return adminRepository.getSummary();
    }

    @Transactional
    public void updateUser(long userId, Jwt jwt, String action, UpdateAccountStatusDTO request) {
        Map<String, Object> admin = authService.role(jwt, "admin");
        if (DatabaseRepository.id(admin.get("id")) == userId) {
            throw DatabaseRepository.fail(400, "Không tự khóa tài khoản.");
        }
        String reason = request.getReason() == null ? "" : request.getReason().trim();
        boolean suspend = "suspend".equals(action);
        if (suspend && reason.isEmpty()) {
            throw DatabaseRepository.fail(400, "Vui lòng nhập lý do khóa tài khoản.");
        }
        adminRepository.updateAccountStatus(userId, suspend ? "SUSPENDED" : "ACTIVE", reason);
        if (suspend) {
            adminRepository.revokeSessions(userId);
        }
    }
}
