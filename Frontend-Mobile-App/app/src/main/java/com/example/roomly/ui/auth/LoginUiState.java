package com.example.roomly.ui.auth;

import androidx.annotation.Nullable;

/**
 * Trạng thái giao diện đăng nhập.
 * Các thông báo lỗi là null khi không có lỗi tương ứng.
 */
public class LoginUiState {

    private final boolean loading;

    @Nullable
    private final String emailError;

    @Nullable
    private final String passwordError;

    @Nullable
    private final String generalError;

    /**
     * Khởi tạo trạng thái tải và các thông báo lỗi.
     */
    public LoginUiState(
            boolean loading,
            @Nullable String emailError,
            @Nullable String passwordError,
            @Nullable String generalError
    ) {
        this.loading = loading;
        this.emailError = emailError;
        this.passwordError = passwordError;
        this.generalError = generalError;
    }

    /**
     * Tạo trạng thái ban đầu: chưa tải và không có lỗi.
     */
    public static LoginUiState initial() {
        return new LoginUiState(
                false,
                null,
                null,
                null
        );
    }

    /**
     * Cho biết yêu cầu đăng nhập có đang được xử lý hay không.
     */
    public boolean isLoading() {
        return loading;
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
     * Trả về lỗi chung, ví dụ lỗi kết nối hoặc đăng nhập thất bại.
     */
    @Nullable
    public String getGeneralError() {
        return generalError;
    }
}