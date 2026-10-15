package com.example.roomly.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.roomly.databinding.FragmentVerifyEmailBinding;
import com.example.roomly.ui.NavigationHelper;

/** Xác minh email bằng mã thật; ViewModel giữ kết quả khi giao diện được tạo lại. */
public class VerifyEmailFragment extends Fragment {
    private FragmentVerifyEmailBinding binding;
    private AuthChallengeViewModel viewModel;
    private String email = "";

    public static VerifyEmailFragment newInstance(String email) {
        VerifyEmailFragment screen = new VerifyEmailFragment();
        Bundle arguments = new Bundle();
        arguments.putString("email", email);
        screen.setArguments(arguments);
        return screen;
    }

    @Override public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        if (getArguments() != null) email = getArguments().getString("email", "").trim();
        viewModel = new ViewModelProvider(this).get(AuthChallengeViewModel.class);
    }

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle state) {
        binding = FragmentVerifyEmailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        binding.tvVerifyEmailAddress.setText(email);
        binding.btnVerifyEmailBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.btnResendVerification.setOnClickListener(v -> viewModel.requestCode(email, false));
        binding.btnCheckVerification.setOnClickListener(v -> verify());
        viewModel.phase().observe(getViewLifecycleOwner(), phase -> render(phase));
    }

    private void verify() {
        String code = binding.edtVerificationCode.getText().toString().trim();
        if (!code.matches("[0-9]{6}")) {
            binding.edtVerificationCode.setError("Nhập đủ 6 chữ số.");
            return;
        }
        viewModel.verify(email, code);
    }

    private void render(String phase) {
        if (binding == null) return;
        boolean busy = viewModel.isBusy();
        binding.progressVerifyEmail.setVisibility(busy ? View.VISIBLE : View.GONE);
        binding.btnResendVerification.setEnabled(!busy && !email.isEmpty());
        binding.btnCheckVerification.setEnabled(!busy && !email.isEmpty());
        binding.edtVerificationCode.setEnabled(!busy);
        binding.tvVerifyEmailError.setText(viewModel.message());
        binding.tvVerifyEmailError.setVisibility(AuthChallengeViewModel.ERROR.equals(phase) ? View.VISIBLE : View.GONE);
        binding.tvVerifyEmailResult.setText(viewModel.message().isEmpty() ? "Hãy kiểm tra hộp thư của bạn." : viewModel.message());
        binding.tvVerifyEmailResult.setVisibility(AuthChallengeViewModel.CODE_SENT.equals(phase) ? View.VISIBLE : View.GONE);
        if (AuthChallengeViewModel.COMPLETE.equals(phase)) {
            NavigationHelper.whenResumed(this, () -> {
                if (!viewModel.consumeCompletion()) return;
                Toast.makeText(requireContext(), "Đã xác minh email.", Toast.LENGTH_LONG).show();
                getParentFragmentManager().popBackStack();
            });
        }
    }

    @Override public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
