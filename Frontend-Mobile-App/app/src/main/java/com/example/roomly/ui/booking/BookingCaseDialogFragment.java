package com.example.roomly.ui.booking;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.roomly.data.model.Booking;
import com.example.roomly.data.model.BookingCase;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.repository.BookingRepository;
import com.example.roomly.data.repository.ReportRepository;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.DialogBookingCaseBinding;
import com.example.roomly.util.KeyboardUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public final class BookingCaseDialogFragment extends DialogFragment {

    public static final String TAG = "booking_case_dialog";

    private static final String ARG_BOOKING_ID = "booking_id";
    private static final String ARG_USER_ID = "user_id";

    private static final String STATE_TYPE = "selected_type";
    private static final String STATE_DESCRIPTION = "description";

    private static final BookingCase.Type[] TYPES = {
        BookingCase.Type.DEPOSIT_DISPUTE,
        BookingCase.Type.HANDOVER_OVERDUE,
        BookingCase.Type.NO_SHOW,
        BookingCase.Type.OTHER
    };

    private static final String[] TYPE_LABELS = {
        "Tranh chấp tiền cọc", "Bàn giao quá hạn", "Không đến hẹn", "Vấn đề khác"
    };

    private DialogBookingCaseBinding binding;
    private boolean submitting;
    private boolean editable;

    public static BookingCaseDialogFragment newInstance(String bookingId) {
        SessionState session = SessionRepository.getInstance().getCurrentSession();

        Bundle arguments = new Bundle();
        arguments.putString(ARG_BOOKING_ID, bookingId);
        arguments.putString(ARG_USER_ID, session.getUserId());

        BookingCaseDialogFragment fragment = new BookingCaseDialogFragment();

        fragment.setArguments(arguments);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        try {
            requireSameSession();

            Booking booking = BookingRepository.cachedBooking(getArgument(ARG_BOOKING_ID));

            if (booking == null) {
                throw new IllegalStateException("Không tìm thấy yêu cầu thuê.");
            }

            BookingCase existing =
                    ReportRepository.getInstance().getCaseByBookingId(booking.getId());

            if (existing != null) {
                return createExistingCaseDialog(existing);
            }

            return createFormDialog(booking, savedInstanceState);
        } catch (RuntimeException exception) {
            editable = false;

            return new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Yêu cầu hỗ trợ")
                    .setMessage(getErrorMessage(exception))
                    .setPositiveButton("Đóng", null)
                    .create();
        }
    }

    private Dialog createExistingCaseDialog(BookingCase bookingCase) {
        editable = false;

        String message =
                "Loại vấn đề: "
                        + bookingCase.getTypeLabel()
                        + "\nTrạng thái: "
                        + bookingCase.getStatusLabel()
                        + "\nĐã gửi: "
                        + bookingCase.getCreatedAtLabel()
                        + "\n\nMô tả:\n"
                        + bookingCase.getDescription();

        if (!bookingCase.isOpen()) {
            message += "\n\nKết luận: " + bookingCase.getDecisionLabel();

            if (!bookingCase.getResolutionNote().isEmpty()) {
                message += "\n\nGhi chú xử lý:\n" + bookingCase.getResolutionNote();
            }

            if (!bookingCase.getResolvedAtLabel().isEmpty()) {
                message += "\n\nĐã xử lý: " + bookingCase.getResolvedAtLabel();
            }
        }

        message += "\n\nKết quả xử lý cọc áp dụng cho thanh toán sandbox, không chuyển tiền thật.";

        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Yêu cầu hỗ trợ hiện tại")
                .setMessage(message)
                .setPositiveButton("Đóng", null)
                .create();
    }

    private Dialog createFormDialog(Booking booking, @Nullable Bundle savedInstanceState) {
        editable = true;

        binding = DialogBookingCaseBinding.inflate(getLayoutInflater());

        binding.tvCaseRoomTitle.setText(booking.getRoomTitle());
        binding.tvCaseBookingCode.setText("Mã yêu cầu thuê: " + booking.getId());

        binding.dropdownCaseType.setAdapter(
                new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        TYPE_LABELS));

        binding.dropdownCaseType.setOnItemClickListener(
                (parent, view, position, id) -> binding.inputCaseType.setError(null));

        if (savedInstanceState != null) {
            binding.dropdownCaseType.setText(savedInstanceState.getString(STATE_TYPE, ""), false);

            binding.edtCaseDescription.setText(savedInstanceState.getString(STATE_DESCRIPTION, ""));
        }

        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Yêu cầu hỗ trợ")
                .setView(binding.getRoot())
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Gửi yêu cầu", null)
                .create();
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
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> submitCase());

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setOnClickListener(
                            view -> {
                                KeyboardUtils.hide(binding.getRoot());
                                dismiss();
                            });
        }
    }

    private void submitCase() {
        if (binding == null || submitting) {
            return;
        }

        binding.inputCaseType.setError(null);
        binding.inputCaseDescription.setError(null);

        String selectedLabel = binding.dropdownCaseType.getText().toString().trim();

        BookingCase.Type selectedType = findType(selectedLabel);

        if (selectedType == null) {
            binding.inputCaseType.setError("Bạn hãy chọn vấn đề cần hỗ trợ.");
            binding.dropdownCaseType.requestFocus();
            return;
        }

        String description =
                binding.edtCaseDescription.getText() == null
                        ? ""
                        : binding.edtCaseDescription.getText().toString().trim();

        int length = description.codePointCount(0, description.length());

        if (length < 10 || length > 3000) {
            binding.inputCaseDescription.setError("Mô tả phải có từ 10 đến 3000 ký tự.");
            binding.edtCaseDescription.requestFocus();
            return;
        }

        KeyboardUtils.hide(binding.getRoot());

        submitting = true;
        setSubmitEnabled(false);

        ReportRepository.getInstance()
                .submit(
                        "booking",
                        getArgument(ARG_BOOKING_ID),
                        selectedType.name(),
                        description,
                        () -> {
                            if (binding == null) return;
                            Toast.makeText(
                                            requireContext(),
                                            "Đã gửi yêu cầu hỗ trợ.",
                                            Toast.LENGTH_LONG)
                                    .show();
                            dismiss();
                        },
                        text -> {
                            submitting = false;
                            if (binding == null) return;
                            setSubmitEnabled(true);
                            Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show();
                        });
    }

    private void requireSameSession() {
        SessionState session = SessionRepository.getInstance().getCurrentSession();

        String expectedUserId = getArgument(ARG_USER_ID);

        if (!session.isLoggedIn()
                || expectedUserId.isEmpty()
                || !expectedUserId.equals(session.getUserId())) {
            throw new IllegalStateException(
                    "Phiên đăng nhập đã thay đổi. " + "Vui lòng mở lại yêu cầu hỗ trợ.");
        }
    }

    @Nullable
    private BookingCase.Type findType(String label) {
        for (int index = 0; index < TYPE_LABELS.length; index++) {
            if (TYPE_LABELS[index].equals(label)) {
                return TYPES[index];
            }
        }

        return null;
    }

    private void setSubmitEnabled(boolean enabled) {
        AlertDialog dialog = (AlertDialog) getDialog();

        if (dialog != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(enabled);
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
                ? "Không thể tải hoặc gửi yêu cầu hỗ trợ."
                : message;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (binding != null) {
            outState.putString(STATE_TYPE, binding.dropdownCaseType.getText().toString());

            outState.putString(
                    STATE_DESCRIPTION,
                    binding.edtCaseDescription.getText() == null
                            ? ""
                            : binding.edtCaseDescription.getText().toString());
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        if (binding != null) {
            KeyboardUtils.hide(binding.getRoot());
            KeyboardUtils.clearTapOutside(binding.getRoot());

            binding.dropdownCaseType.setOnItemClickListener(null);
        }

        binding = null;
        super.onDestroyView();
    }
}
