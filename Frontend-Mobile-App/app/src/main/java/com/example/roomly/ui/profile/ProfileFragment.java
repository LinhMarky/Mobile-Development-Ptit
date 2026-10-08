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

import com.example.roomly.BuildConfig;
import com.example.roomly.R;
import com.example.roomly.data.model.AppMode;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;
import com.example.roomly.data.repository.DebugSessionHelper;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentProfileBinding;
import com.example.roomly.ui.admin.AdminDashboardFragment;
import com.example.roomly.ui.auth.LoginFragment;
import com.example.roomly.ui.auth.VerifyEmailFragment;
import com.google.android.material.button.MaterialButton;

/**
 * Hiển thị trang Cá nhân theo phiên đăng nhập.
 * Cho phép chọn chế độ theo vai trò và mở quản trị khi có quyền ADMIN.
 */
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;

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
     * Đăng ký thao tác và quan sát phiên, chế độ theo vòng đời giao diện.
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
     * Đăng ký đăng nhập, xác minh, chỉnh sửa, quản trị và chọn chế độ.
     * Giữ các nút phiên mẫu để thử giao diện trong bản debug.
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

        binding.btnProfileAdmin.setOnClickListener(
                view -> openAdminDashboard()
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

        // Bật phiên admin mẫu khi đang là khách trong bản debug.
        binding.btnEnableDemoAdminSession.setOnClickListener(view -> {
            boolean enabled = DebugSessionHelper.enableDemoAdminSession();

            if (!enabled) {
                Toast.makeText(
                        requireContext(),
                        "Hãy tắt phiên thử hiện tại trước khi bật admin thử.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        binding.btnDisableDemoSession.setOnClickListener(
                view -> DebugSessionHelper.disableDemoSession()
        );
    }

    /**
     * Hiển thị thông tin tài khoản và các chức năng theo quyền hiện tại.
     * Chỉ hiện nút quản trị khi tài khoản đã đăng nhập và có quyền ADMIN.
     */
    private void renderSession(@Nullable SessionState session) {
        if (binding == null) {
            return;
        }

        currentSession = session == null
                ? SessionState.guest()
                : session;

        boolean loggedIn = currentSession.isLoggedIn();

        boolean isAdmin = loggedIn
                && currentSession.hasRole(UserRole.ADMIN);

        binding.btnProfileAdmin.setVisibility(
                isAdmin ? View.VISIBLE : View.GONE
        );

        // Chỉ cho khách tạo phiên mẫu trong bản debug.
        boolean canEnableDemo = BuildConfig.DEBUG && !loggedIn;

        binding.btnEnableDemoSession.setVisibility(
                canEnableDemo ? View.VISIBLE : View.GONE
        );

        binding.btnEnableDemoAdminSession.setVisibility(
                canEnableDemo ? View.VISIBLE : View.GONE
        );

// Nút tắt dùng được cho cả phiên Người thuê/Chủ trọ và Admin mẫu.
        binding.btnDisableDemoSession.setVisibility(
                DebugSessionHelper.isDemoSession()
                        ? View.VISIBLE
                        : View.GONE
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
     * Nhận chế độ mới và cập nhật các nút lựa chọn.
     */
    private void renderMode(@Nullable AppMode mode) {
        currentMode = mode == null ? AppMode.TENANT : mode;

        renderModeOptions();
    }

    /**
     * Chỉ hiện chế độ mà tài khoản có vai trò tương ứng.
     * Tài khoản chỉ có ADMIN không thấy bộ chọn Người thuê/Chủ trọ.
     */
    private void renderModeOptions() {
        if (binding == null) {
            return;
        }

        boolean loggedIn = currentSession.isLoggedIn();

        boolean canUseTenant = loggedIn
                && currentSession.hasRole(UserRole.TENANT);

        boolean canUseHost = loggedIn
                && currentSession.hasRole(UserRole.HOST);

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

        updateModeButton(binding.btnModeTenant, tenantSelected);
        updateModeButton(binding.btnModeHost, hostSelected);

        binding.tvProfileCurrentMode.setText(
                currentMode == AppMode.HOST
                        ? "Đang sử dụng chế độ Chủ trọ"
                        : "Đang sử dụng chế độ Người thuê"
        );
    }

    /**
     * Đổi màu và mô tả nút để thể hiện chế độ đang chọn.
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
     * Việc đổi chế độ không thay đổi quyền của tài khoản.
     */
    private void selectMode(AppMode requestedMode) {
        if (binding == null) {
            return;
        }

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
        if (binding == null) {
            return;
        }

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
     * Kiểm tra lại quyền ADMIN và mở trang quản trị.
     * Giữ trang Cá nhân trong back stack để có thể quay lại.
     */
    private void openAdminDashboard() {
        if (binding == null) {
            return;
        }

        currentSession = SessionRepository.getInstance()
                .getCurrentSession();

        if (!currentSession.isLoggedIn()
                || !currentSession.hasRole(UserRole.ADMIN)) {
            renderSession(currentSession);
            return;
        }

        getParentFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                        R.id.fragment_container,
                        new AdminDashboardFragment()
                )
                .addToBackStack(null)
                .commit();
    }

    /**
     * Gỡ sự kiện và giải phóng binding khi giao diện bị hủy.
     * Observer tự được gỡ theo vòng đời giao diện.
     */
    @Override
    public void onDestroyView() {
        if (binding != null) {
            binding.btnOpenLogin.setOnClickListener(null);
            binding.btnProfileVerifyEmail.setOnClickListener(null);
            binding.btnEditProfile.setOnClickListener(null);
            binding.btnProfileAdmin.setOnClickListener(null);
            binding.btnModeTenant.setOnClickListener(null);
            binding.btnModeHost.setOnClickListener(null);
            binding.btnEnableDemoSession.setOnClickListener(null);
            binding.btnDisableDemoSession.setOnClickListener(null);
            binding.btnEnableDemoAdminSession.setOnClickListener(null);
        }

        binding = null;

        super.onDestroyView();
    }
}