package com.example.roomly.ui.report;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.repository.DemoAdminReportRepository;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.DialogReportListingBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.example.roomly.util.KeyboardUtils;

public final class ReportListingDialogFragment extends DialogFragment {

    public static final String TAG = "report_listing_dialog";

    private static final String ARG_USER_ID = "user_id";
    private static final String ARG_LISTING_ID = "listing_id";
    private static final String ARG_LISTING_TITLE = "listing_title";
    private static final String ARG_CONTENT_PREVIEW = "content_preview";

    private static final String STATE_REASON = "selected_reason";
    private static final String STATE_DESCRIPTION = "description";

    private static final String[] REASONS = {
            "Thông tin không chính xác",
            "Có dấu hiệu lừa đảo",
            "Nội dung không phù hợp",
            "Tin đăng trùng lặp hoặc quảng cáo",
            "Khác"
    };

    private DialogReportListingBinding binding;
    private boolean submitting;

    public static ReportListingDialogFragment newInstance(
            String listingId,
            String listingTitle,
            String contentPreview
    ) {
        SessionState session =
                SessionRepository.getInstance().getCurrentSession();

        Bundle arguments = new Bundle();
        arguments.putString(ARG_USER_ID, session.getUserId());
        arguments.putString(ARG_LISTING_ID, listingId);
        arguments.putString(ARG_LISTING_TITLE, listingTitle);
        arguments.putString(ARG_CONTENT_PREVIEW, contentPreview);

        ReportListingDialogFragment fragment =
                new ReportListingDialogFragment();

        fragment.setArguments(arguments);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = DialogReportListingBinding.inflate(
                LayoutInflater.from(requireContext())
        );

        binding.tvReportListingTitle.setText(
                getArgument(ARG_LISTING_TITLE)
        );

        ArrayAdapter<String> reasonAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                REASONS
        );

        binding.dropdownReportReason.setAdapter(reasonAdapter);

        binding.dropdownReportReason.setOnItemClickListener(
                (parent, view, position, id) ->
                        binding.inputReportReason.setError(null)
        );

        if (savedInstanceState != null) {
            binding.dropdownReportReason.setText(
                    savedInstanceState.getString(STATE_REASON, ""),
                    false
            );

            binding.edtReportDescription.setText(
                    savedInstanceState.getString(STATE_DESCRIPTION, "")
            );
        }

        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Báo cáo bài đăng")
                .setView(binding.getRoot())
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Gửi báo cáo", null)
                .create();
    }

    @Override
    public void onStart() {
        super.onStart();

        AlertDialog dialog = (AlertDialog) getDialog();

        if (dialog == null || binding == null) {
            return;
        }

        KeyboardUtils.setupTapOutside(binding.getRoot());

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> submitReport());

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setOnClickListener(view -> {
                    KeyboardUtils.hide(binding.getRoot());
                    dismiss();
                });
    }
    private void submitReport() {
        if (binding == null || submitting) {
            return;
        }

        binding.inputReportReason.setError(null);
        binding.inputReportDescription.setError(null);

        String reason = binding.dropdownReportReason
                .getText()
                .toString()
                .trim();

        String description = binding.edtReportDescription.getText() == null
                ? ""
                : binding.edtReportDescription.getText()
                .toString()
                .trim();

        if (!isValidReason(reason)) {
            binding.inputReportReason.setError(
                    "Bạn hãy chọn lý do báo cáo."
            );
            binding.dropdownReportReason.requestFocus();
            return;
        }

        if (description.codePointCount(0, description.length()) > 2000) {
            binding.inputReportDescription.setError(
                    "Mô tả không được vượt quá 2000 ký tự."
            );
            binding.edtReportDescription.requestFocus();
            return;
        }

        if ("Khác".equals(reason) && description.isEmpty()) {
            binding.inputReportDescription.setError(
                    "Bạn hãy mô tả lý do báo cáo."
            );
            binding.edtReportDescription.requestFocus();
            return;
        }
        KeyboardUtils.hide(binding.getRoot());

        submitting = true;
        setSubmitEnabled(false);

        try {
            DemoAdminReportRepository.getInstance().createListingReport(
                    getArgument(ARG_USER_ID),
                    getArgument(ARG_LISTING_ID),
                    getArgument(ARG_LISTING_TITLE),
                    getArgument(ARG_CONTENT_PREVIEW),
                    reason,
                    description
            );

            Toast.makeText(
                    requireContext(),
                    "Đã lưu báo cáo demo. Chưa gửi đến backend.",
                    Toast.LENGTH_LONG
            ).show();

            dismiss();
        } catch (RuntimeException exception) {
            submitting = false;
            setSubmitEnabled(true);

            String message = exception.getMessage();

            if (message == null || message.trim().isEmpty()) {
                message = "Không thể gửi báo cáo. Vui lòng thử lại.";
            }

            if (isAdded()) {
                Toast.makeText(
                        requireContext(),
                        message,
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    private boolean isValidReason(String reason) {
        for (String item : REASONS) {
            if (item.equals(reason)) {
                return true;
            }
        }

        return false;
    }

    private void setSubmitEnabled(boolean enabled) {
        AlertDialog dialog = (AlertDialog) getDialog();

        if (dialog != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setEnabled(enabled);
        }
    }

    private String getArgument(String key) {
        Bundle arguments = getArguments();

        if (arguments == null) {
            return "";
        }

        String value = arguments.getString(key);
        return value == null ? "" : value.trim();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (binding != null) {
            outState.putString(
                    STATE_REASON,
                    binding.dropdownReportReason.getText().toString()
            );

            outState.putString(
                    STATE_DESCRIPTION,
                    binding.edtReportDescription.getText() == null
                            ? ""
                            : binding.edtReportDescription.getText().toString()
            );
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        if (binding != null) {
            KeyboardUtils.hide(binding.getRoot());
            KeyboardUtils.clearTapOutside(binding.getRoot());

            binding.dropdownReportReason.setOnItemClickListener(null);
        }

        binding = null;
        super.onDestroyView();
    }}