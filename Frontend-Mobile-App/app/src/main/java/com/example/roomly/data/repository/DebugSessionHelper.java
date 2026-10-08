package com.example.roomly.data.repository;

import androidx.annotation.MainThread;

import com.example.roomly.BuildConfig;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;

import java.util.EnumSet;

/**
 * Tạo phiên thử trong bản debug, không tạo token đăng nhập.
 * Tài khoản người thuê riêng dùng để thử phòng của tài khoản chủ trọ.
 */
public final class DebugSessionHelper {

    private static final String DEMO_USER_ID = "debug-demo-user";
    private static final String DEMO_ADMIN_ID = "debug-demo-admin";
    private static final String DEMO_TENANT_ID = "debug-demo-tenant";

    /** Ngăn tạo đối tượng vì lớp chỉ chứa hàm dùng chung. */
    private DebugSessionHelper() {
    }

    /** Bật tài khoản thử có hai quyền Người thuê và Chủ trọ. */
    @MainThread
    public static boolean enableDemoSession() {
        return enableSession(
                DEMO_USER_ID,
                "Tài khoản thử giao diện",
                "demo@example.com",
                EnumSet.of(UserRole.TENANT, UserRole.HOST)
        );
    }

    /** Bật tài khoản người thuê riêng, khác ID tài khoản chủ trọ thử. */
    @MainThread
    public static boolean enableDemoTenantSession() {
        return enableSession(
                DEMO_TENANT_ID,
                "Người thuê thử giao diện",
                "tenant-demo@example.com",
                EnumSet.of(UserRole.TENANT)
        );
    }

    /** Bật tài khoản thử chỉ có quyền quản trị. */
    @MainThread
    public static boolean enableDemoAdminSession() {
        return enableSession(
                DEMO_ADMIN_ID,
                "Admin thử giao diện",
                "admin-demo@example.com",
                EnumSet.of(UserRole.ADMIN)
        );
    }

    /** Tạo phiên mẫu khi đang là khách và đang chạy bản debug. */
    @MainThread
    private static boolean enableSession(
            String userId,
            String fullName,
            String email,
            EnumSet<UserRole> roles
    ) {
        if (!canEnableDemoSession()) {
            return false;
        }

        SessionState session = SessionState.authenticated(
                userId,
                fullName,
                email,
                true,
                roles
        );

        SessionRepository.getInstance()
                .updateAuthenticatedSession(session);

        return true;
    }

    /** Không cho phiên thử thay thế tài khoản đang đăng nhập. */
    private static boolean canEnableDemoSession() {
        return BuildConfig.DEBUG
                && !SessionRepository.getInstance()
                .getCurrentSession()
                .isLoggedIn();
    }

    /** Nhận biết ba tài khoản thử được tạo bởi helper này. */
    @MainThread
    public static boolean isDemoSession() {
        if (!BuildConfig.DEBUG) {
            return false;
        }

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()) {
            return false;
        }

        String userId = session.getUserId();

        return DEMO_USER_ID.equals(userId)
                || DEMO_ADMIN_ID.equals(userId)
                || DEMO_TENANT_ID.equals(userId);
    }

    /** Chỉ xóa phiên thử do helper tạo, không xóa tài khoản khác. */
    @MainThread
    public static boolean disableDemoSession() {
        if (!isDemoSession()) {
            return false;
        }

        SessionRepository.getInstance().clearLocalSession();
        return true;
    }
}