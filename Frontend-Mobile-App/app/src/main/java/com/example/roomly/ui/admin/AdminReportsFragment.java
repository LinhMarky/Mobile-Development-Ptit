package com.example.roomly.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.roomly.R;
import com.example.roomly.data.model.AdminReport;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;
import com.example.roomly.data.repository.DemoAdminReportRepository;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentAdminReportsBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Hiển thị và lọc báo cáo vi phạm cho tài khoản ADMIN.
 */
public class AdminReportsFragment extends Fragment {

    private static final String STATE_FILTER = "admin_report_filter";

    private FragmentAdminReportsBinding binding;
    private AdminReportAdapter reportAdapter;

    private int selectedFilterId = R.id.chip_admin_reports_all;

    /**
     * Khôi phục bộ lọc khi Android tạo lại Fragment.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            selectedFilterId = savedInstanceState.getInt(
                    STATE_FILTER,
                    R.id.chip_admin_reports_all
            );
        }
    }

    /**
     * Tạo giao diện danh sách báo cáo.
     */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentAdminReportsBinding.inflate(
                inflater,
                container,
                false
        );

        return binding.getRoot();
    }

    /**
     * Thiết lập danh sách, bộ lọc, nút quay lại và theo dõi phiên.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        binding.rvAdminReports.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        reportAdapter = new AdminReportAdapter(new ArrayList<>());
        reportAdapter.setOnReportClickListener(this::openReportDetail);

        binding.rvAdminReports.setAdapter(reportAdapter);

        binding.btnAdminReportsBack.setOnClickListener(
                clickedView -> getParentFragmentManager().popBackStack()
        );

        binding.chipGroupAdminReports.check(selectedFilterId);

        binding.chipGroupAdminReports.setOnCheckedStateChangeListener(
                (group, checkedIds) -> {
                    if (checkedIds.isEmpty()) {
                        return;
                    }

                    selectedFilterId = checkedIds.get(0);
                    displayReports();
                }
        );

        SessionRepository.getInstance()
                .getSessionState()
                .observe(
                        getViewLifecycleOwner(),
                        session -> displayReports()
                );
    }

    /**
     * Áp dụng bộ lọc sau khi Android khôi phục giao diện.
     */
    @Override
    public void onViewStateRestored(
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewStateRestored(savedInstanceState);

        binding.chipGroupAdminReports.check(selectedFilterId);
        displayReports();
    }

    /**
     * Cập nhật danh sách khi trở về từ màn hình khác.
     */
    @Override
    public void onResume() {
        super.onResume();
        displayReports();
    }

    /**
     * Kiểm tra quyền trước khi đọc danh sách.
     * Xóa dữ liệu hiển thị nếu tài khoản không còn quyền ADMIN.
     */
    private void displayReports() {
        if (binding == null || reportAdapter == null) {
            return;
        }

        SessionState session =
                SessionRepository.getInstance().getCurrentSession();

        boolean allowed = session.isLoggedIn()
                && session.hasRole(UserRole.ADMIN);

        binding.layoutAdminReportsContent.setVisibility(
                allowed ? View.VISIBLE : View.GONE
        );

        binding.tvAdminReportsAccessError.setVisibility(
                allowed ? View.GONE : View.VISIBLE
        );

        if (!allowed) {
            reportAdapter.updateReports(new ArrayList<>());

            binding.tvAdminReportsAccessError.setText(
                    session.isLoggedIn()
                            ? "Tài khoản hiện tại không có quyền quản trị."
                            : "Bạn cần đăng nhập tài khoản quản trị."
            );
            return;
        }

        List<AdminReport> reports =
                DemoAdminReportRepository.getInstance()
                        .getReports(getClosedFilter());

        reportAdapter.updateReports(reports);

        boolean empty = reports.isEmpty();

        binding.rvAdminReports.setVisibility(
                empty ? View.GONE : View.VISIBLE
        );

        binding.tvAdminReportsEmpty.setVisibility(
                empty ? View.VISIBLE : View.GONE
        );
    }

    /**
     * Chuyển bộ lọc thành nhóm trạng thái của repository.
     */
    @Nullable
    private Boolean getClosedFilter() {
        if (selectedFilterId == R.id.chip_admin_reports_open) {
            return false;
        }

        if (selectedFilterId == R.id.chip_admin_reports_closed) {
            return true;
        }

        return null;
    }

    /**
     * Kiểm tra quyền truy cập và mở chi tiết báo cáo được chọn.
     * Giữ danh sách và bộ lọc trong back stack.
     */
    private void openReportDetail(AdminReport report) {
        AdminReport currentReport =
                DemoAdminReportRepository.getInstance()
                        .getReportById(report.getId());

        if (currentReport == null) {
            displayReports();
            return;
        }

        getParentFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                        R.id.fragment_container,
                        AdminReportDetailFragment.newInstance(
                                currentReport.getId()
                        )
                )
                .addToBackStack(null)
                .commit();
    }
    /**
     * Lưu bộ lọc để khôi phục khi Fragment được tạo lại.
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(STATE_FILTER, selectedFilterId);
        super.onSaveInstanceState(outState);
    }

    /**
     * Gỡ các sự kiện và giải phóng adapter, binding.
     */
    @Override
    public void onDestroyView() {
        if (binding != null) {
            binding.btnAdminReportsBack.setOnClickListener(null);

            binding.chipGroupAdminReports
                    .setOnCheckedStateChangeListener(null);

            binding.rvAdminReports.setAdapter(null);
        }

        if (reportAdapter != null) {
            reportAdapter.setOnReportClickListener(null);
        }

        reportAdapter = null;
        binding = null;

        super.onDestroyView();
    }
}