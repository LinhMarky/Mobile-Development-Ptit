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
import com.example.roomly.data.repository.DemoProfileRepository;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentProfileBinding;
import com.example.roomly.ui.admin.AdminDashboardFragment;
import com.example.roomly.ui.auth.LoginFragment;
import com.example.roomly.ui.auth.VerifyEmailFragment;
import com.example.roomly.ui.booking.BookingsFragment;
import com.example.roomly.ui.booking.HostBookingsFragment;
import com.example.roomly.ui.notification.NotificationsFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;
    private AlertDialog demoDialog;

    private SessionState currentSession = SessionState.guest();
    private AppMode currentMode = AppMode.TENANT;
    private boolean openingScreen;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(this)
                .get(ProfileViewModel.class);
    }

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

    @Override
    public void onResume() {
        super.onResume();

        openingScreen = false;

        renderSession(
                SessionRepository.getInstance().getCurrentSession()
        );
    }

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

        binding.btnProfileBookings.setOnClickListener(
                view -> openBookingsScreen()
        );

        binding.btnProfileNotifications.setOnClickListener(
                view -> openNotificationsScreen()
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

    private void showDemoAccountDialog() {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!BuildConfig.DEBUG
                || session.isLoggedIn()
                || demoDialog != null) {
            return;
        }

        String[] choices = {
                "Tài khoản Chủ trọ / Người thuê",
                "Tài khoản Người thuê riêng"
        };

        AlertDialog dialog = new MaterialAlertDialogBuilder(
                requireContext()
        )
                .setTitle("Chọn tài khoản thử")
                .setItems(choices, (interfaceDialog, which) -> {
                    boolean enabled = which == 0
                            ? DebugSessionHelper.enableDemoSession()
                            : DebugSessionHelper.enableDemoTenantSession();

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

    private void renderSession(@Nullable SessionState session) {
        if (binding == null) {
            return;
        }

        currentSession = session == null
                ? SessionState.guest()
                : session;

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

        String phone = DemoProfileRepository.getInstance().getPhone();

        binding.tvProfilePhone.setText(phone);
        binding.tvProfilePhone.setVisibility(
                loggedIn && !phone.isEmpty()
                        ? View.VISIBLE
                        : View.GONE
        );

        binding.btnProfileAdmin.setVisibility(
                loggedIn && currentSession.hasRole(UserRole.ADMIN)
                        ? View.VISIBLE
                        : View.GONE
        );

        binding.btnProfileNotifications.setVisibility(
                loggedIn ? View.VISIBLE : View.GONE
        );

        binding.btnProfileNotifications.setEnabled(
                loggedIn && !openingScreen
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
                        ? View.VISIBLE
                        : View.GONE
        );

        binding.tvProfileVerification.setText(
                !loggedIn
                        ? ""
                        : currentSession.isEmailVerified()
                          ? "Email đã được xác minh"
                          : "Email chưa được xác minh"
        );

        binding.btnProfileVerifyEmail.setVisibility(
                loggedIn && !currentSession.isEmailVerified()
                        ? View.VISIBLE
                        : View.GONE
        );

        binding.btnEditProfile.setEnabled(loggedIn);

        currentMode = viewModel.getCurrentMode();
        renderModeOptions();
    }

    private void renderMode(@Nullable AppMode mode) {
        currentMode = mode == null ? AppMode.TENANT : mode;
        renderModeOptions();
    }

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

        boolean hostBookings = host
                && (currentMode == AppMode.HOST || !tenant);

        binding.btnProfileBookings.setVisibility(
                tenant || host ? View.VISIBLE : View.GONE
        );

        binding.btnProfileBookings.setEnabled(tenant || host);

        binding.btnProfileBookings.setText(
                hostBookings
                        ? "Yêu cầu thuê nhận được"
                        : "Yêu cầu thuê của tôi"
        );

        if (!tenant && !host) {
            binding.tvProfileCurrentMode.setText("");
            return;
        }

        updateModeButton(
                binding.btnModeTenant,
                tenant && !hostBookings
        );

        updateModeButton(
                binding.btnModeHost,
                hostBookings
        );

        binding.tvProfileCurrentMode.setText(
                hostBookings
                        ? "Đang sử dụng chế độ Chủ trọ"
                        : "Đang sử dụng chế độ Người thuê"
        );
    }

    private void updateModeButton(
            MaterialButton button,
            boolean selected
    ) {
        button.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(
                        requireContext(),
                        selected
                                ? R.color.roomly_primary
                                : R.color.roomly_surface
                )
        ));

        button.setTextColor(ContextCompat.getColor(
                requireContext(),
                selected
                        ? R.color.roomly_surface
                        : R.color.roomly_primary
        ));

        button.setSelected(selected);
        button.setContentDescription(
                button.getText() + (selected ? ", đang chọn" : "")
        );
    }

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

    private void openBookingsScreen() {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()) {
            openLoginScreen();
            return;
        }

        boolean tenant = session.hasRole(UserRole.TENANT);
        boolean host = session.hasRole(UserRole.HOST);

        currentMode = viewModel.getCurrentMode();

        if (host && (currentMode == AppMode.HOST || !tenant)) {
            openScreen(new HostBookingsFragment());
        } else if (tenant) {
            openScreen(new BookingsFragment());
        } else {
            renderSession(session);
            showMessage(
                    "Tài khoản chưa có quyền quản lý yêu cầu thuê."
            );
        }
    }

    private void openNotificationsScreen() {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()) {
            openLoginScreen();
            return;
        }

        openScreen(new NotificationsFragment());
    }

    private void openLoginScreen() {
        openScreen(new LoginFragment());
    }

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

    private void handleEditProfile() {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()) {
            openLoginScreen();
            return;
        }

        if (!session.isEmailVerified()) {
            openVerifyEmailScreen();
            return;
        }

        if (binding == null
                || openingScreen
                || getChildFragmentManager().isStateSaved()
                || getChildFragmentManager().findFragmentByTag(
                EditProfileDialogFragment.TAG
        ) != null) {
            return;
        }

        EditProfileDialogFragment.newInstance(session.getUserId())
                .showNow(
                        getChildFragmentManager(),
                        EditProfileDialogFragment.TAG
                );
    }

    private void openAdminDashboard() {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (session.isLoggedIn() && session.hasRole(UserRole.ADMIN)) {
            openScreen(new AdminDashboardFragment());
        } else {
            renderSession(session);
        }
    }

    private void openScreen(Fragment fragment) {
        if (binding == null
                || openingScreen
                || getParentFragmentManager().isStateSaved()) {
            return;
        }

        openingScreen = true;
        binding.btnProfileNotifications.setEnabled(false);

        closeDemoDialog();

        getParentFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void showMessage(String message) {
        if (isAdded()) {
            Toast.makeText(
                    requireContext(),
                    message,
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void closeDemoDialog() {
        if (demoDialog != null) {
            AlertDialog dialog = demoDialog;
            demoDialog = null;

            dialog.setOnDismissListener(null);
            dialog.dismiss();
        }
    }

    @Override
    public void onDestroyView() {
        closeDemoDialog();

        if (binding != null) {
            binding.btnOpenLogin.setOnClickListener(null);
            binding.btnProfileVerifyEmail.setOnClickListener(null);
            binding.btnEditProfile.setOnClickListener(null);
            binding.btnProfileBookings.setOnClickListener(null);
            binding.btnProfileNotifications.setOnClickListener(null);
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