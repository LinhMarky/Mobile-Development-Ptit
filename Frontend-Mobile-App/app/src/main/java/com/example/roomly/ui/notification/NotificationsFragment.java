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
import com.example.roomly.data.api.ApiClient;
import com.example.roomly.data.api.ApiMapper;
import com.example.roomly.data.model.AppNotification;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentNotificationsBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
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
            @Nullable Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            unreadOnly = savedInstanceState.getBoolean(STATE_UNREAD_ONLY, false);
        }

        adapter = new NotificationAdapter(this::openNotification);

        binding.rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvNotifications.setAdapter(adapter);

        binding.tvNotificationsDescription.setText(
                "Cập nhật về tin đăng, yêu cầu thuê và tiền cọc.");

        binding.btnNotificationsBack.setOnClickListener(
                clickedView -> getParentFragmentManager().popBackStack());

        binding.btnNotificationsRetry.setOnClickListener(clickedView -> loadNotifications());

        binding.btnNotificationsMarkAllRead.setOnClickListener(clickedView -> markAllAsRead());

        binding.chipGroupNotifications.check(
                unreadOnly ? R.id.chip_notifications_unread : R.id.chip_notifications_all);

        binding.chipGroupNotifications.setOnCheckedStateChangeListener(
                (group, checkedIds) -> {
                    unreadOnly = checkedIds.contains(R.id.chip_notifications_unread);

                    loadNotifications();
                });
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (binding == null) {
            return;
        }

        unreadOnly =
                binding.chipGroupNotifications.getCheckedChipId() == R.id.chip_notifications_unread;

        loadNotifications();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(STATE_UNREAD_ONLY, unreadOnly);
        super.onSaveInstanceState(outState);
    }

    private void loadNotifications() {
        if (binding == null || adapter == null || !checkNotificationAccess()) return;
        binding.progressNotifications.setVisibility(View.VISIBLE);
        binding.layoutNotificationsError.setVisibility(View.GONE);
        ApiClient.execute(
                () -> {
                    JSONArray rows =
                            ((JSONObject) ApiClient.request("GET", "notifications", null))
                                    .getJSONArray("data");
                    List<AppNotification> result = new ArrayList<>();
                    for (int index = 0; index < rows.length(); index++) {
                        JSONObject row = rows.getJSONObject(index);
                        AppNotification.Type type;
                        try {
                            type = AppNotification.Type.valueOf(row.optString("notification_type"));
                        } catch (IllegalArgumentException exception) {
                            type = AppNotification.Type.SYSTEM;
                        }
                        result.add(
                                new AppNotification(
                                        row.getString("id"),
                                        type,
                                        row.getString("title"),
                                        row.optString("body"),
                                        row.optString("ref_type"),
                                        row.optString("ref_id"),
                                        row.optBoolean("is_read"),
                                        ApiMapper.timestamp(row.getString("created_at"))));
                    }
                    return result;
                },
                notifications -> {
                    if (binding == null || adapter == null) return;
                    List<AppNotification> filtered = new ArrayList<>();
                    int unread = 0;
                    for (AppNotification item : notifications) {
                        if (!item.isRead()) unread++;
                        if (!unreadOnly || !item.isRead()) filtered.add(item);
                    }
                    adapter.submitNotifications(filtered);
                    binding.tvNotificationsUnreadCount.setText("Chưa đọc: " + unread);
                    binding.btnNotificationsMarkAllRead.setEnabled(unread > 0);
                    binding.progressNotifications.setVisibility(View.GONE);
                    binding.rvNotifications.setVisibility(
                            filtered.isEmpty() ? View.GONE : View.VISIBLE);
                    binding.layoutNotificationsEmpty.setVisibility(
                            filtered.isEmpty() ? View.VISIBLE : View.GONE);
                },
                text -> showError(new IllegalStateException(text)));
    }

    private void markAllAsRead() {
        if (binding == null || !checkNotificationAccess()) return;
        binding.btnNotificationsMarkAllRead.setEnabled(false);
        ApiClient.execute(
                () -> ApiClient.request("POST", "notifications/read-all", new JSONObject()),
                row -> loadNotifications(),
                text -> showError(new IllegalStateException(text)));
    }

    private void openNotification(AppNotification notification) {
        if (binding == null || !checkNotificationAccess()) return;
        ApiClient.execute(
                () ->
                        ApiClient.request(
                                "POST",
                                "notifications/" + notification.getId() + "/read",
                                new JSONObject()),
                row -> {
                    if (binding == null) return;
                    loadNotifications();
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle(notification.getTitle())
                            .setMessage(
                                    notification.getBody() + "\n\n" + notification.getTimeLabel())
                            .setPositiveButton("Đóng", null)
                            .show();
                },
                text -> showError(new IllegalStateException(text)));
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

    private boolean checkNotificationAccess() {
        if (binding == null) {
            return false;
        }

        boolean loggedIn = SessionRepository.getInstance().getCurrentSession().isLoggedIn();

        if (loggedIn) {
            return true;
        }

        if (notificationDialog != null) {
            notificationDialog.setOnDismissListener(null);
            notificationDialog.dismiss();
            notificationDialog = null;
        }

        if (adapter != null) {
            adapter.submitNotifications(java.util.Collections.emptyList());
        }

        binding.tvNotificationsUnreadCount.setText("Chưa đọc: 0");

        showError(new IllegalStateException("Bạn cần đăng nhập để xem thông báo."));

        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadNotifications();
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

            binding.chipGroupNotifications.setOnCheckedStateChangeListener(null);

            binding.rvNotifications.setAdapter(null);
        }

        adapter = null;
        binding = null;

        super.onDestroyView();
    }
}
