package com.example.roomly;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.roomly.data.model.AppMode;
import com.example.roomly.data.repository.AppModeRepository;
import com.example.roomly.databinding.ActivityMainBinding;
import com.example.roomly.ui.auth.ForgotPasswordFragment;
import com.example.roomly.ui.auth.LoginFragment;
import com.example.roomly.ui.auth.RegisterFragment;
import com.example.roomly.ui.auth.VerifyEmailFragment;
import com.example.roomly.ui.detail.RoomDetailFragment;
import com.example.roomly.ui.explore.ExploreFragment;
import com.example.roomly.ui.host.HostEditListingFragment;
import com.example.roomly.ui.host.HostRoomsFragment;
import com.example.roomly.ui.messages.ChatFragment;
import com.example.roomly.ui.messages.MessagesFragment;
import com.example.roomly.ui.profile.ProfileFragment;
import com.example.roomly.ui.saved.SavedFragment;
import com.example.roomly.ui.schedule.ScheduleFragment;
import com.example.roomly.ui.host.HostAddRoomFragment;
import com.example.roomly.ui.host.HostRoomDetailFragment;
import com.example.roomly.ui.host.HostEditRoomFragment;
import com.example.roomly.ui.host.HostCreateListingFragment;
import com.example.roomly.ui.host.HostListingsFragment;
import com.example.roomly.ui.host.HostListingDetailFragment;
import com.example.roomly.ui.host.viewing.HostViewingFragment;
import com.example.roomly.ui.host.viewing.HostAppointmentsFragment;
import com.example.roomly.ui.admin.AdminDashboardFragment;
import com.example.roomly.ui.admin.AdminListingsFragment;
import com.example.roomly.ui.admin.AdminListingDetailFragment;
import com.example.roomly.ui.admin.AdminReportsFragment;
import com.example.roomly.ui.admin.AdminReportDetailFragment;
import com.example.roomly.ui.admin.AdminAccountsFragment;
import com.example.roomly.ui.admin.AdminMetricsFragment;
import com.example.roomly.ui.booking.BookingCreateFragment;
import com.example.roomly.ui.booking.BookingsFragment;
import com.example.roomly.ui.booking.BookingDetailFragment;
import com.example.roomly.ui.booking.BookingPaymentFragment;
import com.example.roomly.ui.booking.BookingHandoverFragment;
import com.example.roomly.ui.booking.BookingReviewFragment;
import com.example.roomly.ui.booking.HostBookingsFragment;
import com.example.roomly.ui.booking.HostBookingDetailFragment;
import com.example.roomly.ui.notification.NotificationsFragment;

/**
 * Điều khiển màn hình chính và thanh điều hướng theo chế độ sử dụng.
 * Chế độ chỉ thay đổi giao diện; quyền vẫn được kiểm tra theo phiên.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    // Chế độ tương ứng với menu đang hiển thị.
    private AppMode displayedMode;

    // Chặn sự kiện chọn tab khi đang thay menu bằng code.
    private boolean updatingNavigation;

    /**
     * Khởi tạo giao diện, điều hướng và quan sát chế độ sử dụng.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupWindowInsets();
        setupBottomNavAppearance();
        setupBottomNavigation();

        getSupportFragmentManager().addOnBackStackChangedListener(
                this::updateBottomNavVisibility
        );

        // Cài menu đúng chế độ trước khi nhận thao tác của người dùng.
        AppMode initialMode = AppModeRepository.getInstance()
                .getCurrentMode();

        installNavigationMenu(initialMode);

        if (savedInstanceState == null) {
            showRootScreen(getDefaultTab(initialMode));
        }

        observeAppMode();
        updateBottomNavVisibility();
    }

    /**
     * Thêm khoảng trống để thanh hệ thống không che giao diện.
     */
    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(
                binding.getRoot(),
                (view, insets) -> {
                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    view.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );

        ViewCompat.requestApplyInsets(binding.getRoot());
    }

    /**
     * Đặt nền xanh nhạt cho tab đang chọn.
     */
    private void setupBottomNavAppearance() {
        binding.bottomNav.setItemActiveIndicatorColor(
                ColorStateList.valueOf(
                        ContextCompat.getColor(
                                this,
                                R.color.roomly_primary_light
                        )
                )
        );
    }

    /**
     * Quan sát chế độ và xử lý sau khi các cập nhật phiên hoàn tất.
     * Đưa việc đổi giao diện vào hàng đợi để tránh giao dịch Fragment
     * ngay trong lúc Fragment khác đang được khởi tạo.
     */
    private void observeAppMode() {
        AppModeRepository.getInstance()
                .getMode()
                .observe(this, mode -> {
                    binding.getRoot().post(() -> {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        applyCurrentMode();
                    });
                });
    }

    /**
     * Đồng bộ lại chế độ khi Activity có thể thực hiện giao dịch Fragment.
     * Nếu ứng dụng vừa ở nền, thay đổi sẽ được áp dụng khi quay lại.
     */
    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();

        applyCurrentMode();
        updateBottomNavVisibility();
    }

    /**
     * Đổi menu khi chế độ thay đổi.
     * Giữ trang Cá nhân nếu đang chọn chế độ tại trang này.
     * Những màn hình khác chuyển về tab đầu tiên của chế độ mới.
     */
    private void applyCurrentMode() {
        if (binding == null
                || getSupportFragmentManager().isStateSaved()) {
            return;
        }

        AppMode nextMode = AppModeRepository.getInstance()
                .getCurrentMode();

        if (nextMode == displayedMode) {
            return;
        }

        Fragment currentFragment = getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container);

        boolean keepProfile = currentFragment instanceof ProfileFragment;

        // Xóa màn hình con của chế độ cũ khỏi lịch sử quay lại.
        getSupportFragmentManager().popBackStackImmediate(
                null,
                FragmentManager.POP_BACK_STACK_INCLUSIVE
        );

        installNavigationMenu(nextMode);

        int nextTab = keepProfile
                ? R.id.nav_profile
                : getDefaultTab(nextMode);

        showRootScreen(nextTab);
    }

    /**
     * Thay menu theo chế độ và giữ lựa chọn tab chung nếu có.
     * Không phát sinh thao tác chuyển Fragment trong lúc thay menu.
     */
    private void installNavigationMenu(AppMode mode) {
        int previousTab = binding.bottomNav.getSelectedItemId();

        updatingNavigation = true;

        binding.bottomNav.getMenu().clear();

        binding.bottomNav.inflateMenu(
                mode == AppMode.HOST
                        ? R.menu.bottom_nav_host_menu
                        : R.menu.menu_bottom_nav
        );

        displayedMode = mode;

        int selectedTab =
                binding.bottomNav.getMenu().findItem(previousTab) != null
                        ? previousTab
                        : getDefaultTab(mode);

        binding.bottomNav.setSelectedItemId(selectedTab);

        updatingNavigation = false;
    }

    /**
     * Trả về tab đầu tiên của từng chế độ.
     */
    private int getDefaultTab(AppMode mode) {
        return mode == AppMode.HOST
                ? R.id.nav_host_rooms
                : R.id.nav_explore;
    }

    /**
     * Xử lý chọn tab và giữ nguyên màn hình khi bấm lại tab hiện tại.
     */
    private void setupBottomNavigation() {
        binding.bottomNav.setOnItemSelectedListener(item -> {
            if (updatingNavigation) {
                return true;
            }

            if (getSupportFragmentManager().isStateSaved()) {
                return false;
            }

            Fragment nextFragment = createTabFragment(item.getItemId());

            if (nextFragment == null) {
                return false;
            }

            replaceRootFragment(nextFragment);
            return true;
        });

        binding.bottomNav.setOnItemReselectedListener(item -> {
            // Giữ nguyên Fragment và dữ liệu đang hiển thị.
        });
    }

    /**
     * Tạo Fragment tương ứng với tab và chế độ hiện tại.
     * Các tab người thuê không được mở bằng menu chủ trọ.
     */
    private Fragment createTabFragment(int itemId) {
        if (itemId == R.id.nav_profile) {
            return new ProfileFragment();
        }

        if (itemId == R.id.nav_messages) {
            return new MessagesFragment();
        }

        if (displayedMode == AppMode.HOST) {
            if (itemId == R.id.nav_host_rooms) {
                return new HostRoomsFragment();
            }

            if (itemId == R.id.nav_host_listings) {
                return new HostListingsFragment();
            }

            return null;
        }

        if (itemId == R.id.nav_explore) {
            return new ExploreFragment();
        }

        if (itemId == R.id.nav_saved) {
            return new SavedFragment();
        }

        if (itemId == R.id.nav_schedule) {
            return new ScheduleFragment();
        }

        return null;
    }

    /**
     * Chọn tab bằng code rồi mở màn hình tương ứng một lần.
     */
    private void showRootScreen(int itemId) {
        Fragment fragment = createTabFragment(itemId);

        if (fragment == null) {
            return;
        }

        updatingNavigation = true;
        binding.bottomNav.setSelectedItemId(itemId);
        updatingNavigation = false;

        replaceRootFragment(fragment);
    }

    /**
     * Thay nội dung tab chính và cập nhật menu khi giao dịch hoàn tất.
     * Tab chính không được thêm vào back stack.
     */
    private void replaceRootFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, fragment)
                .runOnCommit(this::updateBottomNavVisibility)
                .commit();
    }

    /**
     * Ẩn menu ở màn hình chi tiết, chat và các màn hình xác thực.
     * Đồng bộ tab đang chọn khi Android khôi phục Fragment.
     */
    private void updateBottomNavVisibility() {
        if (binding == null) {
            return;
        }

        Fragment currentFragment = getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container);

        boolean hideBottomNav =
                currentFragment instanceof RoomDetailFragment
                        || currentFragment instanceof ChatFragment
                        || currentFragment instanceof LoginFragment
                        || currentFragment instanceof RegisterFragment
                        || currentFragment instanceof ForgotPasswordFragment
                        || currentFragment instanceof VerifyEmailFragment
                        || currentFragment instanceof HostAddRoomFragment
                        || currentFragment instanceof HostRoomDetailFragment
                        || currentFragment instanceof HostEditRoomFragment
                        || currentFragment instanceof HostCreateListingFragment
                        || currentFragment instanceof HostListingDetailFragment
                        || currentFragment instanceof HostEditListingFragment
                        || currentFragment instanceof HostViewingFragment
                        || currentFragment instanceof HostAppointmentsFragment
                        || currentFragment instanceof AdminDashboardFragment
                        || currentFragment instanceof AdminListingsFragment
                        || currentFragment instanceof AdminListingDetailFragment
                        || currentFragment instanceof AdminReportsFragment
                        || currentFragment instanceof AdminReportDetailFragment
                        || currentFragment instanceof AdminAccountsFragment
                        || currentFragment instanceof AdminMetricsFragment
                        || currentFragment instanceof BookingCreateFragment
                        || currentFragment instanceof BookingsFragment
                        || currentFragment instanceof BookingDetailFragment
                        || currentFragment instanceof BookingPaymentFragment
                        || currentFragment instanceof BookingHandoverFragment
                        || currentFragment instanceof BookingReviewFragment
                        || currentFragment instanceof HostBookingsFragment
                        || currentFragment instanceof HostBookingDetailFragment
                        || currentFragment instanceof NotificationsFragment;

        binding.bottomNav.setVisibility(
                hideBottomNav ? View.GONE : View.VISIBLE
        );

        if (!hideBottomNav) {
            syncSelectedTab(currentFragment);
        }
    }

    /**
     * Đánh dấu tab phù hợp với Fragment mà không tạo lại màn hình.
     */
    private void syncSelectedTab(Fragment fragment) {
        int itemId = 0;

        if (fragment instanceof ProfileFragment) {
            itemId = R.id.nav_profile;

        } else if (fragment instanceof MessagesFragment) {
            itemId = R.id.nav_messages;

        } else if (fragment instanceof HostRoomsFragment) {
            itemId = R.id.nav_host_rooms;

        } else if (fragment instanceof HostListingsFragment) {
            itemId = R.id.nav_host_listings;

        } else if (fragment instanceof ExploreFragment) {
            itemId = R.id.nav_explore;

        } else if (fragment instanceof SavedFragment) {
            itemId = R.id.nav_saved;

        } else if (fragment instanceof ScheduleFragment) {
            itemId = R.id.nav_schedule;
        }

        if (itemId == 0
                || binding.bottomNav.getMenu().findItem(itemId) == null) {
            return;
        }

        updatingNavigation = true;
        binding.bottomNav.setSelectedItemId(itemId);
        updatingNavigation = false;
    }
}