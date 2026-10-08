package com.example.roomly.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentAdminDashboardBinding;
import com.example.roomly.R;

/**
 * Hiển thị trang quản trị cho tài khoản có quyền ADMIN.
 */
public class AdminDashboardFragment extends Fragment {

    private FragmentAdminDashboardBinding binding;

    /**
     * Tạo giao diện từ fragment_admin_dashboard.xml.
     */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentAdminDashboardBinding.inflate(
                inflater,
                container,
                false
        );

        return binding.getRoot();
    }

    /**
     * Đăng ký các nút và quan sát quyền của phiên hiện tại.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnAdminBack.setOnClickListener(
                clickedView -> getParentFragmentManager().popBackStack()
        );

        binding.btnAdminListings.setOnClickListener(
                clickedView -> openListingsScreen()
        );

        binding.btnAdminReports.setOnClickListener(
                clickedView -> openReportsScreen()
        );
        binding.btnAdminAccounts.setOnClickListener(
                clickedView -> openAccountsScreen()
        );
        binding.btnAdminMetrics.setOnClickListener(
                clickedView -> openMetricsScreen()
        );
        SessionRepository.getInstance()
                .getSessionState()
                .observe(
                        getViewLifecycleOwner(),
                        this::renderSession
                );
    }

    /**
     * Chỉ hiển thị nội dung quản trị cho tài khoản có quyền ADMIN.
     * Ẩn nội dung ngay khi đăng xuất hoặc không còn quyền.
     */
    private void renderSession(@Nullable SessionState session) {
        if (binding == null) {
            return;
        }

        SessionState currentSession = session == null
                ? SessionState.guest()
                : session;

        boolean allowed = currentSession.isLoggedIn()
                && currentSession.hasRole(UserRole.ADMIN);

        binding.layoutAdminContent.setVisibility(
                allowed ? View.VISIBLE : View.GONE
        );

        binding.tvAdminAccessError.setVisibility(
                allowed ? View.GONE : View.VISIBLE
        );

        binding.tvAdminAccessError.setText(
                currentSession.isLoggedIn()
                        ? "Tài khoản hiện tại không có quyền quản trị."
                        : "Bạn cần đăng nhập tài khoản có quyền quản trị."
        );
    }

    /**
     * Kiểm tra lại quyền khi bấm nút và báo màn hình chưa được nối.
     * Bước sau sẽ thay thông báo bằng điều hướng đến chức năng cụ thể.
     */
    private void showPendingFeature(String featureName) {
        if (binding == null) {
            return;
        }

        SessionState session =
                SessionRepository.getInstance().getCurrentSession();

        if (!session.isLoggedIn()
                || !session.hasRole(UserRole.ADMIN)) {
            renderSession(session);
            return;
        }

        Toast.makeText(
                requireContext(),
                featureName + ": đang xây dựng giao diện.",
                Toast.LENGTH_SHORT
        ).show();
    }

    /**
     * Kiểm tra quyền ADMIN và mở danh sách bài đăng cần quản trị.
     */
    private void openListingsScreen() {
        if (binding == null) {
            return;
        }

        SessionState session =
                SessionRepository.getInstance().getCurrentSession();

        if (!session.isLoggedIn()
                || !session.hasRole(UserRole.ADMIN)) {
            renderSession(session);
            return;
        }

        getParentFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                        R.id.fragment_container,
                        new AdminListingsFragment()
                )
                .addToBackStack(null)
                .commit();
    }

    /**
     * Kiểm tra quyền ADMIN và mở danh sách báo cáo vi phạm.
     */
    private void openReportsScreen() {
        if (binding == null) {
            return;
        }

        SessionState session =
                SessionRepository.getInstance().getCurrentSession();

        if (!session.isLoggedIn()
                || !session.hasRole(UserRole.ADMIN)) {
            renderSession(session);
            return;
        }

        getParentFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                        R.id.fragment_container,
                        new AdminReportsFragment()
                )
                .addToBackStack(null)
                .commit();
    }

    /**
     * Mở quản lý tài khoản sau khi kiểm tra quyền admin hiện tại.
     * Giữ trang quản trị trong back stack để có thể quay lại.
     */
    private void openAccountsScreen() {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()
                || !session.hasRole(UserRole.ADMIN)) {
            return;
        }

        getParentFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                        R.id.fragment_container,
                        new AdminAccountsFragment()
                )
                .addToBackStack(null)
                .commit();
    }

    /**
     * Mở thống kê sau khi kiểm tra quyền admin hiện tại.
     * Giữ trang quản trị trong back stack để có thể quay lại.
     */
    private void openMetricsScreen() {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()
                || !session.hasRole(UserRole.ADMIN)) {
            return;
        }

        getParentFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                        R.id.fragment_container,
                        new AdminMetricsFragment()
                )
                .addToBackStack(null)
                .commit();
    }

    /**
     * Gỡ các sự kiện và giải phóng binding khi giao diện bị hủy.
     */
    @Override
    public void onDestroyView() {
        if (binding != null) {
            binding.btnAdminBack.setOnClickListener(null);
            binding.btnAdminListings.setOnClickListener(null);
            binding.btnAdminReports.setOnClickListener(null);
            binding.btnAdminAccounts.setOnClickListener(null);
            binding.btnAdminMetrics.setOnClickListener(null);
        }

        binding = null;

        super.onDestroyView();
    }
}