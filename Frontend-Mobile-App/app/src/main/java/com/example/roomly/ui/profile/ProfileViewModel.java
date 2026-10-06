package com.example.roomly.ui.profile;

import androidx.annotation.MainThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.roomly.data.model.AppMode;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.repository.AppModeRepository;
import com.example.roomly.data.repository.SessionRepository;

/**
 * Cung cấp trạng thái phiên và chế độ giao diện cho trang Cá nhân.
 * Không tự cấp vai trò hoặc thay đổi thông tin đăng nhập.
 */
public class ProfileViewModel extends ViewModel {

    private final SessionRepository sessionRepository =
            SessionRepository.getInstance();

    private final AppModeRepository modeRepository =
            AppModeRepository.getInstance();

    /**
     * Cung cấp phiên để giao diện quan sát thông tin tài khoản và quyền.
     */
    public LiveData<SessionState> getSessionState() {
        return sessionRepository.getSessionState();
    }

    /**
     * Cung cấp chế độ để giao diện cập nhật nút đang chọn.
     */
    public LiveData<AppMode> getMode() {
        return modeRepository.getMode();
    }

    /**
     * Đọc chế độ hiện tại sau khi đối chiếu với phiên đăng nhập.
     */
    @MainThread
    public AppMode getCurrentMode() {
        return modeRepository.getCurrentMode();
    }

    /**
     * Yêu cầu đổi chế độ và trả về kết quả kiểm tra vai trò.
     * Chỉ đổi giao diện, không thay đổi quyền của tài khoản.
     */
    @MainThread
    public boolean selectMode(AppMode mode) {
        return modeRepository.selectMode(mode);
    }
}