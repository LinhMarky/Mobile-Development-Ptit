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
import com.example.roomly.data.api.ApiMapper;
import com.example.roomly.data.model.AppMode;
import com.example.roomly.data.model.Booking;
import com.example.roomly.data.repository.BookingRepository;
import com.example.roomly.databinding.FragmentBookingHandoverBinding;

import org.json.JSONObject;

public final class BookingHandoverFragment extends Fragment {
    private FragmentBookingHandoverBinding binding;
    private final BookingRepository repository = new BookingRepository();
    private JSONObject current;
    private boolean processing;

    public static BookingHandoverFragment newInstance(String bookingId, AppMode actorMode) {
        BookingHandoverFragment fragment = new BookingHandoverFragment();
        Bundle arguments = new Bundle();
        arguments.putString("booking_id", bookingId);
        arguments.putString("actor_mode", actorMode.name());
        fragment.setArguments(arguments);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentBookingHandoverBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.btnHandoverBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.btnHandoverConfirm.setOnClickListener(v -> confirm());
        binding.checkHandoverAcknowledgement.setOnCheckedChangeListener(
                (button, checked) -> updateButton());
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

    private boolean host() {
        return "HOST".equals(requireArguments().getString("actor_mode"));
    }

    private void render(Booking booking) {
        binding.layoutHandoverContent.setVisibility(View.VISIBLE);
        binding.tvHandoverError.setVisibility(View.GONE);
        binding.tvHandoverBookingCode.setText("Yêu cầu #" + booking.getId());
        binding.tvHandoverRoomTitle.setText(booking.getRoomTitle());
        binding.tvHandoverRoomAddress.setText(booking.getRoomAddress());
        binding.tvHandoverMoveIn.setText(booking.getDesiredMoveInLabel());
        binding.tvHandoverStatus.setText(booking.getStatusLabel());
        binding.tvHandoverDescription.setText(
                "Chỉ xác nhận sau khi đã bàn giao phòng thực tế. Cần xác nhận của cả chủ trọ và"
                        + " người thuê để hoàn tất.");
        binding.tvHandoverHostConfirmation.setText(
                current.isNull("host_handover_confirmed_at")
                        ? "Chủ trọ: chưa xác nhận"
                        : "Chủ trọ: đã xác nhận");
        binding.tvHandoverTenantConfirmation.setText(
                current.isNull("tenant_handover_confirmed_at")
                        ? "Người thuê: chưa xác nhận"
                        : "Người thuê: đã xác nhận");
        binding.btnHandoverConfirm.setText("Xác nhận bàn giao");
        updateButton();
    }

    private void updateButton() {
        if (binding == null) return;
        String field = host() ? "host_handover_confirmed_at" : "tenant_handover_confirmed_at";
        boolean canConfirm =
                current != null
                        && "CONFIRMED".equals(current.optString("status"))
                        && current.isNull(field);
        binding.checkHandoverAcknowledgement.setVisibility(canConfirm ? View.VISIBLE : View.GONE);
        binding.btnHandoverConfirm.setVisibility(canConfirm ? View.VISIBLE : View.GONE);
        binding.btnHandoverConfirm.setEnabled(
                !processing
                        && current != null
                        && "CONFIRMED".equals(current.optString("status"))
                        && current.isNull(field)
                        && binding.checkHandoverAcknowledgement.isChecked());
    }

    private void confirm() {
        if (processing || !binding.checkHandoverAcknowledgement.isChecked()) return;
        processing = true;
        updateButton();
        repository.action(
                bookingId(),
                host() ? "handover-host" : "handover-tenant",
                null,
                row -> {
                    processing = false;
                    if (binding != null) {
                        message("Đã ghi nhận xác nhận bàn giao.");
                        load();
                    }
                },
                text -> {
                    processing = false;
                    updateButton();
                    error(text);
                });
    }

    private void error(String text) {
        if (binding == null) return;
        binding.tvHandoverError.setText(text);
        binding.tvHandoverError.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
