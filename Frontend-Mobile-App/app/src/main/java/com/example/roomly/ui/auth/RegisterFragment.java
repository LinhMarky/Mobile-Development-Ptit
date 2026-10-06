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

import com.example.roomly.databinding.FragmentRegisterBinding;

/**
 * Hiển thị form đăng ký và quan sát trạng thái từ ViewModel.
 */
public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private RegisterViewModel viewModel;

    /**
     * Lấy ViewModel để giữ trạng thái khi Fragment được tạo lại.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(this)
                .get(RegisterViewModel.class);
    }

    /**
     * Tạo giao diện đăng ký từ XML bằng ViewBinding.
     */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentRegisterBinding.inflate(
                inflater,
                container,
                false
        );

        return binding.getRoot();
    }

    /**
     * Đăng ký thao tác tạo tài khoản, quay lại và theo dõi trạng thái.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnRegister.setOnClickListener(
                clickedView -> submitRegister()
        );

        binding.btnRegisterBack.setOnClickListener(
                clickedView -> returnToLogin()
        );

        binding.btnBackToLogin.setOnClickListener(
                clickedView -> returnToLogin()
        );

        // Bấm Xong ở ô xác nhận mật khẩu để đóng bàn phím.
        binding.edtRegisterConfirmPassword.setOnEditorActionListener(
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
     * Đọc thông tin nhập và gửi cho ViewModel kiểm tra.
     * Giữ nguyên mật khẩu, không trim khoảng trắng.
     */
    private void submitRegister() {
        String fullName = binding.edtRegisterName.getText() == null
                ? ""
                : binding.edtRegisterName.getText().toString();

        String email = binding.edtRegisterEmail.getText() == null
                ? ""
                : binding.edtRegisterEmail.getText().toString();

        String password = binding.edtRegisterPassword.getText() == null
                ? ""
                : binding.edtRegisterPassword.getText().toString();

        String confirmPassword =
                binding.edtRegisterConfirmPassword.getText() == null
                        ? ""
                        : binding.edtRegisterConfirmPassword
                        .getText()
                        .toString();

        hideKeyboard();

        viewModel.register(
                fullName,
                email,
                password,
                confirmPassword
        );
    }

    /**
     * Hiển thị lỗi từng ô, lỗi chung và trạng thái đang xử lý.
     */
    private void renderState(RegisterUiState state) {
        if (binding == null || state == null) {
            return;
        }

        binding.layoutRegisterName.setError(
                state.getNameError()
        );

        binding.layoutRegisterEmail.setError(
                state.getEmailError()
        );

        binding.layoutRegisterPassword.setError(
                state.getPasswordError()
        );

        binding.layoutRegisterConfirmPassword.setError(
                state.getConfirmPasswordError()
        );

        String generalError = state.getGeneralError();

        binding.tvRegisterError.setText(generalError);
        binding.tvRegisterError.setVisibility(
                generalError == null ? View.GONE : View.VISIBLE
        );

        boolean loading = state.isLoading();

        binding.progressRegister.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        binding.btnRegister.setEnabled(!loading);
        binding.btnRegisterBack.setEnabled(!loading);
        binding.btnBackToLogin.setEnabled(!loading);

        binding.edtRegisterName.setEnabled(!loading);
        binding.edtRegisterEmail.setEnabled(!loading);
        binding.edtRegisterPassword.setEnabled(!loading);
        binding.edtRegisterConfirmPassword.setEnabled(!loading);
    }

    /**
     * Đóng bàn phím và quay lại màn hình đăng nhập trong back stack.
     */
    private void returnToLogin() {
        hideKeyboard();
        getParentFragmentManager().popBackStack();
    }

    /**
     * Đóng bàn phím và bỏ focus khỏi ô đang nhập.
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
     * Gỡ các listener và giải phóng binding khi giao diện bị hủy.
     */
    @Override
    public void onDestroyView() {
        binding.edtRegisterConfirmPassword
                .setOnEditorActionListener(null);

        binding.btnRegister.setOnClickListener(null);
        binding.btnRegisterBack.setOnClickListener(null);
        binding.btnBackToLogin.setOnClickListener(null);

        binding = null;

        super.onDestroyView();
    }
}