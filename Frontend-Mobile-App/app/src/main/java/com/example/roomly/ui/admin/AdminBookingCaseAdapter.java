package com.example.roomly.ui.admin;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomly.R;
import com.example.roomly.data.model.BookingCase;
import com.example.roomly.databinding.ItemAdminBookingCaseBinding;

import java.util.ArrayList;
import java.util.List;

public final class AdminBookingCaseAdapter extends
        RecyclerView.Adapter<AdminBookingCaseAdapter.CaseViewHolder> {

    public interface OnCaseClickListener {
        void onCaseClick(BookingCase bookingCase);
    }

    private final List<BookingCase> cases = new ArrayList<>();
    private final OnCaseClickListener listener;

    public AdminBookingCaseAdapter(OnCaseClickListener listener) {
        this.listener = listener;
    }

    public void submitCases(List<BookingCase> items) {
        cases.clear();

        if (items != null) {
            cases.addAll(items);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CaseViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemAdminBookingCaseBinding binding =
                ItemAdminBookingCaseBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                );

        return new CaseViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull CaseViewHolder holder,
            int position
    ) {
        holder.bind(cases.get(position));
    }

    @Override
    public int getItemCount() {
        return cases.size();
    }

    final class CaseViewHolder extends RecyclerView.ViewHolder {

        private final ItemAdminBookingCaseBinding binding;

        CaseViewHolder(ItemAdminBookingCaseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.getRoot().setOnClickListener(view -> {
                int position = getBindingAdapterPosition();

                if (position == RecyclerView.NO_POSITION
                        || listener == null) {
                    return;
                }

                listener.onCaseClick(cases.get(position));
            });
        }

        void bind(BookingCase bookingCase) {
            binding.tvAdminCaseStatus.setText(
                    bookingCase.getStatusLabel()
            );

            int statusColor = bookingCase.isOpen()
                    ? R.color.roomly_primary
                    : R.color.roomly_text_secondary;

            binding.tvAdminCaseStatus.setTextColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            statusColor
                    )
            );

            binding.tvAdminCaseType.setText(
                    bookingCase.getTypeLabel()
            );

            binding.tvAdminCaseBooking.setText(
                    "Mã yêu cầu thuê: " + bookingCase.getBookingId()
            );

            binding.tvAdminCaseDescription.setText(
                    bookingCase.getDescription()
            );

            binding.tvAdminCaseTime.setText(
                    "Đã gửi: " + bookingCase.getCreatedAtLabel()
            );
        }
    }
}