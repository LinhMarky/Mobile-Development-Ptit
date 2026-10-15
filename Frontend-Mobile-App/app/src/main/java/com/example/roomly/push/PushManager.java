package com.example.roomly.push;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.roomly.data.api.ApiClient;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.repository.SessionRepository;
import com.google.firebase.messaging.FirebaseMessaging;

/** Đăng ký token cho tài khoản đang đăng nhập; lỗi push không chặn các chức năng khác. */
public final class PushManager {
    public static final String CHANNEL_ID = "roomly_updates";
    public static final String EXTRA_OPEN_INBOX = "roomly_open_inbox";
    public static final String EXTRA_RECIPIENT = "recipient_user_id";
    public static final String EXTRA_INSTALLATION = "installation_id";

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static SharedPreferences preferences;
    private static Context application;
    private static String activeUserId = "";
    private static String registeredToken = "";
    private static long registrationVersion;
    private static boolean fetchingToken;
    private static boolean registering;

    private PushManager() {}

    public static void initialize(Context context) {
        if (application != null) return;
        application = context.getApplicationContext();
        preferences = application.getSharedPreferences("roomly_session", Context.MODE_PRIVATE);
        createChannel();
        SessionRepository.getInstance()
                .getSessionState()
                .observeForever(PushManager::onSessionChanged);
    }

    private static void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID, "Cập nhật ROOMLY", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Tin nhắn, lịch xem phòng và thay đổi yêu cầu thuê.");
            application
                    .getSystemService(NotificationManager.class)
                    .createNotificationChannel(channel);
        }
    }

    private static void onSessionChanged(SessionState session) {
        String userId =
                session.isLoggedIn() && session.getUserId() != null ? session.getUserId() : "";
        if (!userId.equals(activeUserId)) {
            activeUserId = userId;
            registrationVersion++;
            registering = false;
            registeredToken = "";
        }

        if (!userId.equals(preferences.getString("fcm_registered_user_id", ""))) {
            preferences
                    .edit()
                    .remove("fcm_registered_user_id")
                    .remove("fcm_registered_installation_id")
                    .apply();
            application.getSystemService(NotificationManager.class).cancelAll();
        }

        if (!userId.isEmpty()) {
            fetchToken(0);
        }
    }

    public static void requestPermission(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || activeUserId.isEmpty()) {
            return;
        }
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED
                || preferences.getBoolean("fcm_permission_asked", false)) {
            return;
        }

        preferences.edit().putBoolean("fcm_permission_asked", true).apply();
        ActivityCompat.requestPermissions(
                activity, new String[] {Manifest.permission.POST_NOTIFICATIONS}, 5041);
    }

    private static void fetchToken(int attempt) {
        if (fetchingToken || activeUserId.isEmpty()) {
            return;
        }
        fetchingToken = true;
        String requestedUser = activeUserId;
        FirebaseMessaging.getInstance()
                .getToken()
                .addOnCompleteListener(
                        task -> {
                            fetchingToken = false;
                            if (task.isSuccessful() && task.getResult() != null) {
                                preferences.edit().putString("fcm_token", task.getResult()).apply();
                                registerToken(0);
                            } else if (!activeUserId.isEmpty()
                                    && !requestedUser.equals(activeUserId)) {
                                // Tài khoản mới chưa thể lấy token khi yêu cầu của tài khoản cũ còn
                                // chạy.
                                fetchToken(0);
                            } else if (attempt < 2 && requestedUser.equals(activeUserId)) {
                                MAIN.postDelayed(
                                        () -> {
                                            if (requestedUser.equals(activeUserId)) {
                                                fetchToken(attempt + 1);
                                            }
                                        },
                                        10000L * (attempt + 1));
                            }
                        });
    }

    public static void onNewToken(String token) {
        MAIN.post(
                () -> {
                    preferences.edit().putString("fcm_token", token).apply();
                    registeredToken = "";
                    registerToken(0);
                });
    }

    private static void registerToken(int attempt) {
        String token = preferences.getString("fcm_token", "");
        if (activeUserId.isEmpty()
                || token.isEmpty()
                || registering
                || token.equals(registeredToken)) {
            return;
        }

        registering = true;
        String userId = activeUserId;
        String installationId = ApiClient.installationId();
        long version = registrationVersion;
        ApiClient.execute(
                () ->
                        ApiClient.request(
                                "PUT",
                                "profile/device-token",
                                ApiClient.json(
                                        "token", token,
                                        "installation_id", installationId,
                                        "device_name", "Roomly Android")),
                response -> {
                    if (version != registrationVersion || !userId.equals(activeUserId)) {
                        return;
                    }
                    registering = false;
                    registeredToken = token;
                    preferences
                            .edit()
                            .putString("fcm_registered_user_id", userId)
                            .putString("fcm_registered_installation_id", installationId)
                            .apply();
                    if (!token.equals(preferences.getString("fcm_token", ""))) {
                        registerToken(0);
                    }
                },
                error -> {
                    if (version != registrationVersion || !userId.equals(activeUserId)) {
                        return;
                    }
                    registering = false;
                    if (attempt < 2) {
                        MAIN.postDelayed(
                                () -> {
                                    if (version == registrationVersion) {
                                        registerToken(attempt + 1);
                                    }
                                },
                                10000L * (attempt + 1));
                    }
                });
    }

    public static boolean isCurrentRecipient(String userId, String installationId) {
        SessionState session = SessionRepository.getInstance().getCurrentSession();
        return session.isLoggedIn()
                && userId != null
                && !userId.isEmpty()
                && userId.equals(session.getUserId())
                && installationId != null
                && installationId.equals(ApiClient.installationId())
                && userId.equals(preferences.getString("fcm_registered_user_id", ""))
                && installationId.equals(
                        preferences.getString("fcm_registered_installation_id", ""));
    }
}
