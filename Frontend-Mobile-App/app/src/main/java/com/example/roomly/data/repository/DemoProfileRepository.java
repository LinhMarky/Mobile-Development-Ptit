package com.example.roomly.data.repository;

import com.example.roomly.data.model.SessionState;

import java.util.HashMap;
import java.util.Map;

/**
 * Lưu hồ sơ trong bộ nhớ để kiểm thử giao diện.
 * Chỉ cho phép cập nhật tài khoản demo, không thay cho API hồ sơ.
 */
public final class DemoProfileRepository {

    private static final DemoProfileRepository INSTANCE =
            new DemoProfileRepository();

    private final Map<String, String> phones = new HashMap<>();

    /** Ngăn tạo repository ngoài lớp. */
    private DemoProfileRepository() {
    }

    /** Trả về repository mẫu dùng chung. */
    public static DemoProfileRepository getInstance() {
        return INSTANCE;
    }

    /** Trả về tên của tài khoản đang đăng nhập. */
    public String getFullName() {
        return SessionRepository.getInstance()
                .getCurrentSession()
                .getFullName();
    }

    /** Trả về email của tài khoản đang đăng nhập. */
    public String getEmail() {
        return SessionRepository.getInstance()
                .getCurrentSession()
                .getEmail();
    }

    /** Trả về số điện thoại riêng của tài khoản thử hiện tại. */
    public String getPhone() {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()
                || !DebugSessionHelper.isDemoSession()) {
            return "";
        }

        String phone = phones.get(session.getUserId());
        return phone == null ? "" : phone;
    }

    /** Giữ cách gọi cũ và chuyển sang hàm kiểm tra chủ sở hữu. */
    public void updateProfile(String fullName, String phone) {
        String userId = SessionRepository.getInstance()
                .getCurrentSession()
                .getUserId();

        updateProfile(userId, fullName, phone);
    }

    /** Kiểm tra phiên trước khi cập nhật hồ sơ mẫu. */
    public void updateProfile(
            String expectedUserId,
            String fullName,
            String phone
    ) {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()
                || expectedUserId == null
                || !expectedUserId.equals(session.getUserId())) {
            throw new IllegalStateException(
                    "Tài khoản đã thay đổi. Hãy mở lại biểu mẫu."
            );
        }

        if (!session.isEmailVerified()) {
            throw new IllegalStateException(
                    "Bạn cần xác minh email trước khi cập nhật."
            );
        }

        if (!DebugSessionHelper.isDemoSession()) {
            throw new IllegalStateException(
                    "Chưa kết nối API cập nhật hồ sơ."
            );
        }

        String cleanName = fullName == null ? "" : fullName.trim();
        String cleanPhone = phone == null ? "" : phone.trim();

        if (cleanName.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập họ tên.");
        }

        phones.put(expectedUserId, cleanPhone);

        SessionRepository.getInstance().updateAuthenticatedSession(
                SessionState.authenticated(
                        session.getUserId(),
                        cleanName,
                        session.getEmail(),
                        session.isEmailVerified(),
                        session.getRoles()
                )
        );
    }
}