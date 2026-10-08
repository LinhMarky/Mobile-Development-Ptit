package com.example.roomly.data.repository;

import androidx.annotation.MainThread;

import com.example.roomly.BuildConfig;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;

import java.util.EnumSet;

/**
 * Tạo phiên mẫu để kiểm tra giao diện khi chưa có backend.
 * Chỉ hoạt động trong bản debug, không tạo token đăng nhập.
 */
public final class DebugSessionHelper {

    private static final String DEMO_USER_ID = "debug-demo-user";
    private static final String DEMO_ADMIN_ID = "debug-demo-admin";

    /**
     * Ngăn tạo đối tượng vì lớp chỉ chứa các hàm dùng chung.
     */
    private DebugSessionHelper() {
    }

    /**
     * Bật tài khoản mẫu có hai vai trò Người thuê và Chủ trọ.
     * Không thay thế tài khoản đang đăng nhập.
     */
    @MainThread
    public static boolean enableDemoSession() {
        if (!canEnableDemoSession()) {
            return false;
        }

        SessionState demoSession = SessionState.authenticated(
                DEMO_USER_ID,
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
     * Bật tài khoản mẫu chỉ có quyền ADMIN để kiểm tra trang quản trị.
     * Không cấp quyền admin cho tài khoản khác đang đăng nhập.
     */
    @MainThread
    public static boolean enableDemoAdminSession() {
        if (!canEnableDemoSession()) {
            return false;
        }

        SessionState demoSession = SessionState.authenticated(
                DEMO_ADMIN_ID,
                "Admin thử giao diện",
                "admin-demo@example.com",
                true,
                EnumSet.of(UserRole.ADMIN)
        );

        SessionRepository.getInstance()
                .updateAuthenticatedSession(demoSession);

        return true;
    }

    /**
     * Chỉ cho tạo phiên mẫu trong bản debug khi đang ở trạng thái khách.
     */
    private static boolean canEnableDemoSession() {
        return BuildConfig.DEBUG
                && !SessionRepository.getInstance()
                .getCurrentSession()
                .isLoggedIn();
    }

    /**
     * Nhận biết phiên mẫu Người thuê/Chủ trọ hoặc Admin do helper tạo.
     */
    @MainThread
    public static boolean isDemoSession() {
        if (!BuildConfig.DEBUG) {
            return false;
        }

        SessionState session =
                SessionRepository.getInstance().getCurrentSession();

        if (!session.isLoggedIn()) {
            return false;
        }

        String userId = session.getUserId();

        return DEMO_USER_ID.equals(userId)
                || DEMO_ADMIN_ID.equals(userId);
    }

    /**
     * Xóa phiên nếu đúng là một trong hai tài khoản mẫu của helper.
     * Không xóa phiên của tài khoản khác.
     */
    @MainThread
    public static boolean disableDemoSession() {
        if (!isDemoSession()) {
            return false;
        }

        SessionRepository.getInstance().clearLocalSession();

        return true;
    }
}