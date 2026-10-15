package com.example.roomly.ui.report;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.repository.ReportRepository;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentMyReportsBinding;

import java.util.Collections;

public final class MyReportsFragment extends Fragment {

    private FragmentMyReportsBinding binding;
    private MyReportAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentMyReportsBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new MyReportAdapter();

        binding.rvMyReports.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvMyReports.setAdapter(adapter);

        binding.btnMyReportsBack.setOnClickListener(
                clicked -> getParentFragmentManager().popBackStack());

        binding.btnMyReportsRetry.setOnClickListener(clicked -> loadReports());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadReports();
    }

    private void loadReports() {
        if (binding == null || adapter == null) {
            return;
        }

        binding.rvMyReports.setVisibility(View.GONE);
        binding.layoutMyReportsEmpty.setVisibility(View.GONE);
        binding.layoutMyReportsError.setVisibility(View.GONE);

        // Xóa dữ liệu cũ trước khi kiểm tra tài khoản hiện tại.
        adapter.submitReports(Collections.emptyList());

        SessionState session = SessionRepository.getInstance().getCurrentSession();

        if (!session.isLoggedIn()) {
            showError("Bạn cần đăng nhập để xem báo cáo đã gửi.");
            return;
        }

        ReportRepository.getInstance()
                .fetchReports(
                        false,
                        null,
                        reports -> {
                            if (binding == null || adapter == null) return;
                            adapter.submitReports(reports);
                            binding.layoutMyReportsEmpty.setVisibility(
                                    reports.isEmpty() ? View.VISIBLE : View.GONE);
                            binding.rvMyReports.setVisibility(
                                    reports.isEmpty() ? View.GONE : View.VISIBLE);
                        },
                        this::showError);
    }

    private void showError(String message) {
        if (binding == null) {
            return;
        }

        binding.rvMyReports.setVisibility(View.GONE);
        binding.layoutMyReportsEmpty.setVisibility(View.GONE);
        binding.layoutMyReportsError.setVisibility(View.VISIBLE);
        binding.tvMyReportsError.setText(message);
    }

    @Override
    public void onDestroyView() {
        if (binding != null) {
            binding.btnMyReportsBack.setOnClickListener(null);
            binding.btnMyReportsRetry.setOnClickListener(null);
            binding.rvMyReports.setAdapter(null);
        }

        adapter = null;
        binding = null;

        super.onDestroyView();
    }
}
