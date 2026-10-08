package com.example.roomly.data.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Thông tin tài khoản dùng cho giao diện quản trị.
 * Trạng thái trong lớp này phục vụ dữ liệu mẫu,
 * chưa phải cấu trúc phản hồi chính thức của API.
 */
public final class AdminAccount {

    public enum Status {
        ACTIVE,
        SUSPENDED
    }

    private final String id;
    private final String fullName;
    private final String email;
    private final Set<UserRole> roles;
    private final boolean emailVerified;
    private final Status status;
    private final String suspensionReason;

    /**
     * Khởi tạo tài khoản và sao chép danh sách quyền
     * để dữ liệu bên ngoài không thay đổi quyền trong đối tượng.
     */
    public AdminAccount(
            String id,
            String fullName,
            String email,
            Set<UserRole> roles,
            boolean emailVerified,
            Status status,
            String suspensionReason
    ) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Thiếu mã tài khoản.");
        }

        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("Tài khoản phải có vai trò.");
        }

        if (status == null) {
            throw new IllegalArgumentException("Thiếu trạng thái tài khoản.");
        }

        this.id = id;
        this.fullName = fullName == null ? "" : fullName;
        this.email = email == null ? "" : email;
        this.roles = Collections.unmodifiableSet(
                EnumSet.copyOf(roles)
        );
        this.emailVerified = emailVerified;
        this.status = status;
        this.suspensionReason = suspensionReason == null
                ? ""
                : suspensionReason;
    }

    /** Trả về mã tài khoản. */
    public String getId() {
        return id;
    }

    /** Trả về tên người dùng. */
    public String getFullName() {
        return fullName;
    }

    /** Trả về email tài khoản. */
    public String getEmail() {
        return email;
    }

    /** Trả về danh sách quyền chỉ được đọc. */
    public Set<UserRole> getRoles() {
        return roles;
    }

    /** Kiểm tra tài khoản có vai trò được yêu cầu hay không. */
    public boolean hasRole(UserRole role) {
        return roles.contains(role);
    }

    /** Cho biết email đã được xác minh hay chưa. */
    public boolean isEmailVerified() {
        return emailVerified;
    }

    /** Trả về trạng thái tài khoản. */
    public Status getStatus() {
        return status;
    }

    /** Trả về lý do khóa tài khoản. */
    public String getSuspensionReason() {
        return suspensionReason;
    }

    /** Chuyển trạng thái thành nội dung hiển thị trên giao diện. */
    public String getStatusLabel() {
        return status == Status.ACTIVE
                ? "Đang hoạt động"
                : "Đã khóa";
    }

    /** Ghép các vai trò thành nội dung dễ đọc. */
    public String getRolesLabel() {
        StringBuilder label = new StringBuilder();

        for (UserRole role : roles) {
            if (label.length() > 0) {
                label.append(" • ");
            }

            if (role == UserRole.TENANT) {
                label.append("Người thuê");
            } else if (role == UserRole.HOST) {
                label.append("Chủ trọ");
            } else if (role == UserRole.ADMIN) {
                label.append("Admin");
            }
        }

        return label.toString();
    }

    /**
     * Tạo bản sao đã khóa, giữ nguyên thông tin và quyền tài khoản.
     */
    public AdminAccount withSuspension(String reason) {
        return new AdminAccount(
                id,
                fullName,
                email,
                roles,
                emailVerified,
                Status.SUSPENDED,
                reason
        );
    }
}