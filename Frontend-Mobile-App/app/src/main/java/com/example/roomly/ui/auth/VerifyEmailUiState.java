package com.example.roomly.ui.auth;

import androidx.annotation.Nullable;

/**
 * Trạng thái giao diện xác minh email.
 * Không tự cấp quyền hoặc thay đổi trạng thái tài khoản.
 */
public class VerifyEmailUiState {

    private final boolean loading;

    @Nullable
    private final String errorMessage;

    @Nullable
    private final String resultMessage;

    /**
     * Khởi tạo trạng thái tải, lỗi và thông báo kết quả.
     */
    public VerifyEmailUiState(
            boolean loading,
            @Nullable String errorMessage,
            @Nullable String resultMessage
    ) {
        this.loading = loading;
        this.errorMessage = errorMessage;
        this.resultMessage = resultMessage;
    }

    /**
     * Tạo trạng thái ban đầu, chưa tải và chưa có thông báo.
     */
    public static VerifyEmailUiState initial() {
        return new VerifyEmailUiState(
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
     * Trả về lỗi cần hiển thị trên giao diện.
     */
    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Trả về thông báo kết quả nhận được.
     */
    @Nullable
    public String getResultMessage() {
        return resultMessage;
    }
}