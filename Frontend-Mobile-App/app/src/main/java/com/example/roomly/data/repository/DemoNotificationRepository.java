package com.example.roomly.data.repository;

import com.example.roomly.data.model.AppNotification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DemoNotificationRepository {

    private static final DemoNotificationRepository INSTANCE =
            new DemoNotificationRepository();

    private final List<AppNotification> notifications = new ArrayList<>();

    private DemoNotificationRepository() {
        createDemoNotifications();
    }

    public static DemoNotificationRepository getInstance() {
        return INSTANCE;
    }

    public synchronized List<AppNotification> getNotifications(
            boolean unreadOnly
    ) {
        List<AppNotification> result = new ArrayList<>();

        for (AppNotification notification : notifications) {
            if (!unreadOnly || !notification.isRead()) {
                result.add(notification);
            }
        }

        return Collections.unmodifiableList(result);
    }

    public synchronized int getUnreadCount() {
        int count = 0;

        for (AppNotification notification : notifications) {
            if (!notification.isRead()) {
                count++;
            }
        }

        return count;
    }

    public synchronized AppNotification getNotificationById(String id) {
        String normalizedId = normalize(id);

        for (AppNotification notification : notifications) {
            if (notification.getId().equals(normalizedId)) {
                return notification;
            }
        }

        return null;
    }

    public synchronized AppNotification markAsRead(String id) {
        String normalizedId = normalize(id);

        for (int index = 0; index < notifications.size(); index++) {
            AppNotification notification = notifications.get(index);

            if (notification.getId().equals(normalizedId)) {
                AppNotification updated = notification.markAsRead();
                notifications.set(index, updated);
                return updated;
            }
        }

        throw new IllegalArgumentException(
                "Không tìm thấy thông báo"
        );
    }

    public synchronized void markAllAsRead() {
        for (int index = 0; index < notifications.size(); index++) {
            notifications.set(
                    index,
                    notifications.get(index).markAsRead()
            );
        }
    }

    private void createDemoNotifications() {
        long now = System.currentTimeMillis();
        long minute = 60_000L;
        long hour = 60 * minute;

        // Thông báo mới nhất nằm đầu danh sách.
        notifications.add(new AppNotification(
                "DEMO-NOTIFICATION-001",
                AppNotification.Type.SYSTEM,
                "Chào mừng bạn đến với ROOMLY",
                "Đây là thông báo mẫu để kiểm tra giao diện. "
                        + "Bạn có thể lọc thông báo chưa đọc "
                        + "và đánh dấu tất cả là đã đọc.",
                "",
                "",
                false,
                now - 5 * minute
        ));

        notifications.add(new AppNotification(
                "DEMO-NOTIFICATION-002",
                AppNotification.Type.BOOKING_STATUS,
                "Yêu cầu thuê mẫu đã được duyệt",
                "Thông báo demo: yêu cầu thuê phòng "
                        + "đã chuyển sang trạng thái được duyệt.",
                "",
                "",
                false,
                now - hour
        ));

        notifications.add(new AppNotification(
                "DEMO-NOTIFICATION-003",
                AppNotification.Type.PAYMENT_STATUS,
                "Thanh toán tiền cọc mẫu thành công",
                "Thông báo demo: hệ thống đã ghi nhận "
                        + "kết quả thanh toán tiền cọc thành công.",
                "",
                "",
                false,
                now - 3 * hour
        ));

        notifications.add(new AppNotification(
                "DEMO-NOTIFICATION-004",
                AppNotification.Type.SYSTEM,
                "Tìm phòng phù hợp với bạn",
                "Bạn có thể xem thông tin phòng, lưu phòng yêu thích "
                        + "và gửi yêu cầu thuê từ trang chi tiết phòng.",
                "",
                "",
                true,
                now - 24 * hour
        ));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}