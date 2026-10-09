package com.example.roomly.ui.booking;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.roomly.data.model.Booking;
import com.example.roomly.data.model.BookingPayment;
import com.example.roomly.data.repository.DemoBookingPaymentRepository;
import com.example.roomly.data.repository.DemoBookingRepository;
import com.example.roomly.databinding.FragmentBookingPaymentBinding;

public final class BookingPaymentFragment extends Fragment {

    private static final String ARG_BOOKING_ID = "booking_id";

    private FragmentBookingPaymentBinding binding;
    private BookingPayment currentPayment;
    private boolean processing;

    public static BookingPaymentFragment newInstance(String bookingId) {
        BookingPaymentFragment fragment = new BookingPaymentFragment();

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
        binding = FragmentBookingPaymentBinding.inflate(
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

        binding.btnPaymentBack.setOnClickListener(
                clickedView ->
                        getParentFragmentManager().popBackStack()
        );

        binding.btnPaymentDemoSuccess.setOnClickListener(
                clickedView -> simulateResult(
                        BookingPayment.Status.SUCCESS
                )
        );

        binding.btnPaymentDemoFailure.setOnClickListener(
                clickedView -> simulateResult(
                        BookingPayment.Status.FAILED
                )
        );

        binding.btnPaymentDemoExpired.setOnClickListener(
                clickedView -> simulateResult(
                        BookingPayment.Status.EXPIRED
                )
        );

        binding.btnPaymentRetry.setOnClickListener(
                clickedView -> retryPayment()
        );

        loadPayment();
    }

    private String getBookingId() {
        Bundle arguments = getArguments();

        return arguments == null
                ? null
                : arguments.getString(ARG_BOOKING_ID);
    }

    private void loadPayment() {
        if (binding == null) {
            return;
        }

        String bookingId = getBookingId();

        if (bookingId == null || bookingId.trim().isEmpty()) {
            showError("Không tìm thấy mã yêu cầu thuê.");
            return;
        }

        try {
            Booking booking = DemoBookingRepository.getInstance()
                    .getBookingById(bookingId);

            if (booking == null) {
                showError("Yêu cầu thuê không tồn tại.");
                return;
            }

            currentPayment = DemoBookingPaymentRepository.getInstance()
                    .getOrCreatePayment(bookingId);

            binding.tvPaymentBookingCode.setText(
                    "Mã yêu cầu: " + booking.getId()
            );

            binding.tvPaymentRoomTitle.setText(
                    booking.getRoomTitle()
            );

            renderPayment();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showError(
                    exception.getMessage() == null
                            ? "Không thể tải thanh toán demo."
                            : exception.getMessage()
            );
        }
    }

    private void renderPayment() {
        if (binding == null || currentPayment == null) {
            return;
        }

        binding.tvPaymentError.setVisibility(View.GONE);
        binding.layoutPaymentContent.setVisibility(View.VISIBLE);

        binding.tvPaymentAmount.setText(
                currentPayment.getAmountLabel()
        );

        binding.tvPaymentStatus.setText(
                currentPayment.getStatusLabel()
        );

        binding.tvPaymentStatusDescription.setText(
                currentPayment.getStatusDescription()
        );

        binding.layoutPaymentDemoControls.setVisibility(
                currentPayment.canSimulateResult()
                        ? View.VISIBLE
                        : View.GONE
        );

        binding.btnPaymentRetry.setVisibility(
                currentPayment.canRetryDemo()
                        ? View.VISIBLE
                        : View.GONE
        );

        updateProcessingUi();
    }

    private void simulateResult(BookingPayment.Status result) {
        if (!canPerformAction()
                || currentPayment == null
                || !currentPayment.canSimulateResult()) {
            return;
        }

        processing = true;
        updateProcessingUi();

        try {
            currentPayment = DemoBookingPaymentRepository.getInstance()
                    .simulateResult(
                            currentPayment.getId(),
                            result
                    );

            processing = false;
            renderPayment();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            handleActionError(exception);
        }
    }

    private void retryPayment() {
        if (!canPerformAction()
                || currentPayment == null
                || !currentPayment.canRetryDemo()) {
            return;
        }

        processing = true;
        updateProcessingUi();

        try {
            currentPayment = DemoBookingPaymentRepository.getInstance()
                    .retryPayment(getBookingId());

            processing = false;
            renderPayment();

            showMessage("Đã tạo lượt thanh toán demo mới.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            handleActionError(exception);
        }
    }

    private boolean canPerformAction() {
        return binding != null
                && isAdded()
                && !processing
                && !getParentFragmentManager().isStateSaved();
    }

    private void updateProcessingUi() {
        if (binding == null) {
            return;
        }

        binding.progressPayment.setVisibility(
                processing ? View.VISIBLE : View.GONE
        );

        boolean canSimulate = !processing
                && currentPayment != null
                && currentPayment.canSimulateResult();

        binding.btnPaymentDemoSuccess.setEnabled(canSimulate);
        binding.btnPaymentDemoFailure.setEnabled(canSimulate);
        binding.btnPaymentDemoExpired.setEnabled(canSimulate);

        binding.btnPaymentRetry.setEnabled(
                !processing
                        && currentPayment != null
                        && currentPayment.canRetryDemo()
        );
    }

    private void handleActionError(RuntimeException exception) {
        processing = false;

        loadPayment();

        showMessage(
                exception.getMessage() == null
                        ? "Không thể thực hiện thao tác thanh toán demo."
                        : exception.getMessage()
        );
    }

    private void showError(String message) {
        if (binding == null) {
            return;
        }

        currentPayment = null;

        binding.layoutPaymentContent.setVisibility(View.GONE);
        binding.tvPaymentError.setVisibility(View.VISIBLE);
        binding.tvPaymentError.setText(message);
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

    @Override
    public void onDestroyView() {
        if (binding != null) {
            binding.btnPaymentBack.setOnClickListener(null);
            binding.btnPaymentDemoSuccess.setOnClickListener(null);
            binding.btnPaymentDemoFailure.setOnClickListener(null);
            binding.btnPaymentDemoExpired.setOnClickListener(null);
            binding.btnPaymentRetry.setOnClickListener(null);
        }

        processing = false;
        currentPayment = null;
        binding = null;

        super.onDestroyView();
    }
}