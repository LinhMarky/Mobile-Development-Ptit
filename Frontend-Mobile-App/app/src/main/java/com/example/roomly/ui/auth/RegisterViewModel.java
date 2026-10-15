package com.example.roomly.ui.auth;

import android.util.Patterns;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roomly.data.repository.AuthRepository;

/** Kiểm tra thông tin đăng ký và quản lý trạng thái giao diện. Chưa gọi API tạo tài khoản. */
public class RegisterViewModel extends ViewModel {

    private final MutableLiveData<RegisterUiState> uiState =
            new MutableLiveData<>(RegisterUiState.initial());

    private final MutableLiveData<Boolean> registered = new MutableLiveData<>(false);

    public LiveData<Boolean> getRegistered() {
        return registered;
    }

    public void consumeRegistration() {
        registered.setValue(false);
    }

    /** Cho phép Fragment theo dõi trạng thái đăng ký. */
    public LiveData<RegisterUiState> getUiState() {
        return uiState;
    }

    /**
     * Kiểm tra các ô nhập khi người dùng bấm Tạo tài khoản. Không trim mật khẩu hoặc mật khẩu xác
     * nhận.
     */
    public void register(
            String nameInput,
            String emailInput,
            String passwordInput,
            String confirmPasswordInput) {
        RegisterUiState currentState = uiState.getValue();

        if (currentState != null && currentState.isLoading()) {
            return;
        }

        String fullName = nameInput == null ? "" : nameInput.trim();

        String email = emailInput == null ? "" : emailInput.trim();

        String password = passwordInput == null ? "" : passwordInput;

        String confirmPassword = confirmPasswordInput == null ? "" : confirmPasswordInput;

        String nameError = validateName(fullName);
        String emailError = validateEmail(email);
        String passwordError = validatePassword(password);

        String confirmPasswordError = validateConfirmPassword(password, confirmPassword);

        boolean hasError =
                nameError != null
                        || emailError != null
                        || passwordError != null
                        || confirmPasswordError != null;

        if (hasError) {
            uiState.setValue(
                    new RegisterUiState(
                            false,
                            nameError,
                            emailError,
                            passwordError,
                            confirmPasswordError,
                            null));

            return;
        }

        uiState.setValue(new RegisterUiState(true, null, null, null, null, null));
        new AuthRepository()
                .register(
                        fullName,
                        email,
                        password,
                        () -> {
                            uiState.setValue(RegisterUiState.initial());
                            registered.setValue(true);
                        },
                        text ->
                                uiState.setValue(
                                        new RegisterUiState(false, null, null, null, null, text)));
    }

    /** Kiểm tra họ tên có từ 2 đến 100 ký tự. */
    @Nullable
    private String validateName(String fullName) {
        int length = fullName.codePointCount(0, fullName.length());

        if (length < 2 || length > 100) {
            return "Họ và tên phải từ 2 đến 100 ký tự.";
        }

        return null;
    }

    /** Kiểm tra email không trống và có định dạng hợp lệ. */
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

    /**
     * Kiểm tra mật khẩu có từ ít nhất 12 ký tự và tối đa 72 byte. Giữ nguyên khoảng trắng và không
     * ép chữ hoa hoặc ký tự đặc biệt.
     */
    @Nullable
    private String validatePassword(String password) {
        int length = password.codePointCount(0, password.length());

        if (length < 12 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            return "Mật khẩu phải từ ít nhất 12 ký tự và tối đa 72 byte.";
        }

        return null;
    }

    /**
     * Kiểm tra mật khẩu xác nhận trùng chính xác với mật khẩu. Trường xác nhận chỉ dùng ở frontend,
     * không gửi trong RegisterRequest.
     */
    @Nullable
    private String validateConfirmPassword(String password, String confirmPassword) {
        if (confirmPassword.isEmpty()) {
            return "Bạn hãy nhập lại mật khẩu.";
        }

        if (!password.equals(confirmPassword)) {
            return "Mật khẩu nhập lại không khớp.";
        }

        return null;
    }
}
