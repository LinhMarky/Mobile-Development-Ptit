package com.example.roomly.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.example.roomly.data.repository.AuthRepository;

/** Giữ kết quả gửi mã/xác nhận qua vòng đời; không giữ View, mật khẩu hoặc mã OTP. */
public class AuthChallengeViewModel extends ViewModel {
    public static final String IDLE = "IDLE";
    public static final String SENDING = "SENDING";
    public static final String CODE_SENT = "CODE_SENT";
    public static final String SUBMITTING = "SUBMITTING";
    public static final String COMPLETE = "COMPLETE";
    public static final String ERROR = "ERROR";
    private final SavedStateHandle state;
    private final AuthRepository repository = new AuthRepository();
    private String message = "";

    public AuthChallengeViewModel(SavedStateHandle state) {
        this.state = state;
        if (isBusy()) {
            state.set("phase", ERROR);
            message = "Ứng dụng vừa được mở lại. Kiểm tra email hoặc đăng nhập trước khi thử lại.";
        }
    }

    public LiveData<String> phase() { return state.getLiveData("phase", IDLE); }
    public String currentPhase() { return state.get("phase") == null ? IDLE : state.get("phase"); }
    public String message() { return message; }
    public boolean isBusy() { return SENDING.equals(currentPhase()) || SUBMITTING.equals(currentPhase()); }
    public boolean hasRequestedCode() { return Boolean.TRUE.equals(state.get("code_requested")); }
    public String email() { String email = state.get("email"); return email == null ? "" : email; }

    public void requestCode(String email, boolean recovery) {
        if (isBusy() || COMPLETE.equals(currentPhase())) return;
        state.set("email", email);
        message = "";
        state.set("phase", SENDING);
        repository.requestCode(email, recovery, () -> {
            state.set("code_requested", true);
            message = "Hãy kiểm tra hộp thư của bạn.";
            state.set("phase", CODE_SENT);
        }, this::failed);
    }

    public void verify(String email, String code) {
        if (isBusy() || COMPLETE.equals(currentPhase())) return;
        state.set("email", email);
        message = "";
        state.set("phase", SUBMITTING);
        repository.verifyEmail(email, code, this::completed, this::failed);
    }

    public void resetPassword(String code, String password) {
        if (isBusy() || COMPLETE.equals(currentPhase()) || !hasRequestedCode()) return;
        message = "";
        state.set("phase", SUBMITTING);
        repository.resetPassword(email(), code, password, this::completed, this::failed);
    }

    private void completed() { state.set("phase", COMPLETE); }
    private void failed(String error) { message = error; state.set("phase", ERROR); }

    public boolean consumeCompletion() {
        if (!COMPLETE.equals(currentPhase())) return false;
        state.set("phase", IDLE);
        return true;
    }
}
