package com.example.roomly.data.model;

import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Thông tin phiên dùng để hiển thị giao diện và kiểm tra quyền ở frontend.
 * Backend vẫn quyết định quyền thực tế của mỗi yêu cầu.
 * Lớp này không chứa mật khẩu hoặc token.
 */
public final class SessionState {

    @Nullable
    private final String userId;

    private final String fullName;
    private final String email;
    private final boolean emailVerified;
    private final Set<UserRole> roles;

    /**
     * Khởi tạo phiên và sao chép các vai trò để dữ liệu không bị sửa ngoài lớp.
     */
    private SessionState(
            @Nullable String userId,
            String fullName,
            String email,
            boolean emailVerified,
            Set<UserRole> roles
    ) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.emailVerified = emailVerified;

        EnumSet<UserRole> roleCopy =
                EnumSet.noneOf(UserRole.class);

        roleCopy.addAll(roles);

        this.roles = Collections.unmodifiableSet(roleCopy);
    }

    /**
     * Tạo trạng thái khách chưa đăng nhập, không có vai trò tài khoản.
     */
    public static SessionState guest() {
        return new SessionState(
                null,
                "",
                "",
                false,
                Collections.emptySet()
        );
    }

    /**
     * Tạo trạng thái tài khoản từ dữ liệu đã được backend xác thực.
     * Không gọi hàm này chỉ vì form đăng nhập hợp lệ.
     */
    public static SessionState authenticated(
            String userId,
            String fullName,
            String email,
            boolean emailVerified,
            Set<UserRole> roles
    ) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Phiên đăng nhập cần có mã người dùng."
            );
        }

        if (roles == null || roles.contains(null)) {
            throw new IllegalArgumentException(
                    "Danh sách vai trò không hợp lệ."
            );
        }

        return new SessionState(
                userId,
                fullName == null ? "" : fullName,
                email == null ? "" : email,
                emailVerified,
                roles
        );
    }

    /**
     * Cho biết đây có phải phiên tài khoản đã đăng nhập hay không.
     */
    public boolean isLoggedIn() {
        return userId != null;
    }

    /**
     * Cho biết người dùng đang ở trạng thái khách.
     */
    public boolean isGuest() {
        return !isLoggedIn();
    }

    /**
     * Trả về mã người dùng để đối chiếu quyền sở hữu dữ liệu.
     */
    @Nullable
    public String getUserId() {
        return userId;
    }

    /**
     * Trả về họ tên của tài khoản.
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Trả về email của tài khoản.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Cho biết tài khoản đã được backend xác nhận email hay chưa.
     */
    public boolean isEmailVerified() {
        return isLoggedIn() && emailVerified;
    }

    /**
     * Trả về tập hợp vai trò chỉ đọc.
     */
    public Set<UserRole> getRoles() {
        return roles;
    }

    /**
     * Kiểm tra tài khoản có vai trò được yêu cầu hay không.
     */
    public boolean hasRole(UserRole role) {
        return isLoggedIn()
                && role != null
                && roles.contains(role);
    }
}