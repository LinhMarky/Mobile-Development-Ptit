package com.example.roomly.data.repository;

import com.example.roomly.data.api.ApiClient;
import com.example.roomly.data.api.ApiMapper;

import org.json.JSONObject;

import java.util.function.Consumer;

public final class AuthRepository {
    public void register(
            String name,
            String email,
            String password,
            Runnable success,
            Consumer<String> failure) {
        ApiClient.execute(
                () ->
                        ApiClient.request(
                                "POST",
                                "auth/register",
                                ApiClient.json(
                                        "full_name", name, "email", email, "password", password)),
                row -> success.run(),
                failure);
    }

    public void requestCode(
            String email, boolean reset, Runnable success, Consumer<String> failure) {
        ApiClient.execute(
                () ->
                        (JSONObject)
                                ApiClient.request(
                                        "POST",
                                        reset ? "auth/recovery-code" : "auth/verification-code",
                                        ApiClient.json("email", email)),
                response -> {
                    if (response.has("demo_code")) {
                        failure.accept("Máy chủ chưa bật gửi email thật. Vui lòng thử lại sau.");
                        return;
                    }
                    success.run();
                },
                failure);
    }

    public void verifyEmail(String email, String code, Runnable success, Consumer<String> failure) {
        ApiClient.execute(
                () ->
                        (JSONObject)
                                ApiClient.request(
                                        "POST",
                                        "auth/verify-email",
                                        ApiClient.json("email", email, "code", code)),
                row -> {
                    try {
                        if (SessionRepository.getInstance().getCurrentSession().isLoggedIn()
                                && ApiClient.cachedUser()
                                        .optString("id")
                                        .equals(row.optString("id"))) ApiClient.updateUser(row);
                        success.run();
                    } catch (Exception exception) {
                        failure.accept("Không cập nhật được phiên. Hãy đăng nhập lại.");
                    }
                },
                failure);
    }

    public void resetPassword(
            String email,
            String code,
            String password,
            Runnable success,
            Consumer<String> failure) {
        ApiClient.execute(
                () ->
                        ApiClient.request(
                                "POST",
                                "auth/reset-password",
                                ApiClient.json(
                                        "email", email, "code", code, "new_password", password)),
                row -> success.run(),
                failure);
    }

    public void updateProfile(
            String name, String phone, Runnable success, Consumer<String> failure) {
        ApiClient.execute(
                () ->
                        (JSONObject)
                                ApiClient.request(
                                        "PUT",
                                        "profile",
                                        ApiClient.json("full_name", name, "phone", phone)),
                row -> {
                    try {
                        ApiClient.updateUser(row);
                        success.run();
                    } catch (Exception exception) {
                        failure.accept("Không cập nhật được hồ sơ trên thiết bị.");
                    }
                },
                failure);
    }

    public void enableHost(Runnable success, Consumer<String> failure) {
        ApiClient.execute(
                () -> (JSONObject) ApiClient.request("POST", "auth/enable-host", new JSONObject()),
                row -> {
                    try {
                        ApiClient.updateUser(row);
                        success.run();
                    } catch (Exception exception) {
                        failure.accept("Hãy đăng nhập lại để cập nhật quyền.");
                    }
                },
                failure);
    }

    public void login(String email, String password, Runnable success, Consumer<String> failure) {
        ApiClient.execute(
                () ->
                        (JSONObject)
                                ApiClient.request(
                                        "POST",
                                        "auth/login",
                                        ApiClient.json(
                                                "email",
                                                email,
                                                "password",
                                                password,
                                                "installation_id",
                                                ApiClient.installationId(),
                                                "device_name",
                                                "Roomly Android")),
                response -> {
                    try {
                        var session = ApiMapper.session(response.getJSONObject("user"));
                        ApiClient.saveSession(response);
                        SessionRepository.getInstance().updateAuthenticatedSession(session);
                        success.run();
                    } catch (Exception exception) {
                        failure.accept("Thông tin đăng nhập từ máy chủ không hợp lệ.");
                    }
                },
                failure);
    }

    public void logout(Runnable finished) {
        ApiClient.execute(
                () ->
                        ApiClient.request(
                                "POST",
                                "auth/logout",
                                ApiClient.json("refresh_token", ApiClient.refreshToken())),
                response -> {
                    SessionRepository.getInstance().clearLocalSession();
                    finished.run();
                },
                error -> {
                    SessionRepository.getInstance().clearLocalSession();
                    finished.run();
                });
    }
}
