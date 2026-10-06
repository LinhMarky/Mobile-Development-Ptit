package com.example.roomly.ui.auth;

import android.util.Patterns;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * Kiểm tra email và quản lý trạng thái yêu cầu đặt lại mật khẩu.
 * Chưa kết nối API khôi phục mật khẩu.
 */
public class ForgotPasswordViewModel extends ViewModel {

    private final MutableLiveData<ForgotPasswordUiState> uiState =
            new MutableLiveData<>(ForgotPasswordUiState.initial());

    /**
     * Cho phép Fragment quan sát trạng thái giao diện.
     */
    public LiveData<ForgotPasswordUiState> getUiState() {
        return uiState;
    }

    /**
     * Kiểm tra email khi người dùng bấm Gửi yêu cầu.
     * Chưa hiển thị kết quả thành công khi chưa có phản hồi từ API.
     */
    public void requestPasswordReset(String emailInput) {
        ForgotPasswordUiState currentState = uiState.getValue();

        if (currentState != null && currentState.isLoading()) {
            return;
        }

        String email = emailInput == null
                ? ""
                : emailInput.trim();

        String emailError = validateEmail(email);

        if (emailError != null) {
            uiState.setValue(new ForgotPasswordUiState(
                    false,
                    false,
                    emailError,
                    null
            ));

            return;
        }

        // Email hợp lệ nhưng chưa có API để tiếp nhận yêu cầu.
        uiState.setValue(new ForgotPasswordUiState(
                false,
                false,
                null,
                "Chưa kết nối dịch vụ khôi phục mật khẩu. "
                        + "Yêu cầu chưa được gửi."
        ));
    }

    /**
     * Kiểm tra email không trống và có định dạng hợp lệ.
     */
    @Nullable
    private String validateEmail(String email) {
        if (email.isEmpty()) {
            return "Bạn hãy nhập email.";
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return "Email không đúng định dạng.";
        }

        return null;
    }
}