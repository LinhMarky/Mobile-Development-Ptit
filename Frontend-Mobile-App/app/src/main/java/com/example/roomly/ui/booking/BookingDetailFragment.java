package com.example.roomly.ui.booking;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.roomly.R;
import com.example.roomly.data.api.ApiClient;
import com.example.roomly.data.api.ApiMapper;
import com.example.roomly.data.model.AppMode;
import com.example.roomly.data.model.Booking;
import com.example.roomly.data.repository.BookingRepository;
import com.example.roomly.data.repository.ReportRepository;
import com.example.roomly.databinding.FragmentBookingDetailBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONObject;

public final class BookingDetailFragment extends Fragment {
    private FragmentBookingDetailBinding binding;
    private final BookingRepository repository = new BookingRepository();
    private JSONObject current;
    private boolean processing;

    public static BookingDetailFragment newInstance(String bookingId) {
        BookingDetailFragment fragment = new BookingDetailFragment();
        Bundle arguments = new Bundle();
        arguments.putString("booking_id", bookingId);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentBookingDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.btnBookingDetailBack.setOnClickListener(
                v -> getParentFragmentManager().popBackStack());
        binding.btnBookingDetailPayment.setOnClickListener(
                v -> open(BookingPaymentFragment.newInstance(bookingId())));
        binding.btnBookingDetailHandover.setOnClickListener(
                v -> open(BookingHandoverFragment.newInstance(bookingId(), AppMode.TENANT)));
        binding.btnBookingDetailReview.setOnClickListener(
                v -> open(BookingReviewFragment.newInstance(bookingId())));
        binding.btnBookingDetailCancel.setOnClickListener(
                v ->
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Hủy yêu cầu thuê?")
                                .setMessage(
                                        "Yêu cầu đang giữ chỗ sẽ trả phòng về trạng thái còn"
                                                + " trống.")
                                .setNegativeButton("Quay lại", null)
                                .setPositiveButton("Hủy yêu cầu", (dialog, which) -> cancel())
                                .show());
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private String bookingId() {
        return requireArguments().getString("booking_id", "");
    }

    private void load() {
        repository.fetchBooking(
                bookingId(),
                row -> {
                    if (binding == null) return;
                    current = row;
                    try {
                        render(ApiMapper.booking(row));
                    } catch (Exception exception) {
                        error("Dữ liệu yêu cầu thuê không hợp lệ.");
                    }
                },
                this::error);
    }

    private void open(Fragment fragment) {
        if (!isAdded() || getParentFragmentManager().isStateSaved()) return;
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void message(String text) {
        if (isAdded()) Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show();
    }

    private void render(Booking booking) {
        binding.layoutBookingDetailContent.setVisibility(View.VISIBLE);
        binding.tvBookingDetailError.setVisibility(View.GONE);
        binding.tvBookingDetailCode.setText("Mã yêu cầu: #" + booking.getId());
        binding.tvBookingDetailRoomTitle.setText(booking.getRoomTitle());
        binding.tvBookingDetailRoomAddress.setText(booking.getRoomAddress());
        binding.tvBookingDetailStatus.setText(booking.getStatusLabel());
        binding.tvBookingDetailRent.setText("Giá thuê: " + booking.getMonthlyRentLabel());
        binding.tvBookingDetailDeposit.setText("Tiền cọc: " + booking.getDepositLabel());
        binding.tvBookingDetailMoveIn.setText("Ngày vào ở: " + booking.getDesiredMoveInLabel());
        binding.tvBookingDetailOccupants.setText("Số người: " + booking.getOccupantCount());
        binding.tvBookingDetailCreatedAt.setText("Ngày gửi: " + booking.getCreatedAtLabel());
        binding.tvBookingDetailNote.setText(
                booking.getNote().isEmpty() ? "Không có ghi chú." : booking.getNote());
        binding.tvBookingDetailDemo.setVisibility(View.GONE);
        binding.tvBookingDetailStatusDescription.setText(booking.getStatusDescription());
        binding.tvBookingDetailPaymentSummary.setText(
                "Tiền cọc: " + booking.getDepositLabel() + "\n" + ApiMapper.feeSummary(current));
        String status = current.optString("status");
        binding.btnBookingDetailCancel.setVisibility(
                status.equals("PENDING") || status.equals("APPROVED") ? View.VISIBLE : View.GONE);
        binding.btnBookingDetailCancel.setEnabled(!processing);
        binding.btnBookingDetailCancel.setText("Hủy yêu cầu");
        binding.btnBookingDetailPayment.setVisibility(
                status.equals("APPROVED") ? View.VISIBLE : View.GONE);
        binding.btnBookingDetailPayment.setText("Xác nhận thuê và tiền cọc");
        binding.btnBookingDetailHandover.setVisibility(
                status.equals("CONFIRMED") || status.equals("COMPLETED")
                        ? View.VISIBLE
                        : View.GONE);
        binding.btnBookingDetailReview.setVisibility(
                status.equals("COMPLETED") ? View.VISIBLE : View.GONE);
        binding.btnBookingDetailCase.setVisibility(
                status.equals("CONFIRMED") || status.equals("COMPLETED")
                        ? View.VISIBLE
                        : View.GONE);
        binding.btnBookingDetailCase.setText("Yêu cầu hỗ trợ");
        binding.btnBookingDetailCase.setOnClickListener(
                v ->
                        ReportRepository.getInstance()
                                .fetchCases(
                                        bookingId(),
                                        null,
                                        cases -> {
                                            if (binding == null) return;
                                            BookingCaseDialogFragment.newInstance(bookingId())
                                                    .show(
                                                            getParentFragmentManager(),
                                                            BookingCaseDialogFragment.TAG);
                                        },
                                        this::error));
    }

    private void cancel() {
        if (processing) return;
        processing = true;
        binding.btnBookingDetailCancel.setEnabled(false);
        repository.action(
                bookingId(),
                "cancel",
                ApiClient.json("reason", "Người thuê hủy yêu cầu"),
                row -> {
                    processing = false;
                    if (binding != null) load();
                },
                text -> {
                    processing = false;
                    if (binding != null) binding.btnBookingDetailCancel.setEnabled(true);
                    error(text);
                });
    }

    private void error(String text) {
        if (binding == null) return;
        binding.tvBookingDetailError.setText(text);
        binding.tvBookingDetailError.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
