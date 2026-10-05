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

import com.example.roomly.databinding.ActivityMainBinding;
import com.example.roomly.ui.detail.RoomDetailFragment;
import com.example.roomly.ui.explore.ExploreFragment;
import com.example.roomly.ui.saved.SavedFragment;
import com.example.roomly.ui.schedule.ScheduleFragment;

public class MainActivity extends AppCompatActivity {

    // Binding liên kết với giao diện activity_main.xml.
    private ActivityMainBinding binding;

    /**
     * Khởi tạo giao diện chính, thiết lập thanh điều hướng
     * và mở màn hình Khám phá khi ứng dụng được tạo lần đầu.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupWindowInsets();
        setupBottomNavAppearance();

        // Android tự khôi phục Fragment khi Activity được tạo lại.
        if (savedInstanceState == null) {
            showExploreScreen();
        }

        setupBottomNavigation();

        // Cập nhật menu khi mở chi tiết hoặc quay lại.
        getSupportFragmentManager().addOnBackStackChangedListener(
                this::updateBottomNavVisibility
        );

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
     * Đặt nền xanh nhạt ROOMLY cho tab đang được chọn.
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
     * Hiển thị màn hình Khám phá khi ứng dụng mở lần đầu.
     * Hàm được gọi trước khi đăng ký sự kiện chọn tab.
     */
    private void showExploreScreen() {
        binding.bottomNav.setSelectedItemId(R.id.nav_explore);

        getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                        R.id.fragment_container,
                        new ExploreFragment()
                )
                .commit();
    }

    /**
     * Chuyển giữa Khám phá, Đã lưu và Lịch trình.
     * Các tab chưa có giao diện sẽ chưa được chuyển sang.
     */
    private void setupBottomNavigation() {
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            Fragment nextFragment;

            if (itemId == R.id.nav_explore) {
                nextFragment = new ExploreFragment();
            } else if (itemId == R.id.nav_saved) {
                nextFragment = new SavedFragment();
            } else if (itemId == R.id.nav_schedule) {
                nextFragment = new ScheduleFragment();
            } else {
                return false;
            }

            getSupportFragmentManager()
                    .beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(
                            R.id.fragment_container,
                            nextFragment
                    )
                    .commit();

            return true;
        });

        // Giữ nguyên màn hình nếu bấm lại tab đang chọn.
        binding.bottomNav.setOnItemReselectedListener(item -> {
            // Không cần tạo lại Fragment.
        });
    }

    /**
     * Ẩn menu dưới khi đang xem chi tiết phòng.
     * Hiện lại menu khi quay về màn hình có thanh điều hướng.
     */
    private void updateBottomNavVisibility() {
        Fragment currentFragment =
                getSupportFragmentManager().findFragmentById(
                        R.id.fragment_container
                );

        boolean isRoomDetail =
                currentFragment instanceof RoomDetailFragment;

        binding.bottomNav.setVisibility(
                isRoomDetail ? View.GONE : View.VISIBLE
        );
    }
}