package com.example.roomly.ui.profile;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.roomly.R;
import com.example.roomly.data.model.AppMode;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentProfileBinding;
import com.example.roomly.ui.auth.LoginFragment;
import com.example.roomly.ui.auth.VerifyEmailFragment;
import com.google.android.material.button.MaterialButton;

import com.example.roomly.BuildConfig;
import com.example.roomly.data.repository.DebugSessionHelper;


/**
 * Hiển thị trang Cá nhân theo phiên đăng nhập.
 * Cho phép chọn chế độ Người thuê hoặc Chủ trọ theo vai trò hiện có.
 */
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;

    // Trạng thái hiện tại dùng để hiển thị giao diện.
    private SessionState currentSession = SessionState.guest();
    private AppMode currentMode = AppMode.TENANT;

    /**
     * Lấy ViewModel quản lý phiên và chế độ giao diện.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(this)
                .get(ProfileViewModel.class);
    }

    /**
     * Tạo giao diện Cá nhân từ XML bằng ViewBinding.
     */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentProfileBinding.inflate(
                inflater,
                container,
                false
        );

        return binding.getRoot();
    }

    /**
     * Đăng ký các thao tác và quan sát phiên, chế độ theo vòng đời View.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        setupActionButtons();

        viewModel.getSessionState().observe(
                getViewLifecycleOwner(),
                this::renderSession
        );

        viewModel.getMode().observe(
                getViewLifecycleOwner(),
                this::renderMode
        );
    }

    /**
     * Đăng ký đăng nhập, xác minh, chỉnh sửa và chọn chế độ.
     */
    private void setupActionButtons() {
        binding.btnOpenLogin.setOnClickListener(
                view -> openLoginScreen()
        );

        binding.btnProfileVerifyEmail.setOnClickListener(
                view -> openVerifyEmailScreen()
        );

        binding.btnEditProfile.setOnClickListener(
                view -> handleEditProfile()
        );

        binding.btnModeTenant.setOnClickListener(
                view -> selectMode(AppMode.TENANT)
        );

        binding.btnModeHost.setOnClickListener(
                view -> selectMode(AppMode.HOST)
        );

        binding.btnEnableDemoSession.setOnClickListener(view -> {
            boolean enabled = DebugSessionHelper.enableDemoSession();

            if (!enabled) {
                Toast.makeText(
                        requireContext(),
                        "Không thể bật phiên mẫu khi đang có tài khoản đăng nhập.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        binding.btnDisableDemoSession.setOnClickListener(view ->
                DebugSessionHelper.disableDemoSession()
        );
    }

    /**
     * Hiển thị lời mời đăng nhập cho khách hoặc thông tin tài khoản.
     * Các lựa chọn chế độ được cập nhật theo vai trò trong phiên.
     */
    private void renderSession(@Nullable SessionState session) {
        if (binding == null) {
            return;
        }

        currentSession = session == null
                ? SessionState.guest()
                : session;

        boolean loggedIn = currentSession.isLoggedIn();

        // Chỉ hiện nút bật phiên mẫu cho khách trong bản debug.
        binding.btnEnableDemoSession.setVisibility(
                BuildConfig.DEBUG && !loggedIn
                        ? View.VISIBLE
                        : View.GONE
        );

        // Chỉ hiện nút tắt khi đang dùng đúng tài khoản mẫu.
        boolean isDemoSession = BuildConfig.DEBUG
                && loggedIn
                && "debug-demo-user".equals(
                currentSession.getUserId()
        );

        binding.btnDisableDemoSession.setVisibility(
                isDemoSession ? View.VISIBLE : View.GONE
        );

        binding.layoutProfileGuest.setVisibility(
                loggedIn ? View.GONE : View.VISIBLE
        );

        binding.layoutProfileAccount.setVisibility(
                loggedIn ? View.VISIBLE : View.GONE
        );

        binding.tvProfileName.setText(
                currentSession.getFullName()
        );

        binding.tvProfileEmail.setText(
                currentSession.getEmail()
        );

        // Không dùng số điện thoại mẫu thay dữ liệu tài khoản thật.
        binding.tvProfilePhone.setText("");
        binding.tvProfilePhone.setVisibility(View.GONE);

        if (!loggedIn) {
            binding.tvProfileVerification.setText("");
            binding.btnProfileVerifyEmail.setVisibility(View.GONE);
            binding.btnEditProfile.setEnabled(false);

        } else {
            boolean verified = currentSession.isEmailVerified();

            binding.tvProfileVerification.setText(
                    verified
                            ? "Email đã được xác minh"
                            : "Email chưa được xác minh"
            );

            binding.btnProfileVerifyEmail.setVisibility(
                    verified ? View.GONE : View.VISIBLE
            );

            binding.btnEditProfile.setEnabled(true);
        }

        currentMode = viewModel.getCurrentMode();

        renderModeOptions();
    }

    /**
     * Nhận chế độ mới từ repository và cập nhật các nút lựa chọn.
     */
    private void renderMode(@Nullable AppMode mode) {
        currentMode = mode == null ? AppMode.TENANT : mode;

        renderModeOptions();
    }

    /**
     * Chỉ hiện chế độ mà tài khoản có vai trò tương ứng.
     * Khách và tài khoản chỉ có ADMIN không thấy bộ chọn này.
     */
    private void renderModeOptions() {
        if (binding == null) {
            return;
        }

        boolean canUseTenant =
                currentSession.hasRole(UserRole.TENANT);

        boolean canUseHost =
                currentSession.hasRole(UserRole.HOST);

        boolean hasAvailableMode = canUseTenant || canUseHost;

        binding.layoutProfileMode.setVisibility(
                hasAvailableMode ? View.VISIBLE : View.GONE
        );

        binding.btnModeTenant.setVisibility(
                canUseTenant ? View.VISIBLE : View.GONE
        );

        binding.btnModeHost.setVisibility(
                canUseHost ? View.VISIBLE : View.GONE
        );

        binding.btnModeTenant.setEnabled(canUseTenant);
        binding.btnModeHost.setEnabled(canUseHost);

        if (!hasAvailableMode) {
            binding.tvProfileCurrentMode.setText("");
            return;
        }

        boolean tenantSelected =
                canUseTenant && currentMode == AppMode.TENANT;

        boolean hostSelected =
                canUseHost && currentMode == AppMode.HOST;

        updateModeButton(
                binding.btnModeTenant,
                tenantSelected
        );

        updateModeButton(
                binding.btnModeHost,
                hostSelected
        );

        binding.tvProfileCurrentMode.setText(
                currentMode == AppMode.HOST
                        ? "Đang sử dụng chế độ Chủ trọ"
                        : "Đang sử dụng chế độ Người thuê"
        );
    }

    /**
     * Đổi màu nút và mô tả để thể hiện chế độ đang được chọn.
     */
    private void updateModeButton(
            MaterialButton button,
            boolean selected
    ) {
        int backgroundColor = ContextCompat.getColor(
                requireContext(),
                selected
                        ? R.color.roomly_primary
                        : R.color.roomly_surface
        );

        int textColor = ContextCompat.getColor(
                requireContext(),
                selected
                        ? R.color.roomly_surface
                        : R.color.roomly_primary
        );

        button.setBackgroundTintList(
                ColorStateList.valueOf(backgroundColor)
        );

        button.setTextColor(textColor);
        button.setSelected(selected);

        button.setContentDescription(
                button.getText().toString()
                        + (selected ? ", đang chọn" : "")
        );
    }

    /**
     * Yêu cầu repository đổi chế độ sau khi kiểm tra vai trò.
     * Không sửa vai trò hoặc token đăng nhập.
     */
    private void selectMode(AppMode requestedMode) {
        if (binding == null) {
            return;
        }

        // Đọc phiên mới nhất thay vì chỉ dựa vào dữ liệu đang hiển thị.
        currentSession = SessionRepository.getInstance()
                .getCurrentSession();

        if (!currentSession.isLoggedIn()) {
            renderSession(currentSession);
            openLoginScreen();
            return;
        }

        boolean changed = viewModel.selectMode(requestedMode);

        if (!changed) {
            renderSession(currentSession);

            Toast.makeText(
                    requireContext(),
                    "Tài khoản chưa có quyền sử dụng chế độ này.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        currentMode = viewModel.getCurrentMode();

        renderModeOptions();
    }

    /**
     * Mở Đăng nhập và giữ trang Cá nhân trong back stack.
     */
    private void openLoginScreen() {
        if (binding == null) {
            return;
        }

        getParentFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                        R.id.fragment_container,
                        new LoginFragment()
                )
                .addToBackStack(null)
                .commit();
    }

    /**
     * Mở xác minh cho tài khoản đã đăng nhập nhưng chưa xác minh email.
     */
    private void openVerifyEmailScreen() {
        if (binding == null) {
            return;
        }

        currentSession = SessionRepository.getInstance()
                .getCurrentSession();

        if (!currentSession.isLoggedIn()) {
            openLoginScreen();
            return;
        }

        if (currentSession.isEmailVerified()) {
            renderSession(currentSession);
            return;
        }

        getParentFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                        R.id.fragment_container,
                        VerifyEmailFragment.newInstance(
                                currentSession.getEmail()
                        )
                )
                .addToBackStack(null)
                .commit();
    }

    /**
     * Kiểm tra đăng nhập và xác minh trước khi chỉnh sửa hồ sơ.
     * Chức năng cập nhật hồ sơ thật sẽ được nối với API sau.
     */
    private void handleEditProfile() {
        currentSession = SessionRepository.getInstance()
                .getCurrentSession();

        if (!currentSession.isLoggedIn()) {
            openLoginScreen();
            return;
        }

        if (!currentSession.isEmailVerified()) {
            openVerifyEmailScreen();
            return;
        }

        Toast.makeText(
                requireContext(),
                "Chưa kết nối dịch vụ cập nhật hồ sơ.",
                Toast.LENGTH_SHORT
        ).show();
    }

    /**
     * Gỡ sự kiện và giải phóng Binding khi giao diện bị hủy.
     * Observer tự được gỡ theo vòng đời giao diện.
     */
    @Override
    public void onDestroyView() {
        if (binding != null) {
            binding.btnOpenLogin.setOnClickListener(null);
            binding.btnProfileVerifyEmail.setOnClickListener(null);
            binding.btnEditProfile.setOnClickListener(null);
            binding.btnModeTenant.setOnClickListener(null);
            binding.btnModeHost.setOnClickListener(null);
            binding.btnEnableDemoSession.setOnClickListener(null);
            binding.btnDisableDemoSession.setOnClickListener(null);
        }

        binding = null;

        super.onDestroyView();
    }
}