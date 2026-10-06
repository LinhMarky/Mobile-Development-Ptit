package com.example.roomly.ui.auth;

import android.util.Patterns;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * Quản lý trạng thái giao diện xác minh email.
 * Trạng thái tài khoản thực tế sẽ được lấy từ backend.
 */
public class VerifyEmailViewModel extends ViewModel {

    private final MutableLiveData<VerifyEmailUiState> uiState =
            new MutableLiveData<>(VerifyEmailUiState.initial());

    /**
     * Cho phép Fragment quan sát trạng thái giao diện.
     */
    public LiveData<VerifyEmailUiState> getUiState() {
        return uiState;
    }

    /**
     * Kiểm tra email trước khi yêu cầu gửi lại thư xác minh.
     * Chưa báo gửi thành công khi chưa có phản hồi từ API.
     */
    public void resendVerification(String emailInput) {
        if (isRequestInProgress()) {
            return;
        }

        String email = emailInput == null
                ? ""
                : emailInput.trim();

        if (email.isEmpty()
                || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            uiState.setValue(new VerifyEmailUiState(
                    false,
                    "Không có email hợp lệ. Bạn hãy quay lại đăng nhập.",
                    null
            ));

            return;
        }

        uiState.setValue(new VerifyEmailUiState(
                false,
                "Chưa kết nối dịch vụ gửi email xác minh. "
                        + "Yêu cầu chưa được gửi.",
                null
        ));
    }

    /**
     * Yêu cầu kiểm tra trạng thái xác minh của tài khoản.
     * Không dùng email hoặc thao tác bấm nút để tự đánh dấu verified.
     */
    public void checkVerification() {
        if (isRequestInProgress()) {
            return;
        }

        uiState.setValue(new VerifyEmailUiState(
                false,
                "Chưa kết nối dịch vụ tài khoản. "
                        + "Chưa thể kiểm tra trạng thái xác minh.",
                null
        ));
    }

    /**
     * Cho biết có yêu cầu đang xử lý để tránh bấm nhiều lần.
     */
    private boolean isRequestInProgress() {
        VerifyEmailUiState currentState = uiState.getValue();

        return currentState != null && currentState.isLoading();
    }
}