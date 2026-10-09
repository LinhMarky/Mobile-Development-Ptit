package com.example.roomly.ui.booking;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.roomly.R;
import com.example.roomly.data.repository.DemoBookingRepository;
import com.example.roomly.databinding.FragmentBookingsBinding;
import com.example.roomly.data.model.Booking;

import java.util.List;

public final class BookingsFragment extends Fragment {

    private FragmentBookingsBinding binding;
    private BookingAdapter adapter;
    private boolean openingDetail;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentBookingsBinding.inflate(
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

        openingDetail = false;

        adapter = new BookingAdapter(this::openBookingDetail);

        binding.rvBookings.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        binding.rvBookings.setAdapter(adapter);

        binding.tvBookingsDescription.setText(
                "Dữ liệu demo để kiểm tra giao diện yêu cầu thuê. "
                        + "Các mẫu này chưa được gửi đến chủ trọ "
                        + "và chưa phát sinh thanh toán."
        );

        binding.btnBookingsBack.setOnClickListener(
                clickedView ->
                        getParentFragmentManager().popBackStack()
        );

        binding.btnBookingsRetry.setOnClickListener(
                clickedView -> loadBookings()
        );

        loadBookings();
    }

    private void loadBookings() {
        if (binding == null || adapter == null) {
            return;
        }

        showLoading();

        try {
            List<Booking> bookings =
                    DemoBookingRepository.getInstance()
                            .getBookings();

            adapter.submitBookings(bookings);

            binding.progressBookings.setVisibility(View.GONE);
            binding.layoutBookingsError.setVisibility(View.GONE);

            boolean empty = bookings.isEmpty();

            binding.layoutBookingsEmpty.setVisibility(
                    empty ? View.VISIBLE : View.GONE
            );

            binding.rvBookings.setVisibility(
                    empty ? View.GONE : View.VISIBLE
            );
        } catch (RuntimeException exception) {
            adapter.submitBookings(null);

            binding.progressBookings.setVisibility(View.GONE);
            binding.rvBookings.setVisibility(View.GONE);
            binding.layoutBookingsEmpty.setVisibility(View.GONE);
            binding.layoutBookingsError.setVisibility(View.VISIBLE);

            binding.tvBookingsError.setText(
                    "Không thể tải dữ liệu demo. Vui lòng thử lại."
            );
        }
    }

    private void showLoading() {
        binding.progressBookings.setVisibility(View.VISIBLE);
        binding.rvBookings.setVisibility(View.GONE);
        binding.layoutBookingsEmpty.setVisibility(View.GONE);
        binding.layoutBookingsError.setVisibility(View.GONE);
    }

    private void openBookingDetail(Booking booking) {
        if (binding == null
                || !isAdded()
                || openingDetail
                || getParentFragmentManager().isStateSaved()) {
            return;
        }

        openingDetail = true;

        getParentFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                        R.id.fragment_container,
                        BookingDetailFragment.newInstance(
                                booking.getId()
                        )
                )
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDestroyView() {
        if (binding != null) {
            binding.btnBookingsBack.setOnClickListener(null);
            binding.btnBookingsRetry.setOnClickListener(null);
            binding.rvBookings.setAdapter(null);
        }

        openingDetail = false;
        adapter = null;
        binding = null;

        super.onDestroyView();
    }
}