package com.example.roomly.ui.admin;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomly.data.model.AdminReport;
import com.example.roomly.databinding.ItemAdminReportBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Hiển thị danh sách báo cáo vi phạm.
 */
public class AdminReportAdapter
        extends RecyclerView.Adapter<
        AdminReportAdapter.ReportViewHolder> {

    private final List<AdminReport> reports = new ArrayList<>();

    private OnReportClickListener reportClickListener;

    public interface OnReportClickListener {

        /**
         * Thông báo báo cáo được chọn để mở chi tiết.
         */
        void onReportClick(AdminReport report);
    }

    /**
     * Sao chép danh sách ban đầu vào Adapter.
     */
    public AdminReportAdapter(List<AdminReport> initialReports) {
        reports.addAll(initialReports);
    }

    /**
     * Đăng ký hoặc gỡ bộ xử lý mở chi tiết.
     */
    public void setOnReportClickListener(
            @Nullable OnReportClickListener listener
    ) {
        reportClickListener = listener;
    }

    /**
     * Tạo giao diện một thẻ báo cáo.
     */
    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemAdminReportBinding binding =
                ItemAdminReportBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                );

        return new ReportViewHolder(binding);
    }

    /**
     * Hiển thị báo cáo và đăng ký thao tác mở chi tiết.
     */
    @Override
    public void onBindViewHolder(
            @NonNull ReportViewHolder holder,
            int position
    ) {
        holder.bind(reports.get(position));

        holder.binding.btnAdminReportDetail.setOnClickListener(view -> {
            int currentPosition = holder.getBindingAdapterPosition();

            if (currentPosition == RecyclerView.NO_POSITION
                    || reportClickListener == null) {
                return;
            }

            reportClickListener.onReportClick(
                    reports.get(currentPosition)
            );
        });
    }

    /**
     * Trả về số báo cáo đang hiển thị.
     */
    @Override
    public int getItemCount() {
        return reports.size();
    }

    /**
     * Thay danh sách đang hiển thị bằng kết quả mới.
     */
    public void updateReports(List<AdminReport> newReports) {
        List<AdminReport> updatedReports = new ArrayList<>(newReports);

        reports.clear();
        reports.addAll(updatedReports);
        notifyDataSetChanged();
    }

    /**
     * Gỡ sự kiện khi thẻ được đưa vào vùng tái sử dụng.
     */
    @Override
    public void onViewRecycled(@NonNull ReportViewHolder holder) {
        holder.binding.btnAdminReportDetail.setOnClickListener(null);
        super.onViewRecycled(holder);
    }

    /**
     * Giữ giao diện của một thẻ báo cáo.
     */
    static class ReportViewHolder extends RecyclerView.ViewHolder {

        private final ItemAdminReportBinding binding;

        /**
         * Khởi tạo ViewHolder bằng binding của thẻ.
         */
        ReportViewHolder(ItemAdminReportBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Hiển thị trạng thái, nội dung tóm tắt, người gửi và thời gian.
         */
        void bind(AdminReport report) {
            binding.tvAdminReportStatus.setText(report.getStatusLabel());
            binding.tvAdminReportType.setText(report.getContentTypeLabel());
            binding.tvAdminReportTitle.setText(report.getTargetTitle());

            binding.tvAdminReportReason.setText(
                    "Lý do: " + report.getReportReason()
            );

            binding.tvAdminReportSender.setText(
                    "Người gửi: " + report.getReporterName()
            );

            SimpleDateFormat formatter = new SimpleDateFormat(
                    "dd/MM/yyyy HH:mm",
                    new Locale("vi", "VN")
            );

            binding.tvAdminReportTime.setText(
                    "Gửi lúc: " + formatter.format(
                            new Date(report.getCreatedAtMillis())
                    )
            );
        }
    }
}