package com.example.roomly.ui.booking;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomly.R;
import com.example.roomly.databinding.ItemBookingBinding;
import com.example.roomly.data.model.Booking;

import java.util.ArrayList;
import java.util.List;

public final class BookingAdapter
        extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    public interface OnBookingClickListener {
        void onBookingClick(Booking booking);
    }

    private final List<Booking> bookings = new ArrayList<>();
    private final OnBookingClickListener listener;

    public BookingAdapter(OnBookingClickListener listener) {
        this.listener = listener;
    }

    public void submitBookings(List<Booking> newBookings) {
        List<Booking> copy = new ArrayList<>();

        if (newBookings != null) {
            for (Booking booking : newBookings) {
                if (booking != null) {
                    copy.add(booking);
                }
            }
        }

        bookings.clear();
        bookings.addAll(copy);

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemBookingBinding binding = ItemBookingBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );

        return new BookingViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull BookingViewHolder holder,
            int position
    ) {
        holder.bind(bookings.get(position));
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    @Override
    public void onViewRecycled(@NonNull BookingViewHolder holder) {
        holder.binding.btnBookingViewDetail.setOnClickListener(null);
        super.onViewRecycled(holder);
    }

    final class BookingViewHolder extends RecyclerView.ViewHolder {

        private final ItemBookingBinding binding;

        BookingViewHolder(ItemBookingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Booking booking) {
            String code = booking.getId();

            if (booking.isDemo()) {
                code += " • Dữ liệu demo";
            }

            binding.tvBookingCode.setText(code);

            binding.tvBookingStatus.setText(
                    booking.getStatusLabel()
            );

            binding.tvBookingStatus.setTextColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            getStatusColorResource(booking.getStatus())
                    )
            );

            binding.tvBookingRoomTitle.setText(
                    booking.getRoomTitle()
            );

            String address = booking.getRoomAddress();

            binding.tvBookingRoomAddress.setText(
                    address.isEmpty()
                            ? "Chưa cung cấp địa chỉ"
                            : address
            );

            binding.tvBookingMonthlyRent.setText(
                    "Giá thuê: " + booking.getMonthlyRentLabel()
            );

            binding.tvBookingDepositAmount.setText(
                    "Tiền cọc: " + booking.getDepositLabel()
            );

            binding.tvBookingMoveInDate.setText(
                    "Ngày dự kiến vào ở: "
                            + booking.getDesiredMoveInLabel()
            );

            binding.tvBookingOccupantCount.setText(
                    "Số người ở: " + booking.getOccupantCount()
            );

            String description = booking.getStatusDescription();

            if (booking.isDemo()) {
                description += "\nTrạng thái minh họa, "
                        + "chưa phát sinh giao dịch thực tế.";
            }

            binding.tvBookingStatusDescription.setText(
                    description
            );

            binding.btnBookingViewDetail.setEnabled(
                    listener != null
            );

            binding.btnBookingViewDetail.setOnClickListener(
                    view -> {
                        int position = getBindingAdapterPosition();

                        if (listener == null
                                || position == RecyclerView.NO_POSITION) {
                            return;
                        }

                        listener.onBookingClick(
                                bookings.get(position)
                        );
                    }
            );
        }
    }

    private static int getStatusColorResource(Booking.Status status) {
        switch (status) {
            case PENDING:
            case APPROVED:
            case CONFIRMED:
            case COMPLETED:
                return R.color.roomly_primary;

            case CANCELLED:
            case REJECTED:
            case EXPIRED:
            default:
                return R.color.roomly_text_secondary;
        }
    }
}