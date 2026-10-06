package com.example.roomly.data.repository;

import androidx.annotation.Nullable;

import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;

/**
 * Kiểm tra trạng thái đăng nhập và quyền trước khi thực hiện thao tác.
 * Backend vẫn phải kiểm tra quyền khi nhận yêu cầu từ ứng dụng.
 */
public final class SessionAccess {

    /**
     * Các kết quả kiểm tra để màn hình chọn cách xử lý phù hợp.
     */
    public enum Result {
        ALLOWED,
        LOGIN_REQUIRED,
        EMAIL_VERIFICATION_REQUIRED,
        ROLE_REQUIRED
    }

    /**
     * Ngăn tạo đối tượng vì lớp này chỉ chứa hàm dùng chung.
     */
    private SessionAccess() {
    }

    /**
     * Kiểm tra thao tác chỉ yêu cầu người dùng đăng nhập.
     */
    public static Result requireLogin() {
        SessionState session = SessionRepository
                .getInstance()
                .getCurrentSession();

        return session.isLoggedIn()
                ? Result.ALLOWED
                : Result.LOGIN_REQUIRED;
    }

    /**
     * Kiểm tra thao tác yêu cầu đăng nhập và xác minh email.
     */
    public static Result requireVerifiedEmail() {
        SessionState session = SessionRepository
                .getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()) {
            return Result.LOGIN_REQUIRED;
        }

        if (!session.isEmailVerified()) {
            return Result.EMAIL_VERIFICATION_REQUIRED;
        }

        return Result.ALLOWED;
    }

    /**
     * Kiểm tra vai trò và yêu cầu xác minh email nếu thao tác cần.
     * Vai trò được đọc từ phiên đăng nhập, không lấy từ chế độ giao diện.
     */
    public static Result requireRole(
            @Nullable UserRole requiredRole,
            boolean requiresVerifiedEmail
    ) {
        SessionState session = SessionRepository
                .getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()) {
            return Result.LOGIN_REQUIRED;
        }

        if (requiredRole == null || !session.hasRole(requiredRole)) {
            return Result.ROLE_REQUIRED;
        }

        if (requiresVerifiedEmail && !session.isEmailVerified()) {
            return Result.EMAIL_VERIFICATION_REQUIRED;
        }

        return Result.ALLOWED;
    }
}