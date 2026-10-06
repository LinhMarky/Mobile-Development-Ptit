package com.example.roomly.data.repository;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.example.roomly.data.model.AppMode;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;

/**
 * Giữ chế độ giao diện trong bộ nhớ khi ứng dụng đang chạy.
 * Chuyển chế độ không thay đổi vai trò hoặc token đăng nhập.
 */
public final class AppModeRepository {

    private static final AppModeRepository INSTANCE =
            new AppModeRepository();

    private final MediatorLiveData<AppMode> currentMode =
            new MediatorLiveData<>(AppMode.TENANT);

    // Theo dõi tài khoản để không giữ chế độ của người dùng trước.
    @Nullable
    private String modeUserId;

    /**
     * Theo dõi phiên khi có màn hình đang quan sát chế độ.
     * Điều chỉnh chế độ khi đăng xuất, đổi tài khoản hoặc đổi vai trò.
     */
    private AppModeRepository() {
        currentMode.addSource(
                SessionRepository.getInstance().getSessionState(),
                this::syncWithSession
        );
    }

    /**
     * Trả về nơi quản lý chế độ dùng chung của ứng dụng.
     */
    public static AppModeRepository getInstance() {
        return INSTANCE;
    }

    /**
     * Cho phép ViewModel và màn hình quan sát chế độ hiện tại.
     */
    public LiveData<AppMode> getMode() {
        return currentMode;
    }

    /**
     * Đọc chế độ sau khi đối chiếu với phiên mới nhất.
     */
    @MainThread
    public AppMode getCurrentMode() {
        syncWithSession(
                SessionRepository.getInstance().getCurrentSession()
        );

        AppMode mode = currentMode.getValue();

        return mode == null ? AppMode.TENANT : mode;
    }

    /**
     * Đổi chế độ khi tài khoản có vai trò tương ứng.
     * Trả về false nếu là khách hoặc không có quyền dùng chế độ.
     */
    @MainThread
    public boolean selectMode(@Nullable AppMode requestedMode) {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        syncWithSession(session);

        if (!canUseMode(session, requestedMode)) {
            return false;
        }

        if (currentMode.getValue() != requestedMode) {
            currentMode.setValue(requestedMode);
        }

        return true;
    }

    /**
     * Kiểm tra vai trò cho chế độ được yêu cầu.
     * Không yêu cầu xác minh email chỉ để chuyển giao diện.
     */
    private boolean canUseMode(
            SessionState session,
            @Nullable AppMode mode
    ) {
        if (!session.isLoggedIn() || mode == null) {
            return false;
        }

        if (mode == AppMode.TENANT) {
            return session.hasRole(UserRole.TENANT);
        }

        return session.hasRole(UserRole.HOST);
    }

    /**
     * Đặt lại chế độ khi đổi tài khoản.
     * Giữ lựa chọn hiện tại nếu tài khoản vẫn có vai trò phù hợp.
     */
    @MainThread
    private void syncWithSession(@Nullable SessionState state) {
        SessionState session = state == null
                ? SessionState.guest()
                : state;

        String nextUserId = session.getUserId();

        boolean accountChanged = modeUserId == null
                ? nextUserId != null
                : !modeUserId.equals(nextUserId);

        modeUserId = nextUserId;

        AppMode previousMode = currentMode.getValue();
        AppMode nextMode = previousMode;

        if (!session.isLoggedIn()) {
            nextMode = AppMode.TENANT;

        } else if (accountChanged
                || !canUseMode(session, previousMode)) {
            // Ưu tiên Người thuê khi tài khoản có cả hai vai trò.
            if (session.hasRole(UserRole.TENANT)) {
                nextMode = AppMode.TENANT;

            } else if (session.hasRole(UserRole.HOST)) {
                nextMode = AppMode.HOST;

            } else {
                // Giá trị mặc định không cấp quyền TENANT.
                // Tài khoản chỉ có ADMIN sẽ dùng lối vào quản trị riêng.
                nextMode = AppMode.TENANT;
            }
        }

        if (previousMode != nextMode) {
            currentMode.setValue(nextMode);
        }
    }
}