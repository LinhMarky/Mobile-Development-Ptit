package com.example.roomly.data.repository;

import androidx.annotation.MainThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.roomly.data.model.SessionState;

/**
 * Giữ trạng thái phiên dùng chung trong bộ nhớ.
 * Chưa lưu token hoặc khôi phục phiên từ thiết bị.
 */
public final class SessionRepository {

    private static final SessionRepository INSTANCE =
            new SessionRepository();

    private final MutableLiveData<SessionState> sessionState =
            new MutableLiveData<>(SessionState.guest());

    /**
     * Chỉ tạo một repository phiên dùng chung.
     */
    private SessionRepository() {
    }

    /**
     * Trả về repository phiên của ứng dụng.
     */
    public static SessionRepository getInstance() {
        return INSTANCE;
    }

    /**
     * Cho phép các ViewModel quan sát thay đổi phiên.
     * Bên ngoài không cập nhật LiveData trực tiếp.
     */
    public LiveData<SessionState> getSessionState() {
        return sessionState;
    }

    /**
     * Trả về trạng thái phiên hiện tại.
     */
    public SessionState getCurrentSession() {
        SessionState current = sessionState.getValue();

        return current == null
                ? SessionState.guest()
                : current;
    }

    /**
     * Cập nhật phiên sau khi backend xác thực tài khoản.
     * Gọi trên luồng giao diện, không dùng kết quả kiểm tra form để cấp quyền.
     */
    @MainThread
    public void updateAuthenticatedSession(SessionState session) {
        if (session == null || !session.isLoggedIn()) {
            throw new IllegalArgumentException(
                    "Cần trạng thái tài khoản đã đăng nhập."
            );
        }

        sessionState.setValue(session);
    }

    /**
     * Xóa trạng thái tài khoản trong bộ nhớ và chuyển về Guest.
     * Khi nối backend, luồng đăng xuất còn phải xử lý token,
     * kết nối chat và thông báo theo contract.
     */
    @MainThread
    public void clearLocalSession() {
        sessionState.setValue(SessionState.guest());
    }
}