package com.example.roomly.ui.notification;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.roomly.R;
import com.example.roomly.data.model.AppNotification;
import com.example.roomly.data.repository.DemoNotificationRepository;
import com.example.roomly.databinding.FragmentNotificationsBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public final class NotificationsFragment extends Fragment {

    private static final String STATE_UNREAD_ONLY = "unread_only";

    private FragmentNotificationsBinding binding;
    private NotificationAdapter adapter;

    private boolean unreadOnly;
    private androidx.appcompat.app.AlertDialog notificationDialog;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentNotificationsBinding.inflate(
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

        if (savedInstanceState != null) {
            unreadOnly = savedInstanceState.getBoolean(
                    STATE_UNREAD_ONLY,
                    false
            );
        }

        adapter = new NotificationAdapter(this::openNotification);

        binding.rvNotifications.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );
        binding.rvNotifications.setAdapter(adapter);

        binding.tvNotificationsDescription.setText(
                "Thông báo mẫu để kiểm tra giao diện. "
                        + "Dữ liệu hiện tại chưa kết nối với backend."
        );

        binding.btnNotificationsBack.setOnClickListener(
                clickedView ->
                        getParentFragmentManager().popBackStack()
        );

        binding.btnNotificationsRetry.setOnClickListener(
                clickedView -> loadNotifications()
        );

        binding.btnNotificationsMarkAllRead.setOnClickListener(
                clickedView -> markAllAsRead()
        );

        binding.chipGroupNotifications.check(
                unreadOnly
                        ? R.id.chip_notifications_unread
                        : R.id.chip_notifications_all
        );

        binding.chipGroupNotifications.setOnCheckedStateChangeListener(
                (group, checkedIds) -> {
                    unreadOnly = checkedIds.contains(
                            R.id.chip_notifications_unread
                    );

                    loadNotifications();
                }
        );
    }

    @Override
    public void onViewStateRestored(
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewStateRestored(savedInstanceState);

        if (binding == null) {
            return;
        }

        unreadOnly = binding.chipGroupNotifications.getCheckedChipId()
                == R.id.chip_notifications_unread;

        loadNotifications();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(STATE_UNREAD_ONLY, unreadOnly);
        super.onSaveInstanceState(outState);
    }

    private void loadNotifications() {
        if (binding == null || adapter == null) {
            return;
        }

        binding.progressNotifications.setVisibility(View.VISIBLE);
        binding.layoutNotificationsError.setVisibility(View.GONE);
        binding.layoutNotificationsEmpty.setVisibility(View.GONE);
        binding.rvNotifications.setVisibility(View.GONE);

        try {
            DemoNotificationRepository repository =
                    DemoNotificationRepository.getInstance();

            List<AppNotification> notifications =
                    repository.getNotifications(unreadOnly);

            int unreadCount = repository.getUnreadCount();

            adapter.submitNotifications(notifications);

            binding.tvNotificationsUnreadCount.setText(
                    "Chưa đọc: " + unreadCount
            );

            binding.btnNotificationsMarkAllRead.setEnabled(
                    unreadCount > 0
            );

            if (notifications.isEmpty()) {
                binding.tvNotificationsEmptyTitle.setText(
                        unreadOnly
                                ? "Không có thông báo chưa đọc"
                                : "Chưa có thông báo"
                );

                binding.tvNotificationsEmptyDescription.setText(
                        unreadOnly
                                ? "Bạn đã đọc tất cả thông báo."
                                : "Các thông báo mới sẽ xuất hiện tại đây."
                );

                binding.layoutNotificationsEmpty.setVisibility(
                        View.VISIBLE
                );
            } else {
                binding.rvNotifications.setVisibility(View.VISIBLE);
            }
        } catch (RuntimeException exception) {
            showError(exception);
        } finally {
            if (binding != null) {
                binding.progressNotifications.setVisibility(View.GONE);
            }
        }
    }

    private void markAllAsRead() {
        if (binding == null) {
            return;
        }

        try {
            DemoNotificationRepository.getInstance().markAllAsRead();
            loadNotifications();
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void openNotification(AppNotification notification) {
        if (binding == null || !isAdded()) {
            return;
        }

        if (notificationDialog != null) {
            return;
        }

        try {
            AppNotification updated =
                    DemoNotificationRepository.getInstance()
                            .markAsRead(notification.getId());

            loadNotifications();

            String message = updated.getBody();

            if (!message.isEmpty()) {
                message += "\n\n";
            }

            message += updated.getTimeLabel();

            notificationDialog = new MaterialAlertDialogBuilder(
                    requireContext()
            )
                    .setTitle(updated.getTitle())
                    .setMessage(message)
                    .setPositiveButton("Đóng", null)
                    .create();

            notificationDialog.setOnDismissListener(
                    dialog -> notificationDialog = null
            );

            notificationDialog.show();
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void showError(RuntimeException exception) {
        if (binding == null) {
            return;
        }

        String message = exception.getMessage();

        if (message == null || message.trim().isEmpty()) {
            message = "Không thể tải thông báo. Vui lòng thử lại.";
        }

        binding.progressNotifications.setVisibility(View.GONE);
        binding.rvNotifications.setVisibility(View.GONE);
        binding.layoutNotificationsEmpty.setVisibility(View.GONE);
        binding.layoutNotificationsError.setVisibility(View.VISIBLE);

        binding.tvNotificationsError.setText(message);
        binding.btnNotificationsMarkAllRead.setEnabled(false);
    }

    @Override
    public void onDestroyView() {
        if (notificationDialog != null) {
            notificationDialog.setOnDismissListener(null);
            notificationDialog.dismiss();
            notificationDialog = null;
        }

        if (binding != null) {
            binding.btnNotificationsBack.setOnClickListener(null);
            binding.btnNotificationsRetry.setOnClickListener(null);
            binding.btnNotificationsMarkAllRead.setOnClickListener(null);

            binding.chipGroupNotifications
                    .setOnCheckedStateChangeListener(null);

            binding.rvNotifications.setAdapter(null);
        }

        adapter = null;
        binding = null;

        super.onDestroyView();
    }
}