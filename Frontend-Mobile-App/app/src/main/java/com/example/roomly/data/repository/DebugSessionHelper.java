package com.example.roomly.data.repository;

import androidx.annotation.MainThread;

import com.example.roomly.BuildConfig;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;

import java.util.EnumSet;

/**
 * Tạo phiên mẫu để kiểm tra giao diện khi chưa có backend.
 * Chỉ cho phép sử dụng trong bản debug.
 */
public final class DebugSessionHelper {

    /**
     * Ngăn tạo đối tượng vì lớp chỉ chứa hàm dùng chung.
     */
    private DebugSessionHelper() {
    }

    /**
     * Bật tài khoản mẫu có hai vai trò Người thuê và Chủ trọ.
     * Không tạo token và không thực hiện đăng nhập với backend.
     */
    @MainThread
    public static boolean enableDemoSession() {
        if (!BuildConfig.DEBUG) {
            return false;
        }

        // Không thay thế một phiên tài khoản đang đăng nhập.
        if (SessionRepository.getInstance()
                .getCurrentSession()
                .isLoggedIn()) {
            return false;
        }

        SessionState demoSession = SessionState.authenticated(
                "debug-demo-user",
                "Tài khoản thử giao diện",
                "demo@example.com",
                true,
                EnumSet.of(
                        UserRole.TENANT,
                        UserRole.HOST
                )
        );

        SessionRepository.getInstance()
                .updateAuthenticatedSession(demoSession);

        return true;
    }

    /**
     * Xóa phiên nếu đây đúng là tài khoản mẫu do helper tạo.
     * Không xóa phiên của tài khoản khác.
     */
    @MainThread
    public static boolean disableDemoSession() {
        if (!BuildConfig.DEBUG) {
            return false;
        }

        String userId = SessionRepository.getInstance()
                .getCurrentSession()
                .getUserId();

        if (!"debug-demo-user".equals(userId)) {
            return false;
        }

        SessionRepository.getInstance().clearLocalSession();

        return true;
    }
}