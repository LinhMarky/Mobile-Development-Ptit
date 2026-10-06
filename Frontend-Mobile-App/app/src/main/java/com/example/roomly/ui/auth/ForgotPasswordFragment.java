package com.example.roomly.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.roomly.databinding.FragmentForgotPasswordBinding;

/**
 * Hiển thị form yêu cầu đặt lại mật khẩu theo mô hình MVVM.
 */
public class ForgotPasswordFragment extends Fragment {

    private FragmentForgotPasswordBinding binding;
    private ForgotPasswordViewModel viewModel;

    /**
     * Lấy ViewModel để giữ trạng thái khi Fragment được tạo lại.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(this)
                .get(ForgotPasswordViewModel.class);
    }

    /**
     * Tạo giao diện Quên mật khẩu từ XML bằng ViewBinding.
     */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentForgotPasswordBinding.inflate(
                inflater,
                container,
                false
        );

        return binding.getRoot();
    }

    /**
     * Đăng ký thao tác gửi yêu cầu, quay lại và quan sát trạng thái.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnRequestPasswordReset.setOnClickListener(
                clickedView -> submitRequest()
        );

        binding.btnForgotPasswordBack.setOnClickListener(
                clickedView -> returnToLogin()
        );

        binding.btnForgotPasswordLogin.setOnClickListener(
                clickedView -> returnToLogin()
        );

        // Bấm Xong để đóng bàn phím của ô email.
        binding.edtForgotPasswordEmail.setOnEditorActionListener(
                (textView, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        hideKeyboard();
                        return true;
                    }

                    return false;
                }
        );

        viewModel.getUiState().observe(
                getViewLifecycleOwner(),
                this::renderState
        );
    }

    /**
     * Đọc email và chuyển cho ViewModel kiểm tra.
     */
    private void submitRequest() {
        String email =
                binding.edtForgotPasswordEmail.getText() == null
                        ? ""
                        : binding.edtForgotPasswordEmail
                        .getText()
                        .toString();

        hideKeyboard();
        viewModel.requestPasswordReset(email);
    }

    /**
     * Hiển thị lỗi, kết quả tiếp nhận và trạng thái tải.
     */
    private void renderState(ForgotPasswordUiState state) {
        if (binding == null || state == null) {
            return;
        }

        binding.layoutForgotPasswordEmail.setError(
                state.getEmailError()
        );

        String generalError = state.getGeneralError();

        binding.tvForgotPasswordError.setText(generalError);
        binding.tvForgotPasswordError.setVisibility(
                generalError == null ? View.GONE : View.VISIBLE
        );

        binding.tvForgotPasswordResult.setVisibility(
                state.isRequestAccepted()
                        ? View.VISIBLE
                        : View.GONE
        );

        boolean loading = state.isLoading();

        binding.progressForgotPassword.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        binding.btnRequestPasswordReset.setEnabled(!loading);
        binding.btnForgotPasswordBack.setEnabled(!loading);
        binding.btnForgotPasswordLogin.setEnabled(!loading);
        binding.edtForgotPasswordEmail.setEnabled(!loading);
    }

    /**
     * Đóng bàn phím và trở về màn hình đăng nhập trong back stack.
     */
    private void returnToLogin() {
        hideKeyboard();
        getParentFragmentManager().popBackStack();
    }

    /**
     * Đóng bàn phím và bỏ focus khỏi ô nhập.
     */
    private void hideKeyboard() {
        if (binding == null) {
            return;
        }

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(
                        requireActivity().getWindow(),
                        binding.getRoot()
                );

        controller.hide(WindowInsetsCompat.Type.ime());

        View focusedView = binding.getRoot().findFocus();

        if (focusedView != null) {
            focusedView.clearFocus();
        }
    }

    /**
     * Gỡ listener và giải phóng binding khi giao diện bị hủy.
     */
    @Override
    public void onDestroyView() {
        binding.edtForgotPasswordEmail
                .setOnEditorActionListener(null);

        binding.btnRequestPasswordReset.setOnClickListener(null);
        binding.btnForgotPasswordBack.setOnClickListener(null);
        binding.btnForgotPasswordLogin.setOnClickListener(null);

        binding = null;

        super.onDestroyView();
    }
}