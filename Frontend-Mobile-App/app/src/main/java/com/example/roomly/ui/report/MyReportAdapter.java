package com.example.roomly.ui.report;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomly.R;
import com.example.roomly.data.model.AdminReport;
import com.example.roomly.databinding.ItemMyReportBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class MyReportAdapter extends
        RecyclerView.Adapter<MyReportAdapter.ReportViewHolder> {

    private final List<AdminReport> reports = new ArrayList<>();

    public void submitReports(List<AdminReport> items) {
        reports.clear();

        if (items != null) {
            reports.addAll(items);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemMyReportBinding binding = ItemMyReportBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );

        return new ReportViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ReportViewHolder holder,
            int position
    ) {
        holder.bind(reports.get(position));
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    static final class ReportViewHolder extends RecyclerView.ViewHolder {

        private final ItemMyReportBinding binding;

        ReportViewHolder(ItemMyReportBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(AdminReport report) {
            binding.tvMyReportStatus.setText(
                    report.getStatusLabel()
            );

            int statusColor = report.getStatus() == AdminReport.Status.OPEN
                    ? R.color.roomly_primary
                    : R.color.roomly_text_secondary;

            binding.tvMyReportStatus.setTextColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            statusColor
                    )
            );

            binding.tvMyReportTitle.setText(
                    report.getTargetTitle()
            );

            binding.tvMyReportReason.setText(
                    "Lý do: " + normalize(report.getReportReason())
            );

            SimpleDateFormat formatter = new SimpleDateFormat(
                    "dd/MM/yyyy HH:mm",
                    new Locale("vi", "VN")
            );

            binding.tvMyReportTime.setText(
                    "Đã gửi: " + formatter.format(
                            new Date(report.getCreatedAtMillis())
                    )
            );

            String resolution = normalize(report.getResolutionNote());

            boolean showResolution =
                    report.getStatus() != AdminReport.Status.OPEN
                            && !resolution.isEmpty();

            binding.tvMyReportResolution.setVisibility(
                    showResolution ? View.VISIBLE : View.GONE
            );

            binding.tvMyReportResolution.setText(
                    showResolution
                            ? "Kết quả xử lý: " + resolution
                            : ""
            );
        }

        private static String normalize(String value) {
            return value == null ? "" : value.trim();
        }
    }
}