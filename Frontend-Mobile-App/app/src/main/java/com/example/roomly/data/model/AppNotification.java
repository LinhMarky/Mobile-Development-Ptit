package com.example.roomly.data.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class AppNotification {

    public enum Type {
        BOOKING_STATUS,
        PAYMENT_STATUS,
        MESSAGE,
        VIEWING_STATUS,
        LISTING_STATUS,
        SYSTEM
    }

    private final String id;
    private final Type type;
    private final String title;
    private final String body;
    private final String targetType;
    private final String targetId;
    private final boolean read;
    private final long createdAtMillis;

    public AppNotification(
            String id,
            Type type,
            String title,
            String body,
            String targetType,
            String targetId,
            boolean read,
            long createdAtMillis
    ) {
        this.id = requireText(id, "Mã thông báo");
        this.title = requireText(title, "Tiêu đề thông báo");

        if (type == null) {
            throw new IllegalArgumentException(
                    "Loại thông báo không được để trống"
            );
        }

        if (createdAtMillis <= 0) {
            throw new IllegalArgumentException(
                    "Thời gian tạo thông báo không hợp lệ"
            );
        }

        this.type = type;
        this.body = normalize(body);
        this.targetType = normalize(targetType);
        this.targetId = normalize(targetId);
        this.read = read;
        this.createdAtMillis = createdAtMillis;
    }

    public String getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public boolean isRead() {
        return read;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public boolean hasTarget() {
        return !targetType.isEmpty() && !targetId.isEmpty();
    }

    public String getTimeLabel() {
        SimpleDateFormat formatter = new SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                new Locale("vi", "VN")
        );

        return formatter.format(new Date(createdAtMillis));
    }

    public String getReadStatusLabel() {
        return read ? "Đã đọc" : "Chưa đọc";
    }

    public AppNotification markAsRead() {
        if (read) {
            return this;
        }

        return new AppNotification(
                id,
                type,
                title,
                body,
                targetType,
                targetId,
                true,
                createdAtMillis
        );
    }

    private static String requireText(String value, String fieldName) {
        String normalized = normalize(value);

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    fieldName + " không được để trống"
            );
        }

        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}