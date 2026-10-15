package com.example.roomly.ui.booking;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import com.example.roomly.databinding.FragmentHostBookingDetailBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONObject;

public final class HostBookingDetailFragment extends Fragment {
    private FragmentHostBookingDetailBinding binding;
    private final BookingRepository repository = new BookingRepository();
    private JSONObject current;
    private boolean processing;

    public static HostBookingDetailFragment newInstance(String bookingId) {
        HostBookingDetailFragment fragment = new HostBookingDetailFragment();
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
        binding = FragmentHostBookingDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.btnHostBookingDetailBack.setOnClickListener(
                v -> getParentFragmentManager().popBackStack());
        binding.btnHostBookingApprove.setOnClickListener(
                v ->
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Duyệt yêu cầu thuê?")
                                .setMessage(
                                        "Phòng được giữ trong 24 giờ để người thuê xác nhận cọc.")
                                .setNegativeButton("Quay lại", null)
                                .setPositiveButton(
                                        "Duyệt",
                                        (dialog, which) -> action("approve", new JSONObject()))
                                .show());
        binding.btnHostBookingReject.setOnClickListener(v -> reject());
        binding.btnHostBookingHandover.setOnClickListener(
                v -> open(BookingHandoverFragment.newInstance(bookingId(), AppMode.HOST)));
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
        binding.layoutHostBookingDetailContent.setVisibility(View.VISIBLE);
        binding.tvHostBookingDetailError.setVisibility(View.GONE);
        binding.tvHostBookingDetailCode.setText("Mã yêu cầu: #" + booking.getId());
        binding.tvHostBookingDetailRoomTitle.setText(booking.getRoomTitle());
        binding.tvHostBookingDetailRoomAddress.setText(booking.getRoomAddress());
        binding.tvHostBookingDetailStatus.setText(booking.getStatusLabel());
        binding.tvHostBookingDetailRent.setText("Giá thuê: " + booking.getMonthlyRentLabel());
        binding.tvHostBookingDetailDeposit.setText("Tiền cọc: " + booking.getDepositLabel());
        binding.tvHostBookingDetailMoveIn.setText("Ngày vào ở: " + booking.getDesiredMoveInLabel());
        binding.tvHostBookingDetailOccupants.setText("Số người: " + booking.getOccupantCount());
        binding.tvHostBookingDetailCreatedAt.setText("Ngày gửi: " + booking.getCreatedAtLabel());
        binding.tvHostBookingDetailNote.setText(
                booking.getNote().isEmpty() ? "Không có ghi chú." : booking.getNote());
        String status = current.optString("status");
        binding.layoutHostBookingDetailActions.setVisibility(
                status.equals("PENDING") ? View.VISIBLE : View.GONE);
        binding.btnHostBookingApprove.setEnabled(!processing);
        binding.btnHostBookingReject.setEnabled(!processing);
        binding.btnHostBookingApprove.setText("Duyệt yêu cầu");
        binding.btnHostBookingReject.setText("Từ chối");
        binding.btnHostBookingHandover.setVisibility(
                status.equals("CONFIRMED") || status.equals("COMPLETED")
                        ? View.VISIBLE
                        : View.GONE);
        binding.btnHostBookingCase.setVisibility(
                status.equals("CONFIRMED") || status.equals("COMPLETED")
                        ? View.VISIBLE
                        : View.GONE);
        binding.btnHostBookingCase.setText("Yêu cầu hỗ trợ");
        binding.btnHostBookingCase.setOnClickListener(
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

    private void reject() {
        EditText reason = new EditText(requireContext());
        reason.setHint("Lý do từ chối");
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Từ chối yêu cầu")
                .setView(reason)
                .setNegativeButton("Quay lại", null)
                .setPositiveButton(
                        "Từ chối",
                        (dialog, which) ->
                                action(
                                        "reject",
                                        ApiClient.json(
                                                "reason", reason.getText().toString().trim())))
                .show();
    }

    private void action(String action, JSONObject body) {
        if (processing) return;
        processing = true;
        binding.btnHostBookingApprove.setEnabled(false);
        binding.btnHostBookingReject.setEnabled(false);
        repository.action(
                bookingId(),
                action,
                body,
                row -> {
                    processing = false;
                    if (binding != null) load();
                },
                text -> {
                    processing = false;
                    if (binding != null) load();
                    error(text);
                });
    }

    private void error(String text) {
        if (binding == null) return;
        binding.tvHostBookingDetailError.setText(text);
        binding.tvHostBookingDetailError.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
