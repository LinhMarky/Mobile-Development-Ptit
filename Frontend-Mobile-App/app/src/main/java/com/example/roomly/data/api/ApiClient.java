package com.example.roomly.data.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.example.roomly.BuildConfig;
import com.example.roomly.data.repository.SessionRepository;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** HTTP runs on worker threads; callbacks always return to the screen thread. */
public final class ApiClient {
    private static final ExecutorService WORKERS = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static SharedPreferences preferences;
    private static Context application;
    private static volatile String accessToken = "";
    private static volatile long sessionVersion;
    private static final Object REFRESH_LOCK = new Object();
    private static final ThreadLocal<Long> REQUEST_SESSION = new ThreadLocal<>();

    private ApiClient() {}

    public static void initialize(Context context) {
        application = context.getApplicationContext();
        preferences =
                context.getApplicationContext()
                        .getSharedPreferences("roomly_session", Context.MODE_PRIVATE);
        accessToken = preferences.getString("access_token", "");
        if (!preferences.contains("installation_id")) {
            preferences.edit().putString("installation_id", UUID.randomUUID().toString()).apply();
        }
    }

    public static String installationId() {
        return preferences.getString("installation_id", "");
    }

    public static JSONObject cachedUser() {
        try {
            return new JSONObject(preferences.getString("user", "{}"));
        } catch (JSONException exception) {
            return new JSONObject();
        }
    }

    public static synchronized void saveSession(JSONObject response) {
        accessToken = response.optString("access_token");
        preferences
                .edit()
                .putString("access_token", accessToken)
                .putString("refresh_token", response.optString("refresh_token"))
                .putString("user", response.optJSONObject("user").toString())
                .apply();
        sessionVersion++;
    }

    public static synchronized void clearSession() {
        accessToken = "";
        sessionVersion++;
        if (preferences != null)
            preferences
                    .edit()
                    .remove("access_token")
                    .remove("refresh_token")
                    .remove("user")
                    .apply();
    }

    public static void updateUser(JSONObject user) throws JSONException {
        preferences.edit().putString("user", user.toString()).apply();
        SessionRepository.getInstance().updateAuthenticatedSession(ApiMapper.session(user));
    }

    public static String refreshToken() {
        return preferences.getString("refresh_token", "");
    }

    public static JSONObject imageBody(String uri) throws IOException {
        try (InputStream input =
                application.getContentResolver().openInputStream(android.net.Uri.parse(uri))) {
            byte[] bytes = readBytes(input, 4 * 1024 * 1024);
            return json(
                    "content_base64",
                    android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP));
        }
    }

    public static byte[] imageBytes(String path) throws IOException, JSONException {
        checkRequestSession();
        String token = accessToken;
        try {
            return downloadImage(path, token);
        } catch (ApiException exception) {
            if (exception.getStatus() != 401 || token.isEmpty()) throw exception;
            refresh(token);
            checkRequestSession();
            return downloadImage(path, accessToken);
        }
    }

    private static byte[] downloadImage(String path, String token) throws IOException {
        java.net.URI base = java.net.URI.create(BuildConfig.API_BASE_URL);
        java.net.URI target = base.resolve(path);
        if (!java.util.Objects.equals(base.getHost(), target.getHost())
                || base.getPort() != target.getPort()
                || !base.getScheme().equals(target.getScheme()))
            throw new IOException("Địa chỉ ảnh không hợp lệ.");
        HttpURLConnection connection = (HttpURLConnection) target.toURL().openConnection();
        try {
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            if (!token.isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + token);
            int status = connection.getResponseCode();
            if (status >= 400) throw new ApiException(status, "Không tải được ảnh.");
            try (InputStream input = connection.getInputStream()) {
                return readBytes(input, 8 * 1024 * 1024);
            }
        } finally {
            connection.disconnect();
        }
    }

    private static byte[] readBytes(InputStream input, int limit) throws IOException {
        if (input == null) throw new IOException("Không đọc được ảnh.");
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (output.size() + count > limit)
                    throw new IOException("Ảnh quá lớn. Chọn ảnh JPG/PNG tối đa 4 MB.");
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }

    public static JSONObject json(Object... pairs) {
        JSONObject body = new JSONObject();
        try {
            for (int index = 0; index < pairs.length; index += 2) {
                body.put(pairs[index].toString(), pairs[index + 1]);
            }
            return body;
        } catch (JSONException exception) {
            throw new IllegalArgumentException("Dữ liệu gửi đi không hợp lệ.", exception);
        }
    }

    public static <T> void execute(
            Callable<T> request, Consumer<T> success, Consumer<String> failure) {
        long version = sessionVersion;
        WORKERS.execute(
                () -> {
                    if (version != sessionVersion) return;
                    REQUEST_SESSION.set(version);
                    try {
                        T result = request.call();
                        MAIN.post(
                                () -> {
                                    if (version == sessionVersion) success.accept(result);
                                });
                    } catch (Exception exception) {
                        String message =
                                exception instanceof IOException
                                        ? "Không kết nối được máy chủ. Kiểm tra mạng và thử lại."
                                        : exception.getMessage();
                        MAIN.post(
                                () -> {
                                    if (version == sessionVersion)
                                        failure.accept(
                                                message == null
                                                        ? "Không thể thực hiện yêu cầu."
                                                        : message);
                                });
                    } finally {
                        REQUEST_SESSION.remove();
                    }
                });
    }

    public static Object request(String method, String path, JSONObject body)
            throws IOException, JSONException {
        checkRequestSession();
        String token = accessToken;
        try {
            return send(method, path, body, token);
        } catch (ApiException exception) {
            if (exception.getStatus() != 401
                    || path.equals("auth/login")
                    || path.equals("auth/register")
                    || path.equals("auth/refresh")
                    || token.isEmpty()) throw exception;
            refresh(token);
            checkRequestSession();
            return send(method, path, body, accessToken);
        }
    }

    private static void checkRequestSession() {
        Long expected = REQUEST_SESSION.get();
        if (expected != null && expected != sessionVersion)
            throw new ApiException(401, "Phiên đăng nhập đã thay đổi.");
    }

    private static void refresh(String rejectedToken) throws IOException, JSONException {
        synchronized (REFRESH_LOCK) {
            checkRequestSession();
            if (!accessToken.equals(rejectedToken)) return;
            long version = sessionVersion;
            try {
                JSONObject response =
                        (JSONObject)
                                send(
                                        "POST",
                                        "auth/refresh",
                                        json("refresh_token", refreshToken()),
                                        "");
                synchronized (ApiClient.class) {
                    checkRequestSession();
                    if (!accessToken.equals(rejectedToken)) return;
                    accessToken = response.getString("access_token");
                    preferences
                            .edit()
                            .putString("access_token", accessToken)
                            .putString("refresh_token", response.getString("refresh_token"))
                            .apply();
                }
            } catch (ApiException exception) {
                if (exception.getStatus() == 400
                        || exception.getStatus() == 401
                        || exception.getStatus() == 403) {
                    MAIN.post(
                            () -> {
                                if (version == sessionVersion)
                                    SessionRepository.getInstance().clearLocalSession();
                            });
                }
                throw exception;
            }
        }
    }

    private static Object send(String method, String path, JSONObject body, String token)
            throws IOException, JSONException {
        HttpURLConnection connection =
                (HttpURLConnection) new URL(BuildConfig.API_BASE_URL + path).openConnection();
        try {
            connection.setRequestMethod(method);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("Accept", "application/json");
            if (!token.isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + token);
            if (body != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                try (var output = connection.getOutputStream()) {
                    output.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }
            }
            int status = connection.getResponseCode();
            InputStream stream =
                    status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String content = read(stream);
            JSONObject envelope;
            try {
                envelope = new JSONObject(content);
            } catch (JSONException exception) {
                throw new ApiException(status, "Máy chủ trả dữ liệu không hợp lệ.");
            }
            if (status >= 400) {
                JSONObject error = envelope.optJSONObject("data");
                throw new ApiException(
                        status,
                        error == null
                                ? envelope.optString("message")
                                : error.optString("detail", envelope.optString("message")));
            }
            return envelope.get("data");
        } finally {
            connection.disconnect();
        }
    }

    private static String read(InputStream stream) throws IOException {
        if (stream == null) return "";
        try (InputStream input = stream;
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }
}
