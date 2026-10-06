package com.example.roomly.ui.auth;

import androidx.annotation.Nullable;

/**
 * Chứa trạng thái tải và các lỗi của màn hình đăng ký.
 * Không lưu mật khẩu trong trạng thái giao diện.
 */
public class RegisterUiState {

    private final boolean loading;

    @Nullable
    private final String nameError;

    @Nullable
    private final String emailError;

    @Nullable
    private final String passwordError;

    @Nullable
    private final String confirmPasswordError;

    @Nullable
    private final String generalError;

    /**
     * Khởi tạo trạng thái tải và thông báo lỗi cho từng ô nhập.
     */
    public RegisterUiState(
            boolean loading,
            @Nullable String nameError,
            @Nullable String emailError,
            @Nullable String passwordError,
            @Nullable String confirmPasswordError,
            @Nullable String generalError
    ) {
        this.loading = loading;
        this.nameError = nameError;
        this.emailError = emailError;
        this.passwordError = passwordError;
        this.confirmPasswordError = confirmPasswordError;
        this.generalError = generalError;
    }

    /**
     * Tạo trạng thái ban đầu, chưa tải và không có lỗi.
     */
    public static RegisterUiState initial() {
        return new RegisterUiState(
                false,
                null,
                null,
                null,
                null,
                null
        );
    }

    /**
     * Cho biết yêu cầu đăng ký đang được xử lý hay không.
     */
    public boolean isLoading() {
        return loading;
    }

    /**
     * Trả về lỗi của ô họ tên.
     */
    @Nullable
    public String getNameError() {
        return nameError;
    }

    /**
     * Trả về lỗi của ô email.
     */
    @Nullable
    public String getEmailError() {
        return emailError;
    }

    /**
     * Trả về lỗi của ô mật khẩu.
     */
    @Nullable
    public String getPasswordError() {
        return passwordError;
    }

    /**
     * Trả về lỗi của ô nhập lại mật khẩu.
     */
    @Nullable
    public String getConfirmPasswordError() {
        return confirmPasswordError;
    }

    /**
     * Trả về lỗi chung của quá trình đăng ký.
     */
    @Nullable
    public String getGeneralError() {
        return generalError;
    }
}