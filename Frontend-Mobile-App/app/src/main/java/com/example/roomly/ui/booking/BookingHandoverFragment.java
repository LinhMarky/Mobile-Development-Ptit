package com.example.roomly.ui.booking;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.roomly.data.model.AppMode;
import com.example.roomly.data.model.Booking;
import com.example.roomly.data.model.BookingHandover;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;
import com.example.roomly.data.repository.DemoBookingHandoverRepository;
import com.example.roomly.data.repository.DemoBookingRepository;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentBookingHandoverBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public final class BookingHandoverFragment extends Fragment {

    private static final String ARG_BOOKING_ID = "booking_id";
    private static final String ARG_ACTOR_MODE = "actor_mode";
    private static final String ARG_USER_ID = "actor_user_id";

    private FragmentBookingHandoverBinding binding;
    private AlertDialog confirmDialog;

    private BookingHandover currentHandover;
    private AppMode actorMode;
    private boolean processing;

    public static BookingHandoverFragment newInstance(
            String bookingId,
            AppMode actorMode
    ) {
        if (actorMode != AppMode.HOST
                && actorMode != AppMode.TENANT) {
            throw new IllegalArgumentException(
                    "Vai trò bàn giao không hợp lệ."
            );
        }

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        BookingHandoverFragment fragment =
                new BookingHandoverFragment();

        Bundle arguments = new Bundle();
        arguments.putString(ARG_BOOKING_ID, bookingId);
        arguments.putString(ARG_ACTOR_MODE, actorMode.name());
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
        binding = FragmentBookingHandoverBinding.inflate(
                inflater, container, false
        );

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        Bundle arguments = getArguments();

        if (arguments != null) {
            String mode = arguments.getString(ARG_ACTOR_MODE);

            if (AppMode.HOST.name().equals(mode)) {
                actorMode = AppMode.HOST;
            } else if (AppMode.TENANT.name().equals(mode)) {
                actorMode = AppMode.TENANT;
            }
        }

        binding.btnHandoverBack.setOnClickListener(
                clickedView ->
                        getParentFragmentManager().popBackStack()
        );

        binding.checkHandoverAcknowledgement
                .setOnCheckedChangeListener(
                        (button, checked) -> updateConfirmButton()
                );

        binding.btnHandoverConfirm.setOnClickListener(
                clickedView -> showConfirmation()
        );

        loadHandover();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHandover();
    }

    private String getArgument(String key) {
        Bundle arguments = getArguments();

        return arguments == null
                ? null
                : arguments.getString(key);
    }

    private boolean hasActorAccess() {
        if (actorMode == null) {
            return false;
        }

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        String expectedUserId = getArgument(ARG_USER_ID);

        UserRole requiredRole = actorMode == AppMode.HOST
                ? UserRole.HOST
                : UserRole.TENANT;

        return session.isLoggedIn()
                && expectedUserId != null
                && expectedUserId.equals(session.getUserId())
                && session.hasRole(requiredRole);
    }

    private void loadHandover() {
        if (binding == null) {
            return;
        }

        if (!hasActorAccess()) {
            closeConfirmDialog();

            showError(
                    "Tài khoản không phù hợp hoặc đã thay đổi. "
                            + "Vui lòng quay lại và mở lại bàn giao."
            );
            return;
        }

        try {
            String bookingId = getArgument(ARG_BOOKING_ID);

            Booking booking = DemoBookingRepository.getInstance()
                    .getBookingById(bookingId);

            if (booking == null) {
                showError("Không tìm thấy yêu cầu thuê.");
                return;
            }

            currentHandover =
                    DemoBookingHandoverRepository.getInstance()
                            .getOrCreateHandover(bookingId);

            // Khôi phục trạng thái yêu cầu nếu hai xác nhận đã đủ.
            if (currentHandover.isCompleted()
                    && booking.getStatus() == Booking.Status.CONFIRMED) {
                booking = DemoBookingRepository.getInstance()
                        .completeDemoRequest(bookingId);
            }

            renderHandover(booking);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showError(
                    exception.getMessage() == null
                            ? "Không thể tải bàn giao demo."
                            : exception.getMessage()
            );
        }
    }

    private void renderHandover(Booking booking) {
        binding.tvHandoverError.setVisibility(View.GONE);
        binding.layoutHandoverContent.setVisibility(View.VISIBLE);

        binding.tvHandoverBookingCode.setText(
                "Mã yêu cầu: " + booking.getId()
        );

        binding.tvHandoverRoomTitle.setText(
                booking.getRoomTitle()
        );

        binding.tvHandoverRoomAddress.setText(
                booking.getRoomAddress().isEmpty()
                        ? "Chưa cung cấp địa chỉ"
                        : booking.getRoomAddress()
        );

        binding.tvHandoverMoveIn.setText(
                "Ngày dự kiến vào ở: "
                        + booking.getDesiredMoveInLabel()
        );

        binding.tvHandoverStatus.setText(
                currentHandover.getStatusLabel()
        );

        binding.tvHandoverHostConfirmation.setText(
                currentHandover.isHostConfirmed()
                        ? "Chủ trọ: Đã xác nhận bàn giao"
                        : "Chủ trọ: Chưa xác nhận"
        );

        binding.tvHandoverTenantConfirmation.setText(
                currentHandover.isTenantConfirmed()
                        ? "Người thuê: Đã xác nhận nhận phòng"
                        : "Người thuê: Chưa xác nhận"
        );

        String description =
                currentHandover.getStatusDescription();

        if (currentHandover.isCompleted()) {
            description += "\nYêu cầu thuê đã chuyển sang Hoàn tất.";
        } else {
            description += actorMode == AppMode.HOST
                    ? "\nBạn đang xác nhận với vai trò Chủ trọ."
                    : "\nBạn đang xác nhận với vai trò Người thuê.";
        }

        binding.tvHandoverDescription.setText(description);

        boolean canConfirm = canConfirmCurrentSide();

        binding.checkHandoverAcknowledgement.setVisibility(
                canConfirm ? View.VISIBLE : View.GONE
        );

        binding.btnHandoverConfirm.setVisibility(
                canConfirm ? View.VISIBLE : View.GONE
        );

        binding.btnHandoverConfirm.setText(
                actorMode == AppMode.HOST
                        ? "Xác nhận bàn giao demo"
                        : "Xác nhận nhận phòng demo"
        );

        if (!canConfirm) {
            binding.checkHandoverAcknowledgement.setChecked(false);
        }

        updateConfirmButton();
    }

    private boolean canConfirmCurrentSide() {
        if (currentHandover == null || !hasActorAccess()) {
            return false;
        }

        return actorMode == AppMode.HOST
                ? currentHandover.canHostConfirmDemo()
                : currentHandover.canTenantConfirmDemo();
    }

    private void updateConfirmButton() {
        if (binding == null) {
            return;
        }

        binding.checkHandoverAcknowledgement.setEnabled(
                !processing && canConfirmCurrentSide()
        );

        binding.btnHandoverConfirm.setEnabled(
                !processing
                        && canConfirmCurrentSide()
                        && binding.checkHandoverAcknowledgement.isChecked()
        );
    }

    private void showConfirmation() {
        if (binding == null
                || !isAdded()
                || processing
                || confirmDialog != null
                || getParentFragmentManager().isStateSaved()
                || !binding.checkHandoverAcknowledgement.isChecked()) {
            return;
        }

        if (!canConfirmCurrentSide()) {
            loadHandover();
            return;
        }

        confirmDialog = new MaterialAlertDialogBuilder(
                requireContext()
        )
                .setTitle(
                        actorMode == AppMode.HOST
                                ? "Xác nhận bàn giao demo?"
                                : "Xác nhận nhận phòng demo?"
                )
                .setMessage(
                        "Xác nhận của bạn sẽ được ghi nhận trong app. "
                                + "Khi cả hai bên xác nhận, "
                                + "yêu cầu thuê chuyển sang Hoàn tất.\n\n"
                                + "Đây là thao tác demo, "
                                + "không thay thế biên bản bàn giao thực tế."
                )
                .setNegativeButton("Quay lại", null)
                .setPositiveButton("Xác nhận demo", null)
                .create();

        confirmDialog.setOnDismissListener(
                dialog -> confirmDialog = null
        );

        confirmDialog.show();

        confirmDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                        clickedView -> confirmHandover()
                );
    }

    private void confirmHandover() {
        if (binding == null
                || !isAdded()
                || processing
                || getParentFragmentManager().isStateSaved()) {
            return;
        }

        if (!hasActorAccess()
                || !binding.checkHandoverAcknowledgement.isChecked()) {
            closeConfirmDialog();
            loadHandover();
            return;
        }

        processing = true;
        updateConfirmButton();

        if (confirmDialog != null) {
            confirmDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setEnabled(false);
        }

        try {
            currentHandover =
                    DemoBookingHandoverRepository.getInstance()
                            .confirmDemo(
                                    getArgument(ARG_BOOKING_ID),
                                    actorMode,
                                    getArgument(ARG_USER_ID)
                            );

            if (currentHandover.isCompleted()) {
                DemoBookingRepository.getInstance()
                        .completeDemoRequest(
                                currentHandover.getBookingId()
                        );
            }

            processing = false;
            closeConfirmDialog();

            binding.checkHandoverAcknowledgement.setChecked(false);
            loadHandover();

            showMessage(
                    currentHandover != null
                            && currentHandover.isCompleted()
                            ? "Đã hoàn tất bàn giao demo."
                            : "Đã ghi nhận xác nhận của bạn. "
                              + "Đang chờ bên còn lại."
            );
        } catch (IllegalArgumentException | IllegalStateException exception) {
            processing = false;
            closeConfirmDialog();
            loadHandover();

            showMessage(
                    exception.getMessage() == null
                            ? "Không thể xác nhận bàn giao demo."
                            : exception.getMessage()
            );
        }
    }

    private void showError(String message) {
        currentHandover = null;

        binding.layoutHandoverContent.setVisibility(View.GONE);
        binding.tvHandoverError.setVisibility(View.VISIBLE);
        binding.tvHandoverError.setText(message);
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
            binding.btnHandoverBack.setOnClickListener(null);
            binding.btnHandoverConfirm.setOnClickListener(null);
            binding.checkHandoverAcknowledgement
                    .setOnCheckedChangeListener(null);
        }

        processing = false;
        currentHandover = null;
        binding = null;

        super.onDestroyView();
    }
}