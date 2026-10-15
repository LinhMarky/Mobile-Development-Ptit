package com.example.roomly.push;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.roomly.MainActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class RoomlyMessagingService extends FirebaseMessagingService {
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onNewToken(@NonNull String token) {
        PushManager.onNewToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        Map<String, String> data = new HashMap<>(message.getData());
        // Kiểm tra phiên và hiển thị cùng luồng với đăng xuất/chuyển tài khoản.
        mainHandler.post(() -> showNotification(data));
    }

    private void showNotification(Map<String, String> data) {
        String recipient = data.get("recipient_user_id");
        String installation = data.get("installation_id");
        if (!PushManager.isCurrentRecipient(recipient, installation)) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        String notificationId = data.get("notification_id");
        int requestCode = Objects.hash(recipient, notificationId);
        Intent intent =
                new Intent(this, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        .putExtra(PushManager.EXTRA_OPEN_INBOX, true)
                        .putExtra(PushManager.EXTRA_RECIPIENT, recipient)
                        .putExtra(PushManager.EXTRA_INSTALLATION, installation)
                        .putExtra("notification_id", notificationId)
                        .putExtra("ref_type", data.get("ref_type"))
                        .putExtra("ref_id", data.get("ref_id"));
        PendingIntent openInbox =
                PendingIntent.getActivity(
                        this,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String title = data.get("title");
        String body = data.get("body");
        NotificationCompat.Builder notification =
                new NotificationCompat.Builder(this, PushManager.CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(title == null || title.isEmpty() ? "ROOMLY" : title)
                        .setContentText(body == null ? "" : body)
                        .setStyle(
                                new NotificationCompat.BigTextStyle()
                                        .bigText(body == null ? "" : body))
                        .setContentIntent(openInbox)
                        .setAutoCancel(true);
        try {
            NotificationManagerCompat.from(this).notify(requestCode, notification.build());
        } catch (SecurityException ignored) {
            // Người dùng có thể vừa tắt quyền thông báo trong cài đặt.
        }
    }
}
