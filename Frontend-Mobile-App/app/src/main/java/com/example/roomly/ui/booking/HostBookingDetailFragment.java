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

import com.example.roomly.R;
import com.example.roomly.data.model.AppMode;
import com.example.roomly.data.model.Booking;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;
import com.example.roomly.data.repository.DemoBookingRepository;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentHostBookingDetailBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public final class HostBookingDetailFragment extends Fragment {

    private static final String ARG_BOOKING_ID = "booking_id";

    private FragmentHostBookingDetailBinding binding;
    private AlertDialog reviewDialog;

    private boolean processing;
    private boolean openingHandover;

    public static HostBookingDetailFragment newInstance(
            String bookingId
    ) {
        HostBookingDetailFragment fragment =
                new HostBookingDetailFragment();

        Bundle arguments = new Bundle();
        arguments.putString(ARG_BOOKING_ID, bookingId);
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
        binding = FragmentHostBookingDetailBinding.inflate(
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

        openingHandover = false;

        binding.btnHostBookingDetailBack.setOnClickListener(
                clickedView ->
                        getParentFragmentManager().popBackStack()
        );

        binding.btnHostBookingApprove.setOnClickListener(
                clickedView -> showReviewConfirmation(true)
        );

        binding.btnHostBookingReject.setOnClickListener(
                clickedView -> showReviewConfirmation(false)
        );

        binding.btnHostBookingHandover.setOnClickListener(
                clickedView -> openHandover()
        );

        loadBooking();
    }

    @Override
    public void onResume() {
        super.onResume();

        openingHandover = false;
        loadBooking();
    }

    private String getBookingId() {
        Bundle arguments = getArguments();

        return arguments == null
                ? null
                : arguments.getString(ARG_BOOKING_ID);
    }

    private boolean hasHostAccess() {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        return session.isLoggedIn()
                && session.hasRole(UserRole.HOST);
    }

    private boolean canPerformAction() {
        return binding != null
                && isAdded()
                && !processing
                && !openingHandover
                && !getParentFragmentManager().isStateSaved();
    }

    private void loadBooking() {
        if (binding == null) {
            return;
        }

        if (!hasHostAccess()) {
            closeReviewDialog();

            showError(
                    "Màn hình này dành cho tài khoản Chủ trọ. "
                            + "Vui lòng quay lại Cá nhân "
                            + "và chọn tài khoản thử phù hợp."
            );
            return;
        }

        try {
            Booking booking = DemoBookingRepository.getInstance()
                    .getBookingById(getBookingId());

            if (booking == null || !booking.isDemo()) {
                showError("Không tìm thấy yêu cầu thuê demo.");
                return;
            }

            renderBooking(booking);
        } catch (RuntimeException exception) {
            showError(
                    "Không thể tải thông tin yêu cầu thuê. "
                            + "Vui lòng quay lại và thử mở lại."
            );
        }
    }

    private void renderBooking(Booking booking) {
        binding.tvHostBookingDetailError.setVisibility(View.GONE);
        binding.layoutHostBookingDetailContent.setVisibility(
                View.VISIBLE
        );

        binding.tvHostBookingDetailCode.setText(
                "Mã yêu cầu: " + booking.getId()
        );

        binding.tvHostBookingDetailStatus.setText(
                booking.getStatusLabel()
        );

        binding.tvHostBookingDetailStatusDescription.setText(
                booking.getStatusDescription()
        );

        binding.tvHostBookingDetailRoomTitle.setText(
                booking.getRoomTitle()
        );

        binding.tvHostBookingDetailRoomAddress.setText(
                booking.getRoomAddress().isEmpty()
                        ? "Chưa cung cấp địa chỉ"
                        : booking.getRoomAddress()
        );

        binding.tvHostBookingDetailRent.setText(
                "Giá thuê: " + booking.getMonthlyRentLabel()
        );

        binding.tvHostBookingDetailDeposit.setText(
                "Tiền cọc: " + booking.getDepositLabel()
        );

        binding.tvHostBookingDetailMoveIn.setText(
                "Ngày dự kiến vào ở: "
                        + booking.getDesiredMoveInLabel()
        );

        binding.tvHostBookingDetailOccupants.setText(
                "Số người ở: " + booking.getOccupantCount()
        );

        binding.tvHostBookingDetailCreatedAt.setText(
                "Ngày tạo: " + booking.getCreatedAtLabel()
        );

        binding.tvHostBookingDetailNote.setText(
                booking.getNote().isEmpty()
                        ? "Không có ghi chú."
                        : booking.getNote()
        );

        boolean canReview = hasHostAccess()
                && booking.canReviewDemo();

        binding.layoutHostBookingDetailActions.setVisibility(
                canReview ? View.VISIBLE : View.GONE
        );

        binding.btnHostBookingApprove.setEnabled(
                canReview && !processing && !openingHandover
        );

        binding.btnHostBookingReject.setEnabled(
                canReview && !processing && !openingHandover
        );

        boolean canOpenHandover = hasHostAccess()
                && booking.isDemo()
                && isHandoverStatus(booking);

        binding.btnHostBookingHandover.setVisibility(
                canOpenHandover ? View.VISIBLE : View.GONE
        );

        binding.btnHostBookingHandover.setText(
                booking.getStatus() == Booking.Status.COMPLETED
                        ? "Xem bàn giao demo"
                        : "Bàn giao phòng demo"
        );

        binding.btnHostBookingHandover.setEnabled(
                canOpenHandover && !processing && !openingHandover
        );
    }

    private boolean isHandoverStatus(Booking booking) {
        return booking.getStatus() == Booking.Status.CONFIRMED
                || booking.getStatus() == Booking.Status.COMPLETED;
    }

    private void openHandover() {
        if (!canPerformAction() || reviewDialog != null) {
            return;
        }

        if (!hasHostAccess()) {
            loadBooking();
            return;
        }

        Booking booking = DemoBookingRepository.getInstance()
                .getBookingById(getBookingId());

        if (booking == null) {
            showError("Yêu cầu thuê không tồn tại.");
            return;
        }

        if (!booking.isDemo() || !isHandoverStatus(booking)) {
            renderBooking(booking);

            showMessage(
                    "Chỉ có thể mở bàn giao khi yêu cầu "
                            + "đã xác nhận hoặc hoàn tất."
            );
            return;
        }

        openingHandover = true;
        binding.btnHostBookingHandover.setEnabled(false);

        getParentFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                        R.id.fragment_container,
                        BookingHandoverFragment.newInstance(
                                booking.getId(),
                                AppMode.HOST
                        )
                )
                .addToBackStack(null)
                .commit();
    }

    private void showReviewConfirmation(boolean approve) {
        if (!canPerformAction() || reviewDialog != null) {
            return;
        }

        if (!hasHostAccess()) {
            loadBooking();
            return;
        }

        Booking booking = DemoBookingRepository.getInstance()
                .getBookingById(getBookingId());

        if (booking == null) {
            showError("Yêu cầu thuê không tồn tại.");
            return;
        }

        if (!booking.canReviewDemo()) {
            renderBooking(booking);
            showMessage("Yêu cầu không còn ở trạng thái Chờ duyệt.");
            return;
        }

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        String reviewerId = session.getUserId();

        String message = approve
                ? "Bạn muốn duyệt yêu cầu thuê phòng “"
                  + booking.getRoomTitle()
                  + "”?\n\nYêu cầu sẽ chuyển sang Đã duyệt. "
                  + "Người thuê cần thực hiện bước xác nhận "
                  + "hoặc thanh toán cọc tiếp theo."
                : "Bạn muốn từ chối yêu cầu thuê phòng “"
                  + booking.getRoomTitle()
                  + "”?\n\nYêu cầu sẽ chuyển sang Bị từ chối "
                  + "và vẫn được giữ trong danh sách.";

        message += "\n\nĐây là thao tác demo, "
                + "chưa gửi thông báo thực tế.";

        reviewDialog = new MaterialAlertDialogBuilder(
                requireContext()
        )
                .setTitle(
                        approve
                                ? "Duyệt yêu cầu demo?"
                                : "Từ chối yêu cầu demo?"
                )
                .setMessage(message)
                .setNegativeButton("Quay lại", null)
                .setPositiveButton(
                        approve ? "Duyệt demo" : "Từ chối demo",
                        null
                )
                .create();

        reviewDialog.setOnDismissListener(
                dialog -> reviewDialog = null
        );

        reviewDialog.show();

        reviewDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                        clickedView -> reviewRequest(
                                approve,
                                reviewerId
                        )
                );
    }

    private void reviewRequest(
            boolean approve,
            String reviewerId
    ) {
        if (!canPerformAction()) {
            return;
        }

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()
                || !session.hasRole(UserRole.HOST)
                || reviewerId == null
                || !reviewerId.equals(session.getUserId())) {
            closeReviewDialog();
            loadBooking();

            showMessage(
                    "Tài khoản đã thay đổi. "
                            + "Vui lòng kiểm tra lại trước khi xử lý."
            );
            return;
        }

        processing = true;

        binding.btnHostBookingApprove.setEnabled(false);
        binding.btnHostBookingReject.setEnabled(false);

        if (reviewDialog != null) {
            reviewDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setEnabled(false);
        }

        try {
            Booking updated;

            if (approve) {
                updated = DemoBookingRepository.getInstance()
                        .approveDemoRequest(getBookingId());
            } else {
                updated = DemoBookingRepository.getInstance()
                        .rejectDemoRequest(getBookingId());
            }

            processing = false;
            closeReviewDialog();
            renderBooking(updated);

            showMessage(
                    approve
                            ? "Đã duyệt yêu cầu demo."
                            : "Đã từ chối yêu cầu demo."
            );
        } catch (IllegalArgumentException | IllegalStateException exception) {
            processing = false;
            closeReviewDialog();
            loadBooking();

            showMessage(
                    exception.getMessage() == null
                            ? "Không thể xử lý yêu cầu demo."
                            : exception.getMessage()
            );
        }
    }

    private void showError(String message) {
        if (binding == null) {
            return;
        }

        binding.layoutHostBookingDetailContent.setVisibility(
                View.GONE
        );

        binding.tvHostBookingDetailError.setVisibility(View.VISIBLE);
        binding.tvHostBookingDetailError.setText(message);
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

    private void closeReviewDialog() {
        if (reviewDialog != null) {
            AlertDialog dialog = reviewDialog;
            reviewDialog = null;

            dialog.setOnDismissListener(null);
            dialog.dismiss();
        }
    }

    @Override
    public void onDestroyView() {
        closeReviewDialog();

        if (binding != null) {
            binding.btnHostBookingDetailBack.setOnClickListener(null);
            binding.btnHostBookingApprove.setOnClickListener(null);
            binding.btnHostBookingReject.setOnClickListener(null);
            binding.btnHostBookingHandover.setOnClickListener(null);
        }

        processing = false;
        openingHandover = false;
        binding = null;

        super.onDestroyView();
    }
}