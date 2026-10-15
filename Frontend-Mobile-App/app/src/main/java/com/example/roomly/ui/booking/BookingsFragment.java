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
import com.example.roomly.data.model.Booking;
import com.example.roomly.data.repository.BookingRepository;
import com.example.roomly.databinding.FragmentBookingsBinding;

public final class BookingsFragment extends Fragment {

    private FragmentBookingsBinding binding;
    private BookingAdapter adapter;
    private boolean openingDetail;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentBookingsBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        openingDetail = false;

        adapter = new BookingAdapter(this::openBookingDetail);

        binding.rvBookings.setLayoutManager(new LinearLayoutManager(requireContext()));

        binding.rvBookings.setAdapter(adapter);

        binding.tvBookingsDescription.setText(
                "Theo dõi yêu cầu thuê và trạng thái bàn giao của bạn.");

        binding.btnBookingsBack.setOnClickListener(
                clickedView -> getParentFragmentManager().popBackStack());

        binding.btnBookingsRetry.setOnClickListener(clickedView -> loadBookings());
    }

    private void loadBookings() {
        if (binding == null || adapter == null) return;
        openingDetail = false;
        showLoading();
        new BookingRepository()
                .fetchBookings(
                        false,
                        bookings -> {
                            if (binding == null || adapter == null) return;
                            adapter.submitBookings(bookings);
                            binding.progressBookings.setVisibility(View.GONE);
                            binding.layoutBookingsError.setVisibility(View.GONE);
                            binding.layoutBookingsEmpty.setVisibility(
                                    bookings.isEmpty() ? View.VISIBLE : View.GONE);
                            binding.rvBookings.setVisibility(
                                    bookings.isEmpty() ? View.GONE : View.VISIBLE);
                        },
                        message -> {
                            if (binding == null) return;
                            binding.progressBookings.setVisibility(View.GONE);
                            binding.rvBookings.setVisibility(View.GONE);
                            binding.layoutBookingsEmpty.setVisibility(View.GONE);
                            binding.layoutBookingsError.setVisibility(View.VISIBLE);
                            binding.tvBookingsError.setText(message);
                        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadBookings();
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

        getParentFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                        R.id.fragment_container, BookingDetailFragment.newInstance(booking.getId()))
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
