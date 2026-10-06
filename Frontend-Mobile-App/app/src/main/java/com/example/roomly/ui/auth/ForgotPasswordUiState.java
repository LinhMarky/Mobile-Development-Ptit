package com.example.roomly.ui.auth;

import androidx.annotation.Nullable;

/**
 * Chứa trạng thái tải, lỗi và kết quả yêu cầu đặt lại mật khẩu.
 */
public class ForgotPasswordUiState {

    private final boolean loading;
    private final boolean requestAccepted;

    @Nullable
    private final String emailError;

    @Nullable
    private final String generalError;

    /**
     * Khởi tạo trạng thái của màn hình Quên mật khẩu.
     * requestAccepted chỉ là true khi API đã tiếp nhận yêu cầu.
     */
    public ForgotPasswordUiState(
            boolean loading,
            boolean requestAccepted,
            @Nullable String emailError,
            @Nullable String generalError
    ) {
        this.loading = loading;
        this.requestAccepted = requestAccepted;
        this.emailError = emailError;
        this.generalError = generalError;
    }

    /**
     * Tạo trạng thái ban đầu, chưa gửi yêu cầu và không có lỗi.
     */
    public static ForgotPasswordUiState initial() {
        return new ForgotPasswordUiState(
                false,
                false,
                null,
                null
        );
    }

    /**
     * Cho biết yêu cầu đang được xử lý hay không.
     */
    public boolean isLoading() {
        return loading;
    }

    /**
     * Cho biết API đã tiếp nhận yêu cầu hay chưa.
     * Không khẳng định email có tài khoản hoặc thư đã được gửi.
     */
    public boolean isRequestAccepted() {
        return requestAccepted;
    }

    /**
     * Trả về lỗi của ô email.
     */
    @Nullable
    public String getEmailError() {
        return emailError;
    }

    /**
     * Trả về lỗi chung, ví dụ lỗi mạng hoặc lỗi dịch vụ.
     */
    @Nullable
    public String getGeneralError() {
        return generalError;
    }
}