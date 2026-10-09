package com.example.roomly.ui.booking;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;

import com.example.roomly.data.model.Booking;
import com.example.roomly.data.model.BookingReview;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;
import com.example.roomly.data.repository.DemoBookingRepository;
import com.example.roomly.data.repository.DemoBookingReviewRepository;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentBookingReviewBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public final class BookingReviewFragment extends Fragment {

    private static final String ARG_BOOKING_ID = "booking_id";
    private static final String ARG_USER_ID = "review_user_id";

    private FragmentBookingReviewBinding binding;
    private AlertDialog confirmDialog;

    private BookingReview savedReview;
    private boolean processing;

    public static BookingReviewFragment newInstance(String bookingId) {
        BookingReviewFragment fragment = new BookingReviewFragment();

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        Bundle arguments = new Bundle();
        arguments.putString(ARG_BOOKING_ID, bookingId);
        arguments.putString(ARG_USER_ID, session.getUserId());

        fragment.setArguments(arguments);

        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentBookingReviewBinding.inflate(
                inflater,
                container,
                false
        );

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        // Cho phép chuyển focus khỏi ô nhận xét.
        binding.getRoot().setFocusableInTouchMode(true);

        setupTapOutsideToHideKeyboard();

        binding.btnReviewBack.setOnClickListener(clickedView -> {
            hideKeyboard();
            getParentFragmentManager().popBackStack();
        });

        binding.ratingReview.setOnRatingBarChangeListener(
                (ratingBar, rating, fromUser) ->
                        updateRatingLabel(Math.round(rating))
        );

        binding.btnReviewSubmit.setOnClickListener(
                clickedView -> showReviewConfirmation()
        );

        loadReview();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadReview();
    }

    @Override
    public void onViewStateRestored(
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewStateRestored(savedInstanceState);

        if (binding == null) {
            return;
        }

        if (savedReview != null) {
            renderSavedReview();
        } else {
            updateRatingLabel(
                    Math.round(binding.ratingReview.getRating())
            );
        }
    }

    private String getArgument(String key) {
        Bundle arguments = getArguments();

        return arguments == null
                ? null
                : arguments.getString(key);
    }

    private boolean hasTenantAccess() {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        String expectedUserId = getArgument(ARG_USER_ID);

        return session.isLoggedIn()
                && session.hasRole(UserRole.TENANT)
                && expectedUserId != null
                && expectedUserId.equals(session.getUserId());
    }

    private void loadReview() {
        if (binding == null) {
            return;
        }

        if (!hasTenantAccess()) {
            closeConfirmDialog();

            showError(
                    "Tài khoản không phù hợp hoặc đã thay đổi. "
                            + "Vui lòng quay lại và mở lại đánh giá."
            );
            return;
        }

        try {
            Booking booking = DemoBookingRepository.getInstance()
                    .getBookingById(getArgument(ARG_BOOKING_ID));

            if (booking == null || !booking.isDemo()) {
                showError("Không tìm thấy yêu cầu thuê demo.");
                return;
            }

            if (booking.getStatus() != Booking.Status.COMPLETED) {
                showError(
                        "Bạn chỉ có thể đánh giá "
                                + "sau khi yêu cầu thuê đã Hoàn tất."
                );
                return;
            }

            binding.tvReviewError.setVisibility(View.GONE);
            binding.layoutReviewContent.setVisibility(View.VISIBLE);

            binding.tvReviewBookingCode.setText(
                    "Mã yêu cầu: " + booking.getId()
            );

            binding.tvReviewRoomTitle.setText(
                    booking.getRoomTitle()
            );

            binding.tvReviewRoomAddress.setText(
                    booking.getRoomAddress().isEmpty()
                            ? "Chưa cung cấp địa chỉ"
                            : booking.getRoomAddress()
            );

            savedReview = DemoBookingReviewRepository.getInstance()
                    .getReview(
                            booking.getId(),
                            getArgument(ARG_USER_ID)
                    );

            if (savedReview != null) {
                renderSavedReview();
            } else {
                renderEditableForm();
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showError(
                    exception.getMessage() == null
                            ? "Không thể tải đánh giá demo."
                            : exception.getMessage()
            );
        }
    }

    private void renderEditableForm() {
        binding.ratingReview.setIsIndicator(false);
        binding.ratingReview.setEnabled(!processing);

        binding.inputReviewComment.setEnabled(!processing);
        binding.tvReviewSavedInfo.setVisibility(View.GONE);

        binding.btnReviewSubmit.setVisibility(View.VISIBLE);
        binding.btnReviewSubmit.setEnabled(!processing);

        updateRatingLabel(
                Math.round(binding.ratingReview.getRating())
        );
    }

    private void renderSavedReview() {
        hideKeyboard();

        binding.ratingReview.setRating(savedReview.getRating());
        binding.ratingReview.setIsIndicator(true);

        binding.edtReviewComment.setText(savedReview.getComment());
        binding.inputReviewComment.setError(null);
        binding.inputReviewComment.setEnabled(false);

        binding.tvReviewRatingLabel.setText(
                savedReview.getRatingLabel()
        );

        binding.tvReviewSavedInfo.setText(
                "Đã lưu đánh giá demo vào "
                        + savedReview.getCreatedAtLabel()
                        + ".\nĐánh giá chưa được đăng công khai."
        );

        binding.tvReviewSavedInfo.setVisibility(View.VISIBLE);
        binding.btnReviewSubmit.setVisibility(View.GONE);
    }

    private void updateRatingLabel(int rating) {
        if (binding == null) {
            return;
        }

        String label;

        switch (rating) {
            case 1:
                label = "1 sao — Rất không hài lòng";
                break;
            case 2:
                label = "2 sao — Không hài lòng";
                break;
            case 3:
                label = "3 sao — Bình thường";
                break;
            case 4:
                label = "4 sao — Hài lòng";
                break;
            case 5:
                label = "5 sao — Rất hài lòng";
                break;
            default:
                label = "Chọn số sao";
                break;
        }

        binding.tvReviewRatingLabel.setText(label);
    }

    private boolean canPerformAction() {
        return binding != null
                && isAdded()
                && !processing
                && savedReview == null
                && !getParentFragmentManager().isStateSaved();
    }

    private void showReviewConfirmation() {
        if (!canPerformAction() || confirmDialog != null) {
            return;
        }

        if (!hasTenantAccess()) {
            loadReview();
            return;
        }

        binding.inputReviewComment.setError(null);

        int rating = Math.round(binding.ratingReview.getRating());

        if (rating < 1 || rating > 5) {
            hideKeyboard();
            showMessage("Vui lòng chọn từ 1 đến 5 sao.");
            binding.ratingReview.requestFocus();
            return;
        }

        String comment = binding.edtReviewComment.getText() == null
                ? ""
                : binding.edtReviewComment.getText().toString().trim();

        if (comment.codePointCount(0, comment.length()) > 1000) {
            binding.inputReviewComment.setError(
                    "Nhận xét tối đa 1.000 ký tự."
            );
            binding.edtReviewComment.requestFocus();
            return;
        }

        hideKeyboard();

        confirmDialog = new MaterialAlertDialogBuilder(
                requireContext()
        )
                .setTitle("Lưu đánh giá demo?")
                .setMessage(
                        "Số sao: " + rating + "/5"
                                + "\n\nNhận xét: "
                                + (comment.isEmpty()
                                ? "Không có nhận xét."
                                : comment)
                                + "\n\nSau khi lưu, đánh giá demo "
                                + "sẽ chuyển sang chế độ chỉ xem. "
                                + "Chưa đăng công khai hoặc gửi đến chủ trọ."
                )
                .setNegativeButton("Sửa lại", null)
                .setPositiveButton("Lưu demo", null)
                .create();

        confirmDialog.setOnDismissListener(
                dialog -> confirmDialog = null
        );

        confirmDialog.show();

        confirmDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                        clickedView -> saveReview(rating, comment)
                );
    }

    private void saveReview(int rating, String comment) {
        if (!canPerformAction()) {
            return;
        }

        if (!hasTenantAccess()) {
            closeConfirmDialog();
            loadReview();
            return;
        }

        hideKeyboard();
        processing = true;

        binding.btnReviewSubmit.setEnabled(false);
        binding.ratingReview.setEnabled(false);
        binding.inputReviewComment.setEnabled(false);

        if (confirmDialog != null) {
            confirmDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setEnabled(false);
        }

        try {
            savedReview = DemoBookingReviewRepository.getInstance()
                    .createDemoReview(
                            getArgument(ARG_BOOKING_ID),
                            getArgument(ARG_USER_ID),
                            rating,
                            comment
                    );

            processing = false;
            closeConfirmDialog();
            renderSavedReview();

            showMessage("Đã lưu đánh giá demo.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            processing = false;
            closeConfirmDialog();
            loadReview();

            showMessage(
                    exception.getMessage() == null
                            ? "Không thể lưu đánh giá demo."
                            : exception.getMessage()
            );
        }
    }

    private void setupTapOutsideToHideKeyboard() {
        attachOutsideTouchListener(binding.getRoot());
    }

    private void attachOutsideTouchListener(View view) {
        // Không can thiệp thao tác nhập hoặc chọn chữ.
        if (view == binding.edtReviewComment) {
            return;
        }

        view.setOnTouchListener((touchedView, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                hideKeyboardIfOutsideComment(event);
            }

            // Giữ thao tác cuộn, chọn sao và bấm nút.
            return false;
        });

        if (view instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) view;

            for (int index = 0; index < parent.getChildCount(); index++) {
                attachOutsideTouchListener(
                        parent.getChildAt(index)
                );
            }
        }
    }

    private void hideKeyboardIfOutsideComment(MotionEvent event) {
        if (binding == null
                || !binding.edtReviewComment.hasFocus()) {
            return;
        }

        Rect commentBounds = new Rect();

        boolean visible = binding.edtReviewComment
                .getGlobalVisibleRect(commentBounds);

        if (!visible || !commentBounds.contains(
                (int) event.getRawX(),
                (int) event.getRawY()
        )) {
            hideKeyboard();
        }
    }

    private void hideKeyboard() {
        if (binding == null) {
            return;
        }

        View root = binding.getRoot();

        WindowInsetsControllerCompat controller =
                ViewCompat.getWindowInsetsController(root);

        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.ime());
        }

        InputMethodManager inputMethodManager =
                (InputMethodManager) root.getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);

        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(
                    binding.edtReviewComment.getWindowToken(),
                    0
            );
        }

        binding.edtReviewComment.clearFocus();
        root.requestFocus();
    }

    private void clearOutsideTouchListeners(View view) {
        if (view == binding.edtReviewComment) {
            return;
        }

        view.setOnTouchListener(null);

        if (view instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) view;

            for (int index = 0; index < parent.getChildCount(); index++) {
                clearOutsideTouchListeners(
                        parent.getChildAt(index)
                );
            }
        }
    }

    private void showError(String message) {
        hideKeyboard();
        savedReview = null;

        binding.layoutReviewContent.setVisibility(View.GONE);
        binding.tvReviewError.setVisibility(View.VISIBLE);
        binding.tvReviewError.setText(message);
    }

    private void showMessage(String message) {
        if (isAdded()) {
            Toast.makeText(
                    requireContext(),
                    message,
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void closeConfirmDialog() {
        if (confirmDialog != null) {
            AlertDialog dialog = confirmDialog;
            confirmDialog = null;

            dialog.setOnDismissListener(null);
            dialog.dismiss();
        }
    }

    @Override
    public void onDestroyView() {
        closeConfirmDialog();

        if (binding != null) {
            hideKeyboard();
            clearOutsideTouchListeners(binding.getRoot());

            binding.btnReviewBack.setOnClickListener(null);
            binding.btnReviewSubmit.setOnClickListener(null);
            binding.ratingReview.setOnRatingBarChangeListener(null);
        }

        processing = false;
        savedReview = null;
        binding = null;

        super.onDestroyView();
    }
}