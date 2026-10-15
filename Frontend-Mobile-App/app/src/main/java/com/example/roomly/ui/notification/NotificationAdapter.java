package com.example.roomly.ui.notification;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomly.R;
import com.example.roomly.data.model.AppNotification;
import com.example.roomly.databinding.ItemNotificationBinding;

import java.util.ArrayList;
import java.util.List;

public final class NotificationAdapter extends
        RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(AppNotification notification);
    }

    private final List<AppNotification> notifications = new ArrayList<>();
    private final OnNotificationClickListener listener;

    public NotificationAdapter(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    public void submitNotifications(List<AppNotification> items) {
        notifications.clear();

        if (items != null) {
            notifications.addAll(items);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemNotificationBinding binding = ItemNotificationBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );

        return new NotificationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull NotificationViewHolder holder,
            int position
    ) {
        holder.bind(notifications.get(position));
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    final class NotificationViewHolder extends RecyclerView.ViewHolder {

        private final ItemNotificationBinding binding;

        NotificationViewHolder(ItemNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.getRoot().setOnClickListener(view -> {
                int position = getBindingAdapterPosition();

                if (position == RecyclerView.NO_POSITION || listener == null) {
                    return;
                }

                listener.onNotificationClick(
                        notifications.get(position)
                );
            });
        }

        void bind(AppNotification notification) {
            binding.tvNotificationTitle.setText(
                    notification.getTitle()
            );

            binding.tvNotificationBody.setText(
                    notification.getBody()
            );

            binding.tvNotificationBody.setVisibility(
                    notification.getBody().isEmpty()
                            ? View.GONE
                            : View.VISIBLE
            );

            binding.tvNotificationTime.setText(
                    notification.getTimeLabel()
            );

            binding.tvNotificationReadStatus.setText(
                    notification.getReadStatusLabel()
            );

            // INVISIBLE giữ vị trí tiêu đề thẳng hàng giữa các thẻ.
            binding.indicatorNotificationUnread.setVisibility(
                    notification.isRead()
                            ? View.INVISIBLE
                            : View.VISIBLE
            );

            int statusColor = notification.isRead()
                    ? R.color.roomly_text_secondary
                    : R.color.roomly_primary;

            binding.tvNotificationReadStatus.setTextColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            statusColor
                    )
            );
        }
    }
}