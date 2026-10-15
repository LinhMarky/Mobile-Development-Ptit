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
import com.example.roomly.data.model.Booking;
import com.example.roomly.data.repository.BookingRepository;
import com.example.roomly.databinding.FragmentBookingPaymentBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

public final class BookingPaymentFragment extends Fragment {
    private FragmentBookingPaymentBinding binding;
    private final BookingRepository repository = new BookingRepository();
    private JSONObject current;
    private boolean processing;
    private JSONObject payment;

    public static BookingPaymentFragment newInstance(String bookingId) {
        BookingPaymentFragment fragment = new BookingPaymentFragment();
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
        binding = FragmentBookingPaymentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.btnPaymentBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.btnPaymentRetry.setOnClickListener(v -> acceptTerms());
        binding.btnPaymentDemoSuccess.setText("Mô phỏng thanh toán thành công");
        binding.btnPaymentDemoFailure.setText("Mô phỏng thanh toán thất bại");
        binding.btnPaymentDemoExpired.setVisibility(View.GONE);
        binding.btnPaymentDemoSuccess.setOnClickListener(v -> simulate("SUCCEEDED"));
        binding.btnPaymentDemoFailure.setOnClickListener(v -> simulate("FAILED"));
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
        binding.layoutPaymentContent.setVisibility(View.VISIBLE);
        binding.tvPaymentError.setVisibility(View.GONE);
        binding.tvPaymentRoomTitle.setText(booking.getRoomTitle());
        binding.tvPaymentBookingCode.setText("Yêu cầu #" + booking.getId());
        binding.tvPaymentAmount.setText(booking.getDepositLabel());
        binding.tvPaymentStatus.setText(booking.getStatusLabel());
        binding.tvPaymentStatusDescription.setText(
                "Thanh toán sandbox phục vụ báo cáo, không thu tiền thật. Giá và cọc áp dụng theo"
                        + " yêu cầu đã gửi.\n"
                        + ApiMapper.feeSummary(current));
        binding.btnPaymentRetry.setText("Đồng ý điều khoản và xác nhận cọc");
        binding.btnPaymentRetry.setVisibility(
                "APPROVED".equals(current.optString("status")) ? View.VISIBLE : View.GONE);
        binding.layoutPaymentDemoControls.setVisibility(View.GONE);
        ApiClient.execute(
                () ->
                        (JSONObject)
                                ApiClient.request("GET", "payments/booking/" + bookingId(), null),
                page -> {
                    if (binding == null) return;
                    JSONArray rows = page.optJSONArray("data");
                    payment = rows == null || rows.length() == 0 ? null : rows.optJSONObject(0);
                    renderPayment();
                },
                this::error);
    }

    private void renderPayment() {
        if (binding == null) return;
        boolean pending = payment != null && "PENDING".equals(payment.optString("status"));
        binding.layoutPaymentDemoControls.setVisibility(pending ? View.VISIBLE : View.GONE);
        binding.btnPaymentDemoSuccess.setEnabled(!processing);
        binding.btnPaymentDemoFailure.setEnabled(!processing);
        binding.btnPaymentRetry.setEnabled(!processing);
        binding.progressPayment.setVisibility(processing ? View.VISIBLE : View.GONE);
        if (payment != null)
            binding.tvPaymentStatus.setText(
                    "Phiên cọc #" + payment.optString("id") + ": " + payment.optString("status"));
    }

    private void acceptTerms() {
        if (processing || current == null) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xác nhận điều khoản thuê")
                .setMessage(
                        "Bạn đồng ý giá thuê, tiền cọc và các khoản phí trong yêu cầu này? Thanh"
                                + " toán hiện dùng sandbox để báo cáo.\n"
                                + ApiMapper.feeSummary(current))
                .setNegativeButton("Quay lại", null)
                .setPositiveButton("Tôi đồng ý", (dialog, which) -> createPayment())
                .show();
    }

    private void createPayment() {
        if (processing) return;
        processing = true;
        renderPayment();
        try {
            if (ApiMapper.money(current, "deposit_vnd") == 0) {
                repository.action(
                        bookingId(),
                        "confirm-deposit",
                        ApiClient.json("accepted_terms", true),
                        row -> {
                            processing = false;
                            if (binding != null) {
                                message("Đã xác nhận thuê, không yêu cầu cọc.");
                                load();
                            }
                        },
                        this::paymentError);
                return;
            }
        } catch (Exception exception) {
            paymentError("Tiền cọc không hợp lệ.");
            return;
        }
        ApiClient.execute(
                () ->
                        (JSONObject)
                                ApiClient.request(
                                        "POST",
                                        "payments",
                                        ApiClient.json(
                                                "booking_id", bookingId(), "accepted_terms", true)),
                row -> {
                    processing = false;
                    payment = row;
                    renderPayment();
                },
                this::paymentError);
    }

    private void simulate(String status) {
        if (processing || payment == null) return;
        processing = true;
        renderPayment();
        String id = payment.optString("id");
        ApiClient.execute(
                () ->
                        ApiClient.request(
                                "POST",
                                "payments/" + id + "/simulate",
                                ApiClient.json("status", status)),
                row -> {
                    processing = false;
                    if (binding != null) load();
                },
                this::paymentError);
    }

    private void paymentError(String text) {
        processing = false;
        renderPayment();
        error(text);
    }

    private void error(String text) {
        if (binding == null) return;
        binding.tvPaymentError.setText(text);
        binding.tvPaymentError.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
