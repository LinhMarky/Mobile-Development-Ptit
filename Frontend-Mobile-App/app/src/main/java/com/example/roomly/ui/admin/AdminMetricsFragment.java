package com.example.roomly.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.roomly.BuildConfig;
import com.example.roomly.data.model.AdminAccount;
import com.example.roomly.data.model.AdminListing;
import com.example.roomly.data.model.AdminReport;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;
import com.example.roomly.data.repository.DemoAdminAccountRepository;
import com.example.roomly.data.repository.DemoAdminListingRepository;
import com.example.roomly.data.repository.DemoAdminReportRepository;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentAdminMetricsBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Hiển thị thống kê từ dữ liệu quản trị mẫu hiện có.
 * Chưa sử dụng API thống kê của backend.
 */
public class AdminMetricsFragment extends Fragment {

    private FragmentAdminMetricsBinding binding;

    /** Tạo giao diện thống kê bằng ViewBinding. */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentAdminMetricsBinding.inflate(
                inflater,
                container,
                false
        );

        return binding.getRoot();
    }

    /**
     * Đăng ký nút quay lại và quan sát quyền truy cập
     * theo vòng đời giao diện.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnAdminMetricsBack.setOnClickListener(
                clickedView ->
                        getParentFragmentManager().popBackStack()
        );

        SessionRepository.getInstance()
                .getSessionState()
                .observe(
                        getViewLifecycleOwner(),
                        session -> displayMetrics()
                );
    }

    /** Đọc lại số liệu mỗi khi màn hình hoạt động trở lại. */
    @Override
    public void onResume() {
        super.onResume();

        displayMetrics();
    }

    /**
     * Kiểm tra quyền admin trước khi đọc và hiển thị thống kê.
     * Xóa số liệu trên giao diện nếu phiên mất quyền.
     */
    private void displayMetrics() {
        if (binding == null) {
            return;
        }

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        boolean allowed = session.isLoggedIn()
                && session.hasRole(UserRole.ADMIN);

        binding.layoutAdminMetricsContent.setVisibility(
                allowed ? View.VISIBLE : View.GONE
        );

        binding.tvAdminMetricsAccessError.setVisibility(
                allowed ? View.GONE : View.VISIBLE
        );

        if (!allowed) {
            clearMetrics();
            return;
        }

        displayListingMetrics();
        displayReportMetrics();
        displayAccountMetrics();

        String updatedTime = new SimpleDateFormat(
                "HH:mm • dd/MM/yyyy",
                new Locale("vi", "VN")
        ).format(new Date());

        binding.tvAdminMetricsUpdated.setText(
                "Cập nhật lúc " + updatedTime
        );

        binding.tvAdminMetricsDemoNote.setVisibility(
                BuildConfig.DEBUG ? View.VISIBLE : View.GONE
        );
    }

    /** Đếm tổng bài đăng và số lượng theo từng trạng thái mẫu. */
    private void displayListingMetrics() {
        List<AdminListing> listings =
                DemoAdminListingRepository.getInstance()
                        .getListings(null);

        int pending = 0;
        int published = 0;
        int hidden = 0;
        int rejected = 0;

        for (AdminListing listing : listings) {
            switch (listing.getStatus()) {
                case PENDING:
                    pending++;
                    break;

                case PUBLISHED:
                    published++;
                    break;

                case HIDDEN:
                    hidden++;
                    break;

                case REJECTED:
                    rejected++;
                    break;
            }
        }

        binding.tvAdminMetricsListingTotal.setText(
                String.valueOf(listings.size())
        );

        binding.tvAdminMetricsListingBreakdown.setText(
                "Chờ duyệt: " + pending
                        + "\nĐang hiển thị: " + published
                        + "\nĐã ẩn: " + hidden
                        + "\nĐã từ chối: " + rejected
        );
    }

    /** Đếm báo cáo đang mở và kết quả xử lý các báo cáo đã đóng. */
    private void displayReportMetrics() {
        List<AdminReport> reports =
                DemoAdminReportRepository.getInstance()
                        .getReports(null);

        int open = 0;
        int resolved = 0;
        int dismissed = 0;

        for (AdminReport report : reports) {
            switch (report.getStatus()) {
                case OPEN:
                    open++;
                    break;

                case RESOLVED:
                    resolved++;
                    break;

                case DISMISSED:
                    dismissed++;
                    break;
            }
        }

        binding.tvAdminMetricsReportTotal.setText(
                String.valueOf(reports.size())
        );

        binding.tvAdminMetricsReportBreakdown.setText(
                "Chờ xử lý: " + open
                        + "\nGhi nhận vi phạm: " + resolved
                        + "\nKhông ghi nhận vi phạm: " + dismissed
        );
    }

    /**
     * Đếm tài khoản hoạt động, bị khóa và đã xác minh email.
     * Số liệu lấy từ cùng repository với màn hình quản lý tài khoản.
     */
    private void displayAccountMetrics() {
        List<AdminAccount> accounts =
                DemoAdminAccountRepository.getInstance()
                        .getAccounts(null, "");

        int active = 0;
        int suspended = 0;
        int verified = 0;

        for (AdminAccount account : accounts) {
            if (account.getStatus() == AdminAccount.Status.ACTIVE) {
                active++;
            } else if (
                    account.getStatus()
                            == AdminAccount.Status.SUSPENDED
            ) {
                suspended++;
            }

            if (account.isEmailVerified()) {
                verified++;
            }
        }

        binding.tvAdminMetricsAccountTotal.setText(
                String.valueOf(accounts.size())
        );

        binding.tvAdminMetricsAccountBreakdown.setText(
                "Đang hoạt động: " + active
                        + "\nĐã khóa: " + suspended
                        + "\nEmail đã xác minh: " + verified
        );
    }

    /** Xóa số liệu để không giữ nội dung quản trị khi mất quyền. */
    private void clearMetrics() {
        binding.tvAdminMetricsListingTotal.setText("");
        binding.tvAdminMetricsListingBreakdown.setText("");

        binding.tvAdminMetricsReportTotal.setText("");
        binding.tvAdminMetricsReportBreakdown.setText("");

        binding.tvAdminMetricsAccountTotal.setText("");
        binding.tvAdminMetricsAccountBreakdown.setText("");

        binding.tvAdminMetricsUpdated.setText("");
        binding.tvAdminMetricsDemoNote.setVisibility(View.GONE);
    }

    /** Gỡ listener và giải phóng binding khi giao diện bị hủy. */
    @Override
    public void onDestroyView() {
        if (binding != null) {
            binding.btnAdminMetricsBack.setOnClickListener(null);
        }

        binding = null;

        super.onDestroyView();
    }
}