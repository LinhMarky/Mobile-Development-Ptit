package com.homely.student14.repository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class NotificationRepository {
    private final DatabaseRepository databaseRepository;

    public List<Map<String, Object>> findByUserId(long userId) {
        return databaseRepository.all(
                "SELECT * FROM notifications WHERE user_id = ? ORDER BY id DESC", userId);
    }

    public boolean markAsRead(long notificationId, long userId) {
        return databaseRepository.update(
                        """
                        UPDATE notifications SET is_read = TRUE WHERE id = ? AND user_id = ?
                        """,
                        notificationId,
                        userId)
                > 0;
    }

    public void markAllAsRead(long userId) {
        databaseRepository.update(
                "UPDATE notifications SET is_read = TRUE WHERE user_id = ?", userId);
    }
}
