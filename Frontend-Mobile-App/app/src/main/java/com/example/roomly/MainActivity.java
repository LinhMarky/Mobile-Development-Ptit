package com.example.roomly;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roomly.databinding.ActivityMainBinding;
import com.example.roomly.ui.explore.ExploreFragment;

import android.view.View;
import com.example.roomly.ui.detail.RoomDetailFragment;

import com.example.roomly.ui.saved.SavedFragment;

public class MainActivity extends AppCompatActivity {

    // Binding giúp truy cập các thành phần trong activity_main.xml.
    private ActivityMainBinding binding;

    /**
     * Khởi tạo giao diện chính, xử lý khoảng cách với thanh hệ thống
     * và mở màn hình Khám phá khi app được tạo lần đầu.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupWindowInsets();

        // Đổi nền của tab đang chọn sang màu xanh nhạt ROOMLY.
        binding.bottomNav.setItemActiveIndicatorColor(
                android.content.res.ColorStateList.valueOf(
                        androidx.core.content.ContextCompat.getColor(
                                this,
                                R.color.roomly_primary_light
                        )
                )
        );

        // Khi xoay màn hình, Android tự khôi phục Fragment hiện tại.
        if (savedInstanceState == null) {
            showExploreScreen();
        }

        setupBottomNavigation();

        // Cập nhật menu dưới khi mở chi tiết hoặc quay về màn hình trước.
        getSupportFragmentManager().addOnBackStackChangedListener(
                this::updateBottomNavVisibility
        );

// Cập nhật cả khi Android khôi phục màn hình sau khi xoay máy.
        updateBottomNavVisibility();
    }

    /**
     * Thêm khoảng trống để giao diện không bị thanh trạng thái
     * và thanh điều hướng hệ thống che khuất.
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
    }

    /**
     * Đưa màn hình Khám phá vào vùng nội dung phía trên menu dưới
     * và đánh dấu tab Khám phá đang được chọn.
     */
    private void showExploreScreen() {
        binding.bottomNav.setSelectedItemId(R.id.nav_explore);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(
                        R.id.fragment_container,
                        new ExploreFragment()
                )
                .commit();
    }

    /**
     * Chuyển màn hình khi người dùng chọn tab Khám phá hoặc Đã lưu.
     * Các tab chưa có giao diện sẽ chưa được chuyển sang.
     */
    private void setupBottomNavigation() {
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            androidx.fragment.app.Fragment nextFragment;

            if (itemId == R.id.nav_explore) {
                nextFragment = new ExploreFragment();
            } else if (itemId == R.id.nav_saved) {
                nextFragment = new SavedFragment();
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

        // Không tạo lại màn hình khi bấm vào tab đang được chọn.
        binding.bottomNav.setOnItemReselectedListener(item -> {
            // Giữ nguyên màn hình hiện tại.
        });
    }

    /**
     * Ẩn menu dưới ở màn hình chi tiết phòng.
     * Hiện lại menu khi người dùng trở về màn hình chính.
     */
    private void updateBottomNavVisibility() {
        androidx.fragment.app.Fragment currentFragment =
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