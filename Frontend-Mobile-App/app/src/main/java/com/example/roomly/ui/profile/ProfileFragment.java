package com.example.roomly.ui.profile;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Hiển thị hồ sơ, chế độ sử dụng và các lựa chọn thử giao diện.
 */
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;
    private AlertDialog demoDialog;

    private SessionState currentSession = SessionState.guest();
    private AppMode currentMode = AppMode.TENANT;

    /** Khởi tạo ViewModel quản lý phiên và chế độ giao diện. */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this)
                .get(ProfileViewModel.class);
    }

    /** Tạo giao diện Cá nhân bằng ViewBinding. */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentProfileBinding.inflate(
                inflater, container, false
        );
        return binding.getRoot();
    }

    /** Đăng ký thao tác và quan sát phiên theo vòng đời giao diện. */
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

    /** Gắn thao tác cho các nút tài khoản, chế độ và phiên thử. */
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
        binding.btnEnableDemoSession.setOnClickListener(
                view -> showDemoAccountDialog()
        );

        binding.btnEnableDemoAdminSession.setOnClickListener(view -> {
            if (!DebugSessionHelper.enableDemoAdminSession()) {
                showMessage(
                        "Hãy tắt phiên thử hiện tại trước khi bật admin thử."
                );
            }
        });

        binding.btnDisableDemoSession.setOnClickListener(
                view -> DebugSessionHelper.disableDemoSession()
        );
    }

    /** Cho khách chọn tài khoản thử trong bản debug. */
    private void showDemoAccountDialog() {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!BuildConfig.DEBUG || session.isLoggedIn()
                || demoDialog != null) {
            return;
        }

        String[] choices = {
                "Tài khoản Chủ trọ / Người thuê",
                "Tài khoản Người thuê riêng"
        };

        AlertDialog dialog =
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Chọn tài khoản thử")
                        .setItems(choices, (interfaceDialog, which) -> {
                            boolean enabled = which == 0
                                    ? DebugSessionHelper.enableDemoSession()
                                    : DebugSessionHelper
                                    .enableDemoTenantSession();

                            if (!enabled) {
                                showMessage(
                                        "Không thể bật phiên thử. "
                                                + "Hãy kiểm tra tài khoản hiện tại."
                                );
                            }
                        })
                        .setNegativeButton("Đóng", null)
                        .create();

        demoDialog = dialog;

        dialog.setOnDismissListener(dismissed -> {
            if (demoDialog == dialog) {
                demoDialog = null;
            }
        });

        dialog.show();
    }

    /** Cập nhật thông tin tài khoản và quyền hiển thị các nút. */
    private void renderSession(@Nullable SessionState session) {
        if (binding == null) {
            return;
        }

        currentSession = session == null
                ? SessionState.guest() : session;

        boolean loggedIn = currentSession.isLoggedIn();

        if (loggedIn) {
            closeDemoDialog();
        }

        binding.layoutProfileGuest.setVisibility(
                loggedIn ? View.GONE : View.VISIBLE
        );
        binding.layoutProfileAccount.setVisibility(
                loggedIn ? View.VISIBLE : View.GONE
        );

        binding.tvProfileName.setText(currentSession.getFullName());
        binding.tvProfileEmail.setText(currentSession.getEmail());
        binding.tvProfilePhone.setText("");
        binding.tvProfilePhone.setVisibility(View.GONE);

        binding.btnProfileAdmin.setVisibility(
                loggedIn && currentSession.hasRole(UserRole.ADMIN)
                        ? View.VISIBLE : View.GONE
        );

        boolean canEnableDemo = BuildConfig.DEBUG && !loggedIn;

        binding.btnEnableDemoSession.setText("Chọn tài khoản thử");
        binding.btnEnableDemoSession.setVisibility(
                canEnableDemo ? View.VISIBLE : View.GONE
        );
        binding.btnEnableDemoAdminSession.setVisibility(
                canEnableDemo ? View.VISIBLE : View.GONE
        );
        binding.btnDisableDemoSession.setVisibility(
                DebugSessionHelper.isDemoSession()
                        ? View.VISIBLE : View.GONE
        );

        binding.tvProfileVerification.setText(
                !loggedIn ? ""
                        : currentSession.isEmailVerified()
                          ? "Email đã được xác minh"
                          : "Email chưa được xác minh"
        );

        binding.btnProfileVerifyEmail.setVisibility(
                loggedIn && !currentSession.isEmailVerified()
                        ? View.VISIBLE : View.GONE
        );
        binding.btnEditProfile.setEnabled(loggedIn);

        currentMode = viewModel.getCurrentMode();
        renderModeOptions();
    }

    /** Nhận chế độ mới và cập nhật bộ chọn chế độ. */
    private void renderMode(@Nullable AppMode mode) {
        currentMode = mode == null ? AppMode.TENANT : mode;
        renderModeOptions();
    }

    /** Chỉ hiển thị chế độ tương ứng với vai trò tài khoản. */
    private void renderModeOptions() {
        if (binding == null) {
            return;
        }

        boolean tenant = currentSession.isLoggedIn()
                && currentSession.hasRole(UserRole.TENANT);
        boolean host = currentSession.isLoggedIn()
                && currentSession.hasRole(UserRole.HOST);

        binding.layoutProfileMode.setVisibility(
                tenant || host ? View.VISIBLE : View.GONE
        );
        binding.btnModeTenant.setVisibility(
                tenant ? View.VISIBLE : View.GONE
        );
        binding.btnModeHost.setVisibility(
                host ? View.VISIBLE : View.GONE
        );

        binding.btnModeTenant.setEnabled(tenant);
        binding.btnModeHost.setEnabled(host);

        if (!tenant && !host) {
            binding.tvProfileCurrentMode.setText("");
            return;
        }

        updateModeButton(
                binding.btnModeTenant,
                tenant && currentMode == AppMode.TENANT
        );
        updateModeButton(
                binding.btnModeHost,
                host && currentMode == AppMode.HOST
        );

        binding.tvProfileCurrentMode.setText(
                currentMode == AppMode.HOST
                        ? "Đang sử dụng chế độ Chủ trọ"
                        : "Đang sử dụng chế độ Người thuê"
        );
    }

    /** Đổi màu và mô tả nút theo trạng thái đang chọn. */
    private void updateModeButton(
            MaterialButton button,
            boolean selected
    ) {
        button.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(
                        requireContext(),
                        selected ? R.color.roomly_primary
                                : R.color.roomly_surface
                )
        ));

        button.setTextColor(ContextCompat.getColor(
                requireContext(),
                selected ? R.color.roomly_surface
                        : R.color.roomly_primary
        ));

        button.setSelected(selected);
        button.setContentDescription(
                button.getText() + (selected ? ", đang chọn" : "")
        );
    }

    /** Đổi chế độ sau khi kiểm tra phiên và vai trò. */
    private void selectMode(AppMode requestedMode) {
        currentSession = SessionRepository.getInstance()
                .getCurrentSession();

        if (!currentSession.isLoggedIn()) {
            openLoginScreen();
            return;
        }

        if (!viewModel.selectMode(requestedMode)) {
            renderSession(currentSession);
            showMessage("Tài khoản chưa có quyền sử dụng chế độ này.");
            return;
        }

        currentMode = viewModel.getCurrentMode();
        renderModeOptions();
    }

    /** Mở màn hình Đăng nhập. */
    private void openLoginScreen() {
        openScreen(new LoginFragment());
    }

    /** Mở xác minh email nếu tài khoản chưa xác minh. */
    private void openVerifyEmailScreen() {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()) {
            openLoginScreen();
            return;
        }

        if (!session.isEmailVerified()) {
            openScreen(
                    VerifyEmailFragment.newInstance(session.getEmail())
            );
        }
    }

    /** Kiểm tra phiên trước khi sử dụng chức năng chỉnh sửa hồ sơ. */
    private void handleEditProfile() {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()) {
            openLoginScreen();
        } else if (!session.isEmailVerified()) {
            openVerifyEmailScreen();
        } else {
            showMessage("Chưa kết nối dịch vụ cập nhật hồ sơ.");
        }
    }

    /** Mở trang quản trị khi phiên hiện tại có quyền ADMIN. */
    private void openAdminDashboard() {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (session.isLoggedIn() && session.hasRole(UserRole.ADMIN)) {
            openScreen(new AdminDashboardFragment());
        } else {
            renderSession(session);
        }
    }

    /** Mở màn hình mới và giữ trang Cá nhân trong back stack. */
    private void openScreen(Fragment fragment) {
        if (binding == null
                || getParentFragmentManager().isStateSaved()) {
            return;
        }

        closeDemoDialog();

        getParentFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    /** Hiển thị thông báo ngắn khi Fragment còn được gắn. */
    private void showMessage(String message) {
        if (isAdded()) {
            Toast.makeText(
                    requireContext(), message, Toast.LENGTH_LONG
            ).show();
        }
    }

    /** Đóng hộp thoại chọn tài khoản thử. */
    private void closeDemoDialog() {
        if (demoDialog != null) {
            AlertDialog dialog = demoDialog;
            demoDialog = null;
            dialog.setOnDismissListener(null);
            dialog.dismiss();
        }
    }

    /** Đóng hộp thoại và giải phóng các tham chiếu giao diện. */
    @Override
    public void onDestroyView() {
        closeDemoDialog();

        if (binding != null) {
            binding.btnOpenLogin.setOnClickListener(null);
            binding.btnProfileVerifyEmail.setOnClickListener(null);
            binding.btnEditProfile.setOnClickListener(null);
            binding.btnProfileAdmin.setOnClickListener(null);
            binding.btnModeTenant.setOnClickListener(null);
            binding.btnModeHost.setOnClickListener(null);
            binding.btnEnableDemoSession.setOnClickListener(null);
            binding.btnEnableDemoAdminSession.setOnClickListener(null);
            binding.btnDisableDemoSession.setOnClickListener(null);
        }

        binding = null;
        super.onDestroyView();
    }
}