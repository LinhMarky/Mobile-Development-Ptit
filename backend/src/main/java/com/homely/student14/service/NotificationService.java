package com.homely.student14.service;

import com.homely.student14.repository.DatabaseRepository;
import com.homely.student14.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final AuthService authService;

    public List<Map<String, Object>> list(Jwt jwt) {
        long userId = DatabaseRepository.id(authService.user(jwt).get("id"));
        return notificationRepository.findByUserId(userId);
    }

    @Transactional
    public void read(long notificationId, Jwt jwt) {
        long userId = DatabaseRepository.id(authService.user(jwt).get("id"));
        if (!notificationRepository.markAsRead(notificationId, userId)) {
            throw DatabaseRepository.fail(404, "Không tìm thấy thông báo.");
        }
    }

    @Transactional
    public void readAll(Jwt jwt) {
        long userId = DatabaseRepository.id(authService.user(jwt).get("id"));
        notificationRepository.markAllAsRead(userId);
    }
}
