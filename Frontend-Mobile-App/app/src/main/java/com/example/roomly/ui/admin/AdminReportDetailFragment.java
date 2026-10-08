package com.example.roomly.ui.admin;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.roomly.R;
import com.example.roomly.data.model.AdminReport;
import com.example.roomly.data.repository.DemoAdminListingRepository;
import com.example.roomly.data.repository.DemoAdminReportRepository;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentAdminReportDetailBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Hiển thị chi tiết và ghi nhận kết quả xử lý báo cáo mẫu.
 */
public class AdminReportDetailFragment extends Fragment {

    private static final String ARG_REPORT_ID = "admin_report_id";

    private FragmentAdminReportDetailBinding binding;
    private String reportId;
    private AlertDialog resolutionDialog;

    /**
     * Tạo màn hình chi tiết với mã báo cáo.
     */
    public static AdminReportDetailFragment newInstance(String reportId) {
        AdminReportDetailFragment fragment =
                new AdminReportDetailFragment();

        Bundle args = new Bundle();
        args.putString(ARG_REPORT_ID, reportId);
        fragment.setArguments(args);

        return fragment;
    }

    /**
     * Đọc mã báo cáo từ Bundle.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();

        if (args != null) {
            reportId = args.getString(ARG_REPORT_ID);
        }
    }

    /**
     * Tạo giao diện chi tiết báo cáo bằng ViewBinding.
     */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentAdminReportDetailBinding.inflate(
                inflater,
                container,
                false
        );

        return binding.getRoot();
    }

    /**
     * Đăng ký các thao tác và theo dõi phiên đăng nhập.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnReportDetailBack.setOnClickListener(
                clickedView -> getParentFragmentManager().popBackStack()
        );

        binding.btnReportDetailOpenListing.setOnClickListener(
                clickedView -> openReportedListing()
        );

        binding.btnReportDetailResolve.setOnClickListener(
                clickedView -> showResolutionDialog(true)
        );

        binding.btnReportDetailDismiss.setOnClickListener(
                clickedView -> showResolutionDialog(false)
        );

        SessionRepository.getInstance()
                .getSessionState()
                .observe(
                        getViewLifecycleOwner(),
                        session -> displayReport()
                );
    }

    /**
     * Cập nhật báo cáo khi trở về từ màn hình kiểm duyệt bài đăng.
     */
    @Override
    public void onResume() {
        super.onResume();
        displayReport();
    }

    /**
     * Hiển thị nội dung và các thao tác phù hợp trạng thái.
     * Repository chỉ trả dữ liệu khi tài khoản có quyền ADMIN.
     */
    private void displayReport() {
        if (binding == null) {
            return;
        }

        AdminReport report = DemoAdminReportRepository.getInstance()
                .getReportById(reportId);

        if (report == null) {
            closeResolutionDialog();

            binding.layoutReportDetailContent.setVisibility(View.GONE);
            binding.tvReportDetailStatus.setText("");
            binding.tvReportDetailTitle.setText("");
            binding.tvReportDetailInfo.setText("");
            binding.tvReportDetailReason.setText("");
            binding.tvReportDetailPreview.setText("");
            binding.tvReportDetailResolution.setText("");

            showError(
                    "Không tìm thấy báo cáo hoặc tài khoản "
                            + "không có quyền quản trị."
            );
            return;
        }

        binding.layoutReportDetailContent.setVisibility(View.VISIBLE);
        binding.tvReportDetailError.setVisibility(View.GONE);

        binding.tvReportDetailStatus.setText(report.getStatusLabel());
        binding.tvReportDetailTitle.setText(report.getTargetTitle());

        SimpleDateFormat formatter = new SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                new Locale("vi", "VN")
        );

        binding.tvReportDetailInfo.setText(
                "Loại nội dung: " + report.getContentTypeLabel()
                        + "\nNgười gửi: " + report.getReporterName()
                        + "\nThời gian: " + formatter.format(
                        new Date(report.getCreatedAtMillis())
                )
        );

        binding.tvReportDetailReason.setText(report.getReportReason());
        binding.tvReportDetailPreview.setText(report.getContentPreview());

        boolean open = report.getStatus() == AdminReport.Status.OPEN;

        binding.btnReportDetailResolve.setVisibility(
                open ? View.VISIBLE : View.GONE
        );

        binding.btnReportDetailDismiss.setVisibility(
                open ? View.VISIBLE : View.GONE
        );

        binding.tvReportDetailResolution.setText(
                open ? "" : "Kết quả xử lý:\n" + report.getResolutionNote()
        );

        binding.tvReportDetailResolution.setVisibility(
                open ? View.GONE : View.VISIBLE
        );

        boolean canOpenListing =
                report.getContentType() == AdminReport.ContentType.LISTING
                        && DemoAdminListingRepository.getInstance()
                        .getListingById(report.getTargetId()) != null;

        binding.btnReportDetailOpenListing.setVisibility(
                canOpenListing ? View.VISIBLE : View.GONE
        );
    }

    /**
     * Kiểm tra báo cáo và mở bài đăng được liên kết.
     */
    private void openReportedListing() {
        if (binding == null) {
            return;
        }

        AdminReport report = DemoAdminReportRepository.getInstance()
                .getReportById(reportId);

        if (report == null
                || report.getContentType()
                != AdminReport.ContentType.LISTING) {
            displayReport();
            return;
        }

        if (DemoAdminListingRepository.getInstance()
                .getListingById(report.getTargetId()) == null) {
            showError("Bài đăng không còn truy cập được.");
            return;
        }

        getParentFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                        R.id.fragment_container,
                        AdminListingDetailFragment.newInstance(
                                report.getTargetId()
                        )
                )
                .addToBackStack(null)
                .commit();
    }

    /**
     * Hiển thị hộp thoại nhập ghi chú kết quả.
     * Giữ hộp thoại mở nếu người dùng chưa nhập ghi chú.
     */
    private void showResolutionDialog(boolean violationFound) {
        if (binding == null || resolutionDialog != null) {
            return;
        }

        AdminReport report = DemoAdminReportRepository.getInstance()
                .getReportById(reportId);

        if (report == null || report.getStatus() != AdminReport.Status.OPEN) {
            displayReport();
            return;
        }

        EditText noteInput = new EditText(requireContext());
        noteInput.setHint("Nhập ghi chú kết quả xử lý");
        noteInput.setMinLines(3);
        noteInput.setGravity(android.view.Gravity.TOP);
        noteInput.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        );

        LinearLayout container = new LinearLayout(requireContext());

        int padding = Math.round(
                20 * getResources().getDisplayMetrics().density
        );

        container.setPadding(padding, padding, padding, 0);

        container.addView(
                noteInput,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        AlertDialog dialog =
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(
                                violationFound
                                        ? "Ghi nhận vi phạm?"
                                        : "Không ghi nhận vi phạm?"
                        )
                        .setMessage(
                                "Ghi nhận kết quả trên báo cáo mẫu. "
                                        + "Thao tác này không tự động "
                                        + "ẩn nội dung được báo cáo."
                        )
                        .setView(container)
                        .setNegativeButton("Quay lại", null)
                        .setPositiveButton("Xác nhận", null)
                        .create();

        resolutionDialog = dialog;

        dialog.setOnDismissListener(dismissedDialog -> {
            if (resolutionDialog == dialog) {
                resolutionDialog = null;
            }
        });

        dialog.show();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(clickedView -> {
                    String note = noteInput.getText().toString().trim();

                    if (note.isEmpty()) {
                        noteInput.setError("Bạn hãy nhập ghi chú.");
                        noteInput.requestFocus();
                        return;
                    }

                    dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                            .setEnabled(false);

                    try {
                        DemoAdminReportRepository.getInstance()
                                .resolveReport(
                                        reportId,
                                        violationFound,
                                        note
                                );

                        dialog.dismiss();
                        displayReport();

                        Toast.makeText(
                                requireContext(),
                                "Đã cập nhật kết quả báo cáo mẫu.",
                                Toast.LENGTH_SHORT
                        ).show();
                    } catch (IllegalArgumentException
                             | IllegalStateException exception) {
                        dialog.dismiss();
                        displayReport();
                        showError(exception.getMessage());
                    }
                });
    }

    /**
     * Hiển thị lỗi thao tác hoặc lỗi truy cập.
     */
    private void showError(@Nullable String message) {
        if (binding == null) {
            return;
        }

        binding.tvReportDetailError.setText(
                message == null ? "Không thể xử lý báo cáo." : message
        );

        binding.tvReportDetailError.setVisibility(View.VISIBLE);
    }

    /**
     * Đóng hộp thoại kết quả nếu đang mở.
     */
    private void closeResolutionDialog() {
        AlertDialog dialog = resolutionDialog;
        resolutionDialog = null;

        if (dialog != null) {
            dialog.dismiss();
        }
    }

    /**
     * Đóng hộp thoại và giải phóng các tham chiếu giao diện.
     */
    @Override
    public void onDestroyView() {
        closeResolutionDialog();

        if (binding != null) {
            binding.btnReportDetailBack.setOnClickListener(null);
            binding.btnReportDetailOpenListing.setOnClickListener(null);
            binding.btnReportDetailResolve.setOnClickListener(null);
            binding.btnReportDetailDismiss.setOnClickListener(null);
        }

        binding = null;
        super.onDestroyView();
    }
}