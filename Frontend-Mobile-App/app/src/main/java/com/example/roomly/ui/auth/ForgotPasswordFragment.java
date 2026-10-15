package com.example.roomly.ui.auth;

import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.roomly.databinding.FragmentForgotPasswordBinding;
import com.example.roomly.ui.NavigationHelper;
import java.nio.charset.StandardCharsets;

/** Gửi mã khôi phục rồi đặt mật khẩu mới ngay trên biểu mẫu, không giữ callback vào View. */
public class ForgotPasswordFragment extends Fragment {
    private FragmentForgotPasswordBinding binding;
    private AuthChallengeViewModel viewModel;

    @Override public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        viewModel = new ViewModelProvider(this).get(AuthChallengeViewModel.class);
    }

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle state) {
        binding = FragmentForgotPasswordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        if (viewModel.hasRequestedCode()) binding.edtForgotPasswordEmail.setText(viewModel.email());
        binding.btnRequestPasswordReset.setOnClickListener(v -> requestCode());
        binding.btnSubmitPasswordReset.setOnClickListener(v -> resetPassword());
        binding.btnForgotPasswordBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.btnForgotPasswordLogin.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        viewModel.phase().observe(getViewLifecycleOwner(), phase -> render(phase));
    }

    private void requestCode() {
        String email = binding.edtForgotPasswordEmail.getText().toString().trim();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.layoutForgotPasswordEmail.setError("Email không hợp lệ.");
            return;
        }
        binding.layoutForgotPasswordEmail.setError(null);
        viewModel.requestCode(email, true);
    }

    private void resetPassword() {
        String code = binding.edtRecoveryCode.getText().toString().trim();
        String password = binding.edtRecoveryPassword.getText().toString();
        if (!code.matches("[0-9]{6}")) {
            binding.edtRecoveryCode.setError("Nhập đủ 6 chữ số.");
            return;
        }
        if (password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            binding.edtRecoveryPassword.setError("Mật khẩu cần ít nhất 12 ký tự, tối đa 72 byte UTF-8.");
            return;
        }
        viewModel.resetPassword(code, password);
    }

    private void render(String phase) {
        if (binding == null) return;
        boolean busy = viewModel.isBusy();
        binding.progressForgotPassword.setVisibility(busy ? View.VISIBLE : View.GONE);
        binding.edtForgotPasswordEmail.setEnabled(!busy && !viewModel.hasRequestedCode());
        binding.btnRequestPasswordReset.setEnabled(!busy);
        binding.btnSubmitPasswordReset.setEnabled(!busy);
        binding.edtRecoveryCode.setEnabled(!busy);
        binding.edtRecoveryPassword.setEnabled(!busy);
        binding.layoutRecoveryForm.setVisibility(viewModel.hasRequestedCode() ? View.VISIBLE : View.GONE);
        binding.tvForgotPasswordError.setText(viewModel.message());
        binding.tvForgotPasswordError.setVisibility(AuthChallengeViewModel.ERROR.equals(phase) ? View.VISIBLE : View.GONE);
        binding.tvForgotPasswordResult.setText(viewModel.message().isEmpty() ? "Nhập mã trong email và mật khẩu mới." : viewModel.message());
        binding.tvForgotPasswordResult.setVisibility(AuthChallengeViewModel.CODE_SENT.equals(phase) ? View.VISIBLE : View.GONE);
        if (AuthChallengeViewModel.COMPLETE.equals(phase)) {
            NavigationHelper.whenResumed(this, () -> {
                if (!viewModel.consumeCompletion()) return;
                Toast.makeText(requireContext(), "Đã đổi mật khẩu. Hãy đăng nhập lại.", Toast.LENGTH_LONG).show();
                getParentFragmentManager().popBackStack();
            });
        }
    }

    @Override public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
