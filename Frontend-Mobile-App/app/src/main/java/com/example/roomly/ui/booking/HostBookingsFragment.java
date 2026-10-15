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
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;
import com.example.roomly.data.repository.BookingRepository;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentHostBookingsBinding;

import java.util.ArrayList;
import java.util.List;

public final class HostBookingsFragment extends Fragment {

    private static final String STATE_FILTER = "host_booking_filter";

    private FragmentHostBookingsBinding binding;
    private BookingAdapter adapter;

    private String selectedFilter = "ALL";
    private boolean openingDetail;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            selectedFilter = savedInstanceState.getString(STATE_FILTER, "ALL");
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentHostBookingsBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        openingDetail = false;

        adapter = new BookingAdapter(this::openBookingDetail);

        binding.rvHostBookings.setLayoutManager(new LinearLayoutManager(requireContext()));

        binding.rvHostBookings.setAdapter(adapter);

        binding.tvHostBookingsDescription.setText("Quản lý yêu cầu thuê các phòng của bạn.");

        binding.btnHostBookingsBack.setOnClickListener(
                clickedView -> getParentFragmentManager().popBackStack());

        binding.btnHostBookingsRetry.setOnClickListener(clickedView -> loadBookings());

        restoreSelectedChip();

        binding.chipGroupHostBookings.setOnCheckedStateChangeListener(
                (group, checkedIds) -> {
                    if (checkedIds.isEmpty()) {
                        return;
                    }

                    int checkedId = checkedIds.get(0);

                    if (checkedId == R.id.chip_host_bookings_pending) {
                        selectedFilter = "PENDING";
                    } else if (checkedId == R.id.chip_host_bookings_approved) {
                        selectedFilter = "APPROVED";
                    } else if (checkedId == R.id.chip_host_bookings_confirmed) {
                        selectedFilter = "CONFIRMED";
                    } else {
                        selectedFilter = "ALL";
                    }

                    loadBookings();
                });

        loadBookings();
    }

    @Override
    public void onResume() {
        super.onResume();

        openingDetail = false;
        loadBookings();
    }

    private void restoreSelectedChip() {
        int chipId;

        switch (selectedFilter) {
            case "PENDING":
                chipId = R.id.chip_host_bookings_pending;
                break;

            case "APPROVED":
                chipId = R.id.chip_host_bookings_approved;
                break;

            case "CONFIRMED":
                chipId = R.id.chip_host_bookings_confirmed;
                break;

            default:
                selectedFilter = "ALL";
                chipId = R.id.chip_host_bookings_all;
                break;
        }

        binding.chipGroupHostBookings.check(chipId);
    }

    private boolean hasHostAccess() {
        SessionState session = SessionRepository.getInstance().getCurrentSession();

        return session.isLoggedIn() && session.hasRole(UserRole.HOST);
    }

    private void loadBookings() {
        if (binding == null || adapter == null) return;
        if (!hasHostAccess()) {
            showError("Vui lòng đăng nhập tài khoản chủ trọ.");
            return;
        }
        showLoading();
        new BookingRepository()
                .fetchBookings(
                        true,
                        bookings -> {
                            if (binding == null || adapter == null) return;
                            List<Booking> filtered = new ArrayList<>();
                            for (Booking booking : bookings) {
                                if (matchesFilter(booking)) filtered.add(booking);
                            }
                            adapter.submitBookings(filtered);
                            binding.progressHostBookings.setVisibility(View.GONE);
                            binding.layoutHostBookingsError.setVisibility(View.GONE);
                            binding.rvHostBookings.setVisibility(
                                    filtered.isEmpty() ? View.GONE : View.VISIBLE);
                            binding.layoutHostBookingsEmpty.setVisibility(
                                    filtered.isEmpty() ? View.VISIBLE : View.GONE);
                            binding.tvHostBookingsEmptyTitle.setText("Chưa có yêu cầu phù hợp");
                            binding.tvHostBookingsEmptyDescription.setText(
                                    "Các yêu cầu thuê sẽ xuất hiện tại đây.");
                        },
                        message -> {
                            if (binding != null) showError(message);
                        });
    }

    private boolean matchesFilter(Booking booking) {
        switch (selectedFilter) {
            case "PENDING":
                return booking.getStatus() == Booking.Status.PENDING;

            case "APPROVED":
                return booking.getStatus() == Booking.Status.APPROVED;

            case "CONFIRMED":
                return booking.getStatus() == Booking.Status.CONFIRMED;

            default:
                return true;
        }
    }

    private void showLoading() {
        binding.progressHostBookings.setVisibility(View.VISIBLE);
        binding.rvHostBookings.setVisibility(View.GONE);
        binding.layoutHostBookingsEmpty.setVisibility(View.GONE);
        binding.layoutHostBookingsError.setVisibility(View.GONE);
    }

    private void showError(String message) {
        binding.progressHostBookings.setVisibility(View.GONE);
        binding.rvHostBookings.setVisibility(View.GONE);
        binding.layoutHostBookingsEmpty.setVisibility(View.GONE);
        binding.layoutHostBookingsError.setVisibility(View.VISIBLE);
        binding.tvHostBookingsError.setText(message);
    }

    private void openBookingDetail(Booking booking) {
        if (binding == null
                || !isAdded()
                || openingDetail
                || getParentFragmentManager().isStateSaved()) {
            return;
        }

        if (!hasHostAccess()) {
            loadBookings();
            return;
        }

        openingDetail = true;

        getParentFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                        R.id.fragment_container,
                        HostBookingDetailFragment.newInstance(booking.getId()))
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_FILTER, selectedFilter);
    }

    @Override
    public void onDestroyView() {
        if (binding != null) {
            binding.btnHostBookingsBack.setOnClickListener(null);
            binding.btnHostBookingsRetry.setOnClickListener(null);
            binding.chipGroupHostBookings.setOnCheckedStateChangeListener(null);
            binding.rvHostBookings.setAdapter(null);
        }

        openingDetail = false;
        adapter = null;
        binding = null;

        super.onDestroyView();
    }
}
