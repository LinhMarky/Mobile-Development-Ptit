package com.example.roomly.ui.admin;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.roomly.data.model.BookingCase;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;
import com.example.roomly.data.repository.DemoBookingCaseRepository;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.DialogAdminBookingCaseBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.example.roomly.util.KeyboardUtils;

public final class AdminBookingCaseDialogFragment extends DialogFragment {

    public static final String TAG = "admin_booking_case_dialog";
    public static final String RESULT_UPDATED = "admin_booking_case_updated";

    private static final String ARG_CASE_ID = "case_id";
    private static final String ARG_ADMIN_ID = "admin_id";

    private static final String STATE_DECISION = "decision";
    private static final String STATE_NOTE = "note";

    private static final BookingCase.Decision[] DECISIONS = {
            BookingCase.Decision.REFUND_TENANT,
            BookingCase.Decision.FORFEIT_DEPOSIT,
            BookingCase.Decision.SPLIT,
            BookingCase.Decision.NO_ACTION
    };

    private static final String[] DECISION_LABELS = {
            "Hoàn cọc cho Người thuê",
            "Người thuê mất cọc",
            "Chia tiền cọc giữa hai bên",
            "Không áp dụng xử lý tiền cọc"
    };

    private DialogAdminBookingCaseBinding binding;
    private boolean editable;
    private boolean submitting;

    public static AdminBookingCaseDialogFragment newInstance(String caseId) {
        SessionState session =
                SessionRepository.getInstance().getCurrentSession();

        Bundle arguments = new Bundle();
        arguments.putString(ARG_CASE_ID, caseId);
        arguments.putString(ARG_ADMIN_ID, session.getUserId());

        AdminBookingCaseDialogFragment fragment =
                new AdminBookingCaseDialogFragment();

        fragment.setArguments(arguments);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        try {
            requireAdminSession();

            BookingCase bookingCase =
                    DemoBookingCaseRepository.getInstance()
                            .getAdminCaseById(getArgument(ARG_CASE_ID));

            if (bookingCase == null) {
                throw new IllegalStateException(
                        "Không tìm thấy yêu cầu hỗ trợ."
                );
            }

            binding = DialogAdminBookingCaseBinding.inflate(
                    getLayoutInflater()
            );

            binding.tvAdminCaseDetailType.setText(
                    bookingCase.getTypeLabel()
            );

            binding.tvAdminCaseDetailBooking.setText(
                    "Mã yêu cầu thuê: " + bookingCase.getBookingId()
            );

            binding.tvAdminCaseDetailStatus.setText(
                    bookingCase.getStatusLabel()
            );

            binding.tvAdminCaseDetailDescription.setText(
                    bookingCase.getDescription()
            );

            binding.tvAdminCaseDetailTime.setText(
                    "Đã gửi: " + bookingCase.getCreatedAtLabel()
            );

            editable = bookingCase.isOpen();

            if (editable) {
                setupForm(savedInstanceState);
            } else {
                showResolution(bookingCase);
            }

            MaterialAlertDialogBuilder builder =
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Xử lý yêu cầu hỗ trợ")
                            .setView(binding.getRoot());

            if (editable) {
                builder.setNegativeButton("Hủy", null);
                builder.setPositiveButton("Lưu kết luận", null);
            } else {
                builder.setPositiveButton("Đóng", null);
            }

            return builder.create();
        } catch (RuntimeException exception) {
            editable = false;

            return new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Yêu cầu hỗ trợ")
                    .setMessage(getErrorMessage(exception))
                    .setPositiveButton("Đóng", null)
                    .create();
        }
    }

    private void setupForm(@Nullable Bundle savedInstanceState) {
        binding.dropdownAdminCaseDecision.setAdapter(
                new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        DECISION_LABELS
                )
        );

        binding.dropdownAdminCaseDecision.setOnItemClickListener(
                (parent, view, position, id) ->
                        binding.inputAdminCaseDecision.setError(null)
        );

        if (savedInstanceState != null) {
            binding.dropdownAdminCaseDecision.setText(
                    savedInstanceState.getString(STATE_DECISION, ""),
                    false
            );

            binding.edtAdminCaseNote.setText(
                    savedInstanceState.getString(STATE_NOTE, "")
            );
        }
    }

    private void showResolution(BookingCase bookingCase) {
        binding.inputAdminCaseDecision.setVisibility(View.GONE);
        binding.inputAdminCaseNote.setVisibility(View.GONE);

        String result = "Kết luận: " + bookingCase.getDecisionLabel();

        if (!bookingCase.getResolutionNote().isEmpty()) {
            result += "\n\nGhi chú: " + bookingCase.getResolutionNote();
        }

        if (!bookingCase.getResolvedAtLabel().isEmpty()) {
            result += "\n\nĐã xử lý: " + bookingCase.getResolvedAtLabel();
        }

        binding.tvAdminCaseDetailResolution.setText(result);
        binding.tvAdminCaseDetailResolution.setVisibility(View.VISIBLE);
    }

    @Override
    public void onStart() {
        super.onStart();

        AlertDialog dialog = (AlertDialog) getDialog();

        if (dialog == null) {
            return;
        }

        if (binding != null) {
            KeyboardUtils.setupTapOutside(binding.getRoot());
        }

        if (editable && binding != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(view -> submitResolution());
        }

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setOnClickListener(view -> {
                    if (binding != null) {
                        KeyboardUtils.hide(binding.getRoot());
                    }

                    dismiss();
                });

        if (!editable) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(view -> {
                        if (binding != null) {
                            KeyboardUtils.hide(binding.getRoot());
                        }

                        dismiss();
                    });
        }
    }
    private void submitResolution() {
        if (binding == null || submitting || !editable) {
            return;
        }

        binding.inputAdminCaseDecision.setError(null);
        binding.inputAdminCaseNote.setError(null);

        String label = binding.dropdownAdminCaseDecision
                .getText()
                .toString()
                .trim();

        BookingCase.Decision decision = findDecision(label);

        if (decision == null) {
            binding.inputAdminCaseDecision.setError(
                    "Bạn hãy chọn kết luận xử lý."
            );
            binding.dropdownAdminCaseDecision.requestFocus();
            return;
        }

        String note = binding.edtAdminCaseNote.getText() == null
                ? ""
                : binding.edtAdminCaseNote.getText().toString().trim();

        if (note.isEmpty()) {
            binding.inputAdminCaseNote.setError(
                    "Bạn hãy nhập ghi chú xử lý."
            );
            binding.edtAdminCaseNote.requestFocus();
            return;
        }

        if (note.codePointCount(0, note.length()) > 2000) {
            binding.inputAdminCaseNote.setError(
                    "Ghi chú không được vượt quá 2000 ký tự."
            );
            binding.edtAdminCaseNote.requestFocus();
            return;
        }

        submitting = true;
        setSubmitEnabled(false);
        KeyboardUtils.hide(binding.getRoot());



        try {
            requireAdminSession();

            DemoBookingCaseRepository.getInstance().resolveDemoCase(
                    getArgument(ARG_CASE_ID),
                    getArgument(ARG_ADMIN_ID),
                    decision,
                    note
            );

            getParentFragmentManager().setFragmentResult(
                    RESULT_UPDATED,
                    new Bundle()
            );

            Toast.makeText(
                    requireContext(),
                    "Đã lưu kết luận demo. Chưa thực hiện chuyển tiền.",
                    Toast.LENGTH_LONG
            ).show();

            dismiss();
        } catch (RuntimeException exception) {
            submitting = false;
            setSubmitEnabled(true);

            if (isAdded()) {
                Toast.makeText(
                        requireContext(),
                        getErrorMessage(exception),
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    private void requireAdminSession() {
        SessionState session =
                SessionRepository.getInstance().getCurrentSession();

        String expectedAdminId = getArgument(ARG_ADMIN_ID);

        if (!session.isLoggedIn()
                || !session.hasRole(UserRole.ADMIN)
                || expectedAdminId.isEmpty()
                || !expectedAdminId.equals(session.getUserId())) {
            throw new IllegalStateException(
                    "Phiên Admin đã thay đổi. Vui lòng mở lại màn hình."
            );
        }
    }

    @Nullable
    private BookingCase.Decision findDecision(String label) {
        for (int index = 0; index < DECISION_LABELS.length; index++) {
            if (DECISION_LABELS[index].equals(label)) {
                return DECISIONS[index];
            }
        }

        return null;
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

    private String getErrorMessage(RuntimeException exception) {
        String message = exception.getMessage();

        return message == null || message.trim().isEmpty()
                ? "Không thể tải hoặc xử lý yêu cầu hỗ trợ."
                : message;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (binding != null && editable) {
            outState.putString(
                    STATE_DECISION,
                    binding.dropdownAdminCaseDecision.getText().toString()
            );

            outState.putString(
                    STATE_NOTE,
                    binding.edtAdminCaseNote.getText() == null
                            ? ""
                            : binding.edtAdminCaseNote.getText().toString()
            );
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        if (binding != null) {
            KeyboardUtils.hide(binding.getRoot());
            KeyboardUtils.clearTapOutside(binding.getRoot());

            binding.dropdownAdminCaseDecision
                    .setOnItemClickListener(null);
        }

        binding = null;
        super.onDestroyView();
    }
}