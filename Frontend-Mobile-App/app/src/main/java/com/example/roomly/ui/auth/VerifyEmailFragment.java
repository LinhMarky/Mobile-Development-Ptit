package com.example.roomly.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.roomly.databinding.FragmentVerifyEmailBinding;

/**
 * Hiển thị hướng dẫn xác minh email và trạng thái từ ViewModel.
 */
public class VerifyEmailFragment extends Fragment {

    private static final String ARG_EMAIL = "email";

    private FragmentVerifyEmailBinding binding;
    private VerifyEmailViewModel viewModel;

    // Email chỉ dùng để hiển thị và yêu cầu gửi lại thư.
    private String email = "";

    /**
     * Tạo màn hình xác minh với email của tài khoản.
     * Email này không được dùng để tự xác nhận quyền tài khoản.
     */
    public static VerifyEmailFragment newInstance(String email) {
        VerifyEmailFragment fragment = new VerifyEmailFragment();

        Bundle args = new Bundle();
        args.putString(ARG_EMAIL, email);

        fragment.setArguments(args);

        return fragment;
    }

    /**
     * Lấy ViewModel và đọc email được truyền qua Bundle.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(this)
                .get(VerifyEmailViewModel.class);

        Bundle args = getArguments();

        if (args != null) {
            String argumentEmail = args.getString(ARG_EMAIL);

            email = argumentEmail == null
                    ? ""
                    : argumentEmail.trim();
        }
    }

    /**
     * Tạo giao diện xác minh email từ XML bằng ViewBinding.
     */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentVerifyEmailBinding.inflate(
                inflater,
                container,
                false
        );

        return binding.getRoot();
    }

    /**
     * Hiển thị email, đăng ký thao tác và quan sát trạng thái.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        binding.tvVerifyEmailAddress.setText(email);

        binding.tvVerifyEmailAddress.setVisibility(
                email.isEmpty() ? View.GONE : View.VISIBLE
        );

        binding.btnVerifyEmailBack.setOnClickListener(
                clickedView ->
                        getParentFragmentManager().popBackStack()
        );

        binding.btnResendVerification.setOnClickListener(
                clickedView ->
                        viewModel.resendVerification(email)
        );

        binding.btnCheckVerification.setOnClickListener(
                clickedView -> viewModel.checkVerification()
        );

        viewModel.getUiState().observe(
                getViewLifecycleOwner(),
                this::renderState
        );
    }

    /**
     * Hiển thị lỗi, thông báo kết quả và trạng thái đang xử lý.
     */
    private void renderState(VerifyEmailUiState state) {
        if (binding == null || state == null) {
            return;
        }

        String errorMessage = state.getErrorMessage();

        binding.tvVerifyEmailError.setText(errorMessage);
        binding.tvVerifyEmailError.setVisibility(
                errorMessage == null ? View.GONE : View.VISIBLE
        );

        String resultMessage = state.getResultMessage();

        binding.tvVerifyEmailResult.setText(resultMessage);
        binding.tvVerifyEmailResult.setVisibility(
                resultMessage == null ? View.GONE : View.VISIBLE
        );

        boolean loading = state.isLoading();

        binding.progressVerifyEmail.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        binding.btnResendVerification.setEnabled(
                !loading && !email.isEmpty()
        );

        binding.btnCheckVerification.setEnabled(!loading);
        binding.btnVerifyEmailBack.setEnabled(!loading);
    }

    /**
     * Gỡ listener và giải phóng binding khi giao diện bị hủy.
     */
    @Override
    public void onDestroyView() {
        binding.btnVerifyEmailBack.setOnClickListener(null);
        binding.btnResendVerification.setOnClickListener(null);
        binding.btnCheckVerification.setOnClickListener(null);

        binding = null;

        super.onDestroyView();
    }
}