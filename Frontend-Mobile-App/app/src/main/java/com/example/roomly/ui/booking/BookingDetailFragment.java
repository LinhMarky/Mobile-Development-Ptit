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
import com.example.roomly.data.model.BookingPayment;
import com.example.roomly.data.model.BookingReview;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;
import com.example.roomly.data.repository.DemoBookingPaymentRepository;
import com.example.roomly.data.repository.DemoBookingRepository;
import com.example.roomly.data.repository.DemoBookingReviewRepository;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentBookingDetailBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public final class BookingDetailFragment extends Fragment {

    private static final String ARG_BOOKING_ID = "booking_id";

    private FragmentBookingDetailBinding binding;
    private AlertDialog actionDialog;

    private boolean processing;
    private boolean openingScreen;

    public static BookingDetailFragment newInstance(String bookingId) {
        BookingDetailFragment fragment = new BookingDetailFragment();

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
        binding = FragmentBookingDetailBinding.inflate(
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

        openingScreen = false;

        binding.btnBookingDetailBack.setOnClickListener(
                clickedView ->
                        getParentFragmentManager().popBackStack()
        );

        binding.btnBookingDetailCancel.setOnClickListener(
                clickedView -> showActionConfirmation(true)
        );

        binding.btnBookingDetailPayment.setOnClickListener(
                clickedView -> handlePaymentAction()
        );

        binding.btnBookingDetailHandover.setOnClickListener(
                clickedView -> openHandover()
        );

        binding.btnBookingDetailReview.setOnClickListener(
                clickedView -> openReview()
        );

        loadBooking();
    }

    @Override
    public void onResume() {
        super.onResume();

        openingScreen = false;
        loadBooking();
    }

    private String getBookingId() {
        Bundle arguments = getArguments();

        return arguments == null
                ? null
                : arguments.getString(ARG_BOOKING_ID);
    }

    private Booking getCurrentBooking() {
        return DemoBookingRepository.getInstance()
                .getBookingById(getBookingId());
    }

    private boolean hasTenantAccess() {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        return session.isLoggedIn()
                && session.hasRole(UserRole.TENANT);
    }

    private void loadBooking() {
        if (binding == null) {
            return;
        }

        try {
            Booking booking = getCurrentBooking();

            if (booking == null) {
                showError("Không tìm thấy yêu cầu thuê.");
                return;
            }

            renderBooking(booking);
        } catch (RuntimeException exception) {
            showError(
                    "Không thể tải yêu cầu thuê. "
                            + "Vui lòng quay lại và thử mở lại."
            );
        }
    }

    private void renderBooking(Booking booking) {
        binding.tvBookingDetailError.setVisibility(View.GONE);
        binding.layoutBookingDetailContent.setVisibility(View.VISIBLE);

        binding.tvBookingDetailDemo.setVisibility(
                booking.isDemo() ? View.VISIBLE : View.GONE
        );

        binding.tvBookingDetailCode.setText(
                "Mã yêu cầu: " + booking.getId()
        );

        binding.tvBookingDetailStatus.setText(
                booking.getStatusLabel()
        );

        binding.tvBookingDetailStatusDescription.setText(
                booking.getStatusDescription()
        );

        binding.tvBookingDetailRoomTitle.setText(
                booking.getRoomTitle()
        );

        binding.tvBookingDetailRoomAddress.setText(
                booking.getRoomAddress().isEmpty()
                        ? "Chưa cung cấp địa chỉ"
                        : booking.getRoomAddress()
        );

        binding.tvBookingDetailRent.setText(
                "Giá thuê: " + booking.getMonthlyRentLabel()
        );

        binding.tvBookingDetailDeposit.setText(
                "Tiền cọc: " + booking.getDepositLabel()
        );

        binding.tvBookingDetailMoveIn.setText(
                "Ngày dự kiến vào ở: "
                        + booking.getDesiredMoveInLabel()
        );

        binding.tvBookingDetailOccupants.setText(
                "Số người ở: " + booking.getOccupantCount()
        );

        binding.tvBookingDetailCreatedAt.setText(
                "Ngày tạo: " + booking.getCreatedAtLabel()
        );

        binding.tvBookingDetailNote.setText(
                booking.getNote().isEmpty()
                        ? "Không có ghi chú."
                        : booking.getNote()
        );

        boolean canCancel = hasTenantAccess()
                && booking.canCancelDemo();

        binding.btnBookingDetailCancel.setVisibility(
                canCancel ? View.VISIBLE : View.GONE
        );

        binding.btnBookingDetailCancel.setEnabled(
                canCancel && !processing && !openingScreen
        );

        renderPaymentSummary(booking);
        renderHandoverButton(booking);
        renderReviewButton(booking);
    }

    private void renderPaymentSummary(Booking booking) {
        binding.tvBookingDetailPaymentSummary.setVisibility(View.GONE);
        binding.btnBookingDetailPayment.setVisibility(View.GONE);

        if (!booking.isDemo()) {
            return;
        }

        BookingPayment payment =
                DemoBookingPaymentRepository.getInstance()
                        .getLatestPayment(booking.getId());

        boolean approved =
                booking.getStatus() == Booking.Status.APPROVED;

        if (payment != null) {
            showPaymentSummary(
                    "Thanh toán demo: " + payment.getStatusLabel()
                            + "\nKhông phát sinh giao dịch thực tế."
            );
        }

        if (isHandoverStatus(booking)) {
            if (payment != null
                    && payment.getStatus()
                    == BookingPayment.Status.SUCCESS) {
                showPaymentButton("Xem kết quả thanh toán demo");
            } else if (booking.getDepositVnd() != null
                    && booking.getDepositVnd() == 0L) {
                showPaymentSummary(
                        "Phòng không yêu cầu cọc. "
                                + "Không phát sinh thanh toán tiền cọc."
                );
            }

            return;
        }

        if (!approved) {
            return;
        }

        Long deposit = booking.getDepositVnd();

        if (deposit == null) {
            showPaymentSummary(
                    "Chưa cung cấp tiền cọc. "
                            + "Chưa thể xác nhận hoặc thanh toán demo."
            );
            return;
        }

        if (deposit == 0L) {
            showPaymentSummary(
                    "Phòng không yêu cầu cọc. "
                            + "Bạn có thể xác nhận thuê demo "
                            + "mà không cần thanh toán."
            );

            showPaymentButton("Xác nhận thuê demo");
            return;
        }

        if (payment == null) {
            showPaymentSummary(
                    "Yêu cầu đã được duyệt. "
                            + "Bạn có thể thử thanh toán tiền cọc demo."
            );
        }

        showPaymentButton(
                payment == null
                        ? "Thanh toán tiền cọc demo"
                        : "Mở thanh toán demo"
        );
    }

    private boolean isHandoverStatus(Booking booking) {
        return booking.getStatus() == Booking.Status.CONFIRMED
                || booking.getStatus() == Booking.Status.COMPLETED;
    }

    private void renderHandoverButton(Booking booking) {
        boolean visible = booking.isDemo()
                && isHandoverStatus(booking)
                && hasTenantAccess();

        binding.btnBookingDetailHandover.setVisibility(
                visible ? View.VISIBLE : View.GONE
        );

        binding.btnBookingDetailHandover.setText(
                booking.getStatus() == Booking.Status.COMPLETED
                        ? "Xem bàn giao demo"
                        : "Bàn giao phòng demo"
        );

        binding.btnBookingDetailHandover.setEnabled(
                visible && !processing && !openingScreen
        );
    }

    private void renderReviewButton(Booking booking) {
        boolean visible = booking.isDemo()
                && booking.getStatus() == Booking.Status.COMPLETED
                && hasTenantAccess();

        binding.btnBookingDetailReview.setVisibility(
                visible ? View.VISIBLE : View.GONE
        );

        binding.btnBookingDetailReview.setEnabled(
                visible && !processing && !openingScreen
        );

        binding.btnBookingDetailReview.setText(
                "Đánh giá trải nghiệm thuê"
        );

        if (!visible) {
            return;
        }

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        BookingReview review =
                DemoBookingReviewRepository.getInstance()
                        .getReview(
                                booking.getId(),
                                session.getUserId()
                        );

        if (review != null) {
            binding.btnBookingDetailReview.setText(
                    "Xem đánh giá của tôi"
            );
        }
    }

    private void showPaymentSummary(String message) {
        binding.tvBookingDetailPaymentSummary.setText(message);
        binding.tvBookingDetailPaymentSummary.setVisibility(View.VISIBLE);
    }

    private void showPaymentButton(String label) {
        binding.btnBookingDetailPayment.setText(label);

        binding.btnBookingDetailPayment.setVisibility(
                hasTenantAccess() ? View.VISIBLE : View.GONE
        );

        binding.btnBookingDetailPayment.setEnabled(
                hasTenantAccess() && !processing && !openingScreen
        );
    }

    private boolean canPerformAction() {
        return binding != null
                && isAdded()
                && !processing
                && !openingScreen
                && !getParentFragmentManager().isStateSaved();
    }

    private boolean checkTenantAccess() {
        if (hasTenantAccess()) {
            return true;
        }

        closeActionDialog();
        loadBooking();

        showMessage(
                "Vui lòng sử dụng tài khoản Người thuê "
                        + "để thực hiện thao tác."
        );

        return false;
    }

    private void openReview() {
        if (!canPerformAction()
                || actionDialog != null
                || !checkTenantAccess()) {
            return;
        }

        Booking booking = getCurrentBooking();

        if (booking == null) {
            showError("Yêu cầu thuê không tồn tại.");
            return;
        }

        if (!booking.isDemo()
                || booking.getStatus() != Booking.Status.COMPLETED) {
            renderBooking(booking);

            showMessage(
                    "Chỉ có thể đánh giá sau khi yêu cầu đã Hoàn tất."
            );
            return;
        }

        openScreen(
                BookingReviewFragment.newInstance(booking.getId())
        );
    }

    private void openHandover() {
        if (!canPerformAction()
                || actionDialog != null
                || !checkTenantAccess()) {
            return;
        }

        Booking booking = getCurrentBooking();

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

        openScreen(
                BookingHandoverFragment.newInstance(
                        booking.getId(),
                        AppMode.TENANT
                )
        );
    }

    private void handlePaymentAction() {
        if (!canPerformAction()
                || actionDialog != null
                || !checkTenantAccess()) {
            return;
        }

        Booking booking = getCurrentBooking();

        if (booking == null) {
            showError("Yêu cầu thuê không tồn tại.");
            return;
        }

        if (!booking.isDemo()) {
            showMessage("Chức năng này chỉ áp dụng cho dữ liệu demo.");
            return;
        }

        Long deposit = booking.getDepositVnd();

        if (booking.canConfirmDemo()
                && deposit != null
                && deposit == 0L) {
            showActionConfirmation(false);
            return;
        }

        BookingPayment payment =
                DemoBookingPaymentRepository.getInstance()
                        .getLatestPayment(booking.getId());

        boolean canPay =
                booking.getStatus() == Booking.Status.APPROVED
                        && deposit != null
                        && deposit > 0;

        boolean canViewResult =
                isHandoverStatus(booking)
                        && payment != null
                        && payment.getStatus()
                        == BookingPayment.Status.SUCCESS;

        if (!canPay && !canViewResult) {
            renderBooking(booking);
            showMessage("Chưa thể mở thanh toán cho yêu cầu này.");
            return;
        }

        openScreen(
                BookingPaymentFragment.newInstance(booking.getId())
        );
    }

    private void openScreen(Fragment fragment) {
        if (!canPerformAction()) {
            return;
        }

        openingScreen = true;

        binding.btnBookingDetailPayment.setEnabled(false);
        binding.btnBookingDetailHandover.setEnabled(false);
        binding.btnBookingDetailReview.setEnabled(false);
        binding.btnBookingDetailCancel.setEnabled(false);

        getParentFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void showActionConfirmation(boolean cancel) {
        if (!canPerformAction()
                || actionDialog != null
                || !checkTenantAccess()) {
            return;
        }

        Booking booking = getCurrentBooking();

        if (booking == null) {
            showError("Yêu cầu thuê không tồn tại.");
            return;
        }

        Long deposit = booking.getDepositVnd();

        boolean allowed = cancel
                ? booking.canCancelDemo()
                : booking.canConfirmDemo()
                  && deposit != null
                  && deposit == 0L;

        if (!allowed) {
            renderBooking(booking);
            showMessage("Yêu cầu không còn phù hợp với thao tác này.");
            return;
        }

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        String expectedUserId = session.getUserId();

        String message = cancel
                ? "Bạn muốn hủy yêu cầu thuê phòng “"
                  + booking.getRoomTitle()
                  + "”?\n\nYêu cầu sẽ chuyển sang Đã hủy "
                  + "và vẫn được giữ trong danh sách."
                : "Bạn muốn xác nhận thuê phòng “"
                  + booking.getRoomTitle()
                  + "”?\n\nPhòng không yêu cầu cọc. "
                  + "Yêu cầu sẽ chuyển sang Đã xác nhận.";

        message += "\n\nThao tác chỉ thay đổi dữ liệu demo trong app.";

        actionDialog = new MaterialAlertDialogBuilder(
                requireContext()
        )
                .setTitle(
                        cancel
                                ? "Hủy yêu cầu demo?"
                                : "Xác nhận thuê demo?"
                )
                .setMessage(message)
                .setNegativeButton(
                        cancel ? "Giữ yêu cầu" : "Quay lại",
                        null
                )
                .setPositiveButton(
                        cancel ? "Hủy yêu cầu" : "Xác nhận demo",
                        null
                )
                .create();

        actionDialog.setOnDismissListener(
                dialog -> actionDialog = null
        );

        actionDialog.show();

        actionDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                        clickedView -> performConfirmedAction(
                                cancel,
                                expectedUserId
                        )
                );
    }

    private void performConfirmedAction(
            boolean cancel,
            String expectedUserId
    ) {
        if (!canPerformAction() || !checkTenantAccess()) {
            return;
        }

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (expectedUserId == null
                || !expectedUserId.equals(session.getUserId())) {
            closeActionDialog();
            loadBooking();
            showMessage("Tài khoản đã thay đổi. Vui lòng thử lại.");
            return;
        }

        processing = true;

        binding.btnBookingDetailCancel.setEnabled(false);
        binding.btnBookingDetailPayment.setEnabled(false);
        binding.btnBookingDetailHandover.setEnabled(false);
        binding.btnBookingDetailReview.setEnabled(false);

        if (actionDialog != null) {
            actionDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setEnabled(false);
        }

        try {
            Booking updated;

            if (cancel) {
                updated = DemoBookingRepository.getInstance()
                        .cancelDemoRequest(getBookingId());
            } else {
                Booking current = getCurrentBooking();

                if (current == null
                        || current.getDepositVnd() == null
                        || current.getDepositVnd() != 0L) {
                    throw new IllegalStateException(
                            "Thao tác này chỉ dành cho phòng không cần cọc."
                    );
                }

                updated = DemoBookingRepository.getInstance()
                        .confirmDemoRequest(getBookingId());
            }

            processing = false;
            closeActionDialog();
            renderBooking(updated);

            showMessage(
                    cancel
                            ? "Đã hủy yêu cầu demo."
                            : "Đã xác nhận thuê demo."
            );
        } catch (IllegalArgumentException | IllegalStateException exception) {
            processing = false;
            closeActionDialog();
            loadBooking();

            showMessage(
                    exception.getMessage() == null
                            ? "Không thể thực hiện thao tác."
                            : exception.getMessage()
            );
        }
    }

    private void showError(String message) {
        if (binding == null) {
            return;
        }

        binding.layoutBookingDetailContent.setVisibility(View.GONE);
        binding.tvBookingDetailError.setVisibility(View.VISIBLE);
        binding.tvBookingDetailError.setText(message);
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

    private void closeActionDialog() {
        if (actionDialog != null) {
            AlertDialog dialog = actionDialog;
            actionDialog = null;

            dialog.setOnDismissListener(null);
            dialog.dismiss();
        }
    }

    @Override
    public void onDestroyView() {
        closeActionDialog();

        if (binding != null) {
            binding.btnBookingDetailBack.setOnClickListener(null);
            binding.btnBookingDetailCancel.setOnClickListener(null);
            binding.btnBookingDetailPayment.setOnClickListener(null);
            binding.btnBookingDetailHandover.setOnClickListener(null);
            binding.btnBookingDetailReview.setOnClickListener(null);
        }

        processing = false;
        openingScreen = false;
        binding = null;

        super.onDestroyView();
    }
}