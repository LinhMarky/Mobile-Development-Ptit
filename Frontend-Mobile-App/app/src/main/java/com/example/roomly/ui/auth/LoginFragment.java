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
import com.example.roomly.R;

import com.example.roomly.databinding.FragmentLoginBinding;

/**
 * Hiển thị giao diện đăng nhập và quan sát trạng thái từ ViewModel.
 */
public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private LoginViewModel viewModel;

    /**
     * Lấy ViewModel để giữ trạng thái khi Fragment được tạo lại.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(this)
                .get(LoginViewModel.class);
    }

    /**
     * Tạo giao diện đăng nhập từ XML bằng ViewBinding.
     */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentLoginBinding.inflate(
                inflater,
                container,
                false
        );

        return binding.getRoot();
    }

    /**
     * Đăng ký thao tác đăng nhập và quan sát trạng thái giao diện.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnLogin.setOnClickListener(
                clickedView -> submitLogin()
        );

        binding.btnOpenRegister.setOnClickListener(
                clickedView -> openRegisterScreen()
        );

        binding.btnForgotPassword.setOnClickListener(
                clickedView -> openForgotPasswordScreen()
        );

        // Đóng màn hình đăng nhập và trở về trang Cá nhân.
        binding.btnContinueGuest.setOnClickListener(clickedView -> {
            hideKeyboard();
            getParentFragmentManager().popBackStack();
        });

        // Bấm Xong ở ô mật khẩu sẽ đóng bàn phím.
        binding.edtLoginPassword.setOnEditorActionListener(
                (textView, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        hideKeyboard();
                        return true;
                    }

                    return false;
                }
        );

        // Quan sát theo vòng đời giao diện để tránh cập nhật View cũ.
        viewModel.getUiState().observe(
                getViewLifecycleOwner(),
                this::renderState
        );
    }

    /**
     * Đọc email, mật khẩu và chuyển cho ViewModel kiểm tra.
     * Giữ nguyên nội dung mật khẩu, không loại bỏ khoảng trắng.
     */
    private void submitLogin() {
        String email = binding.edtLoginEmail.getText() == null
                ? ""
                : binding.edtLoginEmail.getText().toString();

        String password = binding.edtLoginPassword.getText() == null
                ? ""
                : binding.edtLoginPassword.getText().toString();

        hideKeyboard();
        viewModel.login(email, password);
    }

    /**
     * Hiển thị lỗi và trạng thái tải nhận được từ ViewModel.
     * Vô hiệu hóa thao tác khi yêu cầu đang được xử lý.
     */
    private void renderState(LoginUiState state) {
        if (binding == null || state == null) {
            return;
        }

        binding.layoutLoginEmail.setError(
                state.getEmailError()
        );

        binding.layoutLoginPassword.setError(
                state.getPasswordError()
        );

        String generalError = state.getGeneralError();

        binding.tvLoginError.setText(generalError);
        binding.tvLoginError.setVisibility(
                generalError == null ? View.GONE : View.VISIBLE
        );

        boolean loading = state.isLoading();

        binding.progressLogin.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        binding.btnLogin.setEnabled(!loading);
        binding.btnOpenRegister.setEnabled(!loading);
        binding.btnForgotPassword.setEnabled(!loading);
        binding.btnContinueGuest.setEnabled(!loading);

        binding.edtLoginEmail.setEnabled(!loading);
        binding.edtLoginPassword.setEnabled(!loading);
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
     * Mở màn hình đăng ký và giữ màn hình đăng nhập trong back stack.
     */
    private void openRegisterScreen() {
        hideKeyboard();

        getParentFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                        R.id.fragment_container,
                        new RegisterFragment()
                )
                .addToBackStack(null)
                .commit();
    }

    /**
     * Mở form Quên mật khẩu và giữ màn hình đăng nhập trong back stack.
     */
    private void openForgotPasswordScreen() {
        hideKeyboard();

        getParentFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                        R.id.fragment_container,
                        new ForgotPasswordFragment()
                )
                .addToBackStack(null)
                .commit();
    }

    /**
     * Gỡ listener và giải phóng binding khi giao diện bị hủy.
     */
    @Override
    public void onDestroyView() {
        binding.edtLoginPassword.setOnEditorActionListener(null);
        binding.btnLogin.setOnClickListener(null);
        binding.btnContinueGuest.setOnClickListener(null);
        binding.btnOpenRegister.setOnClickListener(null);
        binding.btnForgotPassword.setOnClickListener(null);

        binding = null;

        super.onDestroyView();
    }
}