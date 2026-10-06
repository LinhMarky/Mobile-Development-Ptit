package com.example.roomly.ui.auth;

import android.util.Patterns;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * Kiểm tra dữ liệu nhập và quản lý trạng thái màn hình đăng nhập.
 * Chưa gọi API hoặc tạo phiên đăng nhập.
 */
public class LoginViewModel extends ViewModel {

    // Chỉ ViewModel được cập nhật trạng thái.
    private final MutableLiveData<LoginUiState> uiState =
            new MutableLiveData<>(LoginUiState.initial());

    /**
     * Cho phép Fragment theo dõi trạng thái qua LiveData chỉ đọc.
     */
    public LiveData<LoginUiState> getUiState() {
        return uiState;
    }

    /**
     * Kiểm tra dữ liệu khi người dùng bấm Đăng nhập.
     * Không trim mật khẩu vì khoảng trắng có thể thuộc mật khẩu.
     */
    public void login(String emailInput, String passwordInput) {
        LoginUiState currentState = uiState.getValue();

        if (currentState != null && currentState.isLoading()) {
            return;
        }

        String email = emailInput == null
                ? ""
                : emailInput.trim();

        String password = passwordInput == null
                ? ""
                : passwordInput;

        String emailError = validateEmail(email);
        String passwordError = validatePassword(password);

        if (emailError != null || passwordError != null) {
            uiState.setValue(new LoginUiState(
                    false,
                    emailError,
                    passwordError,
                    null
            ));

            return;
        }

        // Chỉ kiểm tra form ở bước này, chưa xác thực tài khoản.
        uiState.setValue(new LoginUiState(
                false,
                null,
                null,
                "Chưa kết nối dịch vụ đăng nhập. "
                        + "Bạn có thể tiếp tục với tư cách khách."
        ));
    }

    /**
     * Kiểm tra email đã nhập và có định dạng hợp lệ.
     */
    private String validateEmail(String email) {
        if (email.isEmpty()) {
            return "Bạn hãy nhập email.";
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return "Email không đúng định dạng.";
        }

        return null;
    }

    /**
     * Kiểm tra mật khẩu đăng nhập không trống.
     * Quy tắc tối thiểu 12 ký tự sẽ áp dụng ở form tạo mật khẩu.
     */
    private String validatePassword(String password) {
        if (password.isEmpty()) {
            return "Bạn hãy nhập mật khẩu.";
        }

        return null;
    }
}