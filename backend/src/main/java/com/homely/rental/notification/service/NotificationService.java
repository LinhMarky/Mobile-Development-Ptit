package com.homely.rental.notification.service;

import com.homely.rental.auth.entity.User;
import com.homely.rental.auth.security.UserResolver;
import com.homely.rental.common.dto.PageResponse;
import com.homely.rental.common.exception.ResourceNotFoundException;
import com.homely.rental.notification.dto.NotificationDTO;
import com.homely.rental.notification.entity.Notification;
import com.homely.rental.notification.entity.NotificationType;
import com.homely.rental.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notifications;
    private final UserResolver userResolver;
    private final ApplicationEventPublisher events;

    @Transactional(readOnly = true)
    public PageResponse<NotificationDTO> getMyNotifications(Pageable pageable) {
        if (pageable.isUnpaged() || pageable.getPageSize() > 50)
            throw new IllegalArgumentException("Notification page size must be between 1 and 50");
        User user = userResolver.requireCurrent();
        var page = notifications.findByUserIdOrderByCreatedAtDescIdDesc(user.getId(), pageable);
        return PageResponse.of(page, page.getContent().stream().map(this::toDTO).toList());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount() {
        return notifications.countByUserIdAndIsReadFalse(userResolver.requireCurrent().getId());
    }

    @Transactional
    public int markAllAsRead() {
        return notifications.markAllAsRead(userResolver.requireCurrent().getId());
    }

    @Transactional
    public NotificationDTO markAsRead(Long id) {
        Long userId = userResolver.requireCurrent().getId();
        Notification notification = notifications.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        notification.setRead(true);
        return toDTO(notification);
    }

    /** Inbox and device deliveries participate in the caller's transaction. */
    @Transactional
    public Notification createNotification(User targetUser, NotificationType type,
                                           String title, String body, String refType, Long refId) {
        Notification notification = new Notification();
        notification.setUser(targetUser); notification.setType(type);
        notification.setTitle(title); notification.setBody(body);
        notification.setRefType(refType); notification.setRefId(refId);
        Notification saved = notifications.save(notification);
        events.publishEvent(new NotificationCreated(saved.getId()));
        return saved;
    }

    private NotificationDTO toDTO(Notification notification) {
        return NotificationDTO.builder().id(notification.getId()).type(notification.getType().name())
                .title(notification.getTitle()).body(notification.getBody()).refType(notification.getRefType())
                .refId(notification.getRefId()).isRead(notification.isRead()).createdAt(notification.getCreatedAt()).build();
    }
}
