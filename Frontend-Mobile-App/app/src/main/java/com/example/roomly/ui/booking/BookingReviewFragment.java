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
import com.example.roomly.databinding.FragmentBookingReviewBinding;
import com.example.roomly.ui.NavigationHelper;

import org.json.JSONObject;

public final class BookingReviewFragment extends Fragment {
    private FragmentBookingReviewBinding binding;
    private final BookingRepository repository = new BookingRepository();
    private JSONObject current;
    private boolean processing;

    public static BookingReviewFragment newInstance(String bookingId) {
        BookingReviewFragment fragment = new BookingReviewFragment();
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
        binding = FragmentBookingReviewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.btnReviewBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.btnReviewSubmit.setText("Gửi đánh giá");
        binding.btnReviewSubmit.setOnClickListener(v -> submit());
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
        binding.layoutReviewContent.setVisibility(View.VISIBLE);
        binding.tvReviewError.setVisibility(View.GONE);
        binding.tvReviewBookingCode.setText("Yêu cầu #" + booking.getId());
        binding.tvReviewRoomTitle.setText(booking.getRoomTitle());
        binding.tvReviewRoomAddress.setText(booking.getRoomAddress());
        binding.tvReviewSavedInfo.setVisibility(View.VISIBLE);
        binding.tvReviewSavedInfo.setText("Mỗi yêu cầu hoàn tất được gửi một đánh giá.");
        binding.btnReviewSubmit.setEnabled(
                !processing && booking.getStatus() == Booking.Status.COMPLETED);
        loadExistingReview(booking);
    }

    private void loadExistingReview(Booking booking) {
        ApiClient.execute(
                () ->
                        (JSONObject)
                                ApiClient.request(
                                        "GET", "rooms/" + booking.getRoomId() + "/reviews", null),
                page -> {
                    if (binding == null) return;
                    org.json.JSONArray rows = page.optJSONArray("data");
                    for (int index = 0; rows != null && index < rows.length(); index++) {
                        JSONObject row = rows.optJSONObject(index);
                        if (row == null || !bookingId().equals(row.optString("booking_id")))
                            continue;
                        binding.ratingReview.setRating(row.optInt("rating"));
                        binding.edtReviewComment.setText(
                                row.isNull("comment") ? "" : row.optString("comment"));
                        binding.ratingReview.setEnabled(false);
                        binding.edtReviewComment.setEnabled(false);
                        binding.btnReviewSubmit.setEnabled(false);
                        binding.tvReviewSavedInfo.setText(
                                "Bạn đã gửi đánh giá cho yêu cầu thuê này.");
                        break;
                    }
                },
                this::error);
    }

    private void submit() {
        if (processing) return;
        int rating = Math.round(binding.ratingReview.getRating());
        if (rating < 1 || rating > 5) {
            error("Vui lòng chọn từ 1 đến 5 sao.");
            return;
        }
        String comment = binding.edtReviewComment.getText().toString().trim();
        processing = true;
        binding.btnReviewSubmit.setEnabled(false);
        ApiClient.execute(
                () ->
                        ApiClient.request(
                                "POST",
                                "reviews",
                                ApiClient.json(
                                        "booking_id",
                                        bookingId(),
                                        "rating",
                                        rating,
                                        "comment",
                                        comment)),
                row -> {
                    if (binding == null) return;
                    NavigationHelper.whenResumed(
                            this,
                            () -> {
                                processing = false;
                                message("Đã gửi đánh giá.");
                                getParentFragmentManager().popBackStack();
                            });
                },
                text -> {
                    processing = false;
                    if (binding != null) binding.btnReviewSubmit.setEnabled(true);
                    error(text);
                });
    }

    private void error(String text) {
        if (binding == null) return;
        binding.tvReviewError.setText(text);
        binding.tvReviewError.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
