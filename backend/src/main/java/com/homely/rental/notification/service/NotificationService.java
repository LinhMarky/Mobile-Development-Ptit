package com.homely.rental.notification.service;

import com.homely.rental.auth.entity.User;
import com.homely.rental.auth.security.UserResolver;
import com.homely.rental.common.dto.PageResponse;
import com.homely.rental.common.exception.IdInvalidException;
import com.homely.rental.common.exception.ResourceNotFoundException;
import com.homely.rental.notification.dto.NotificationDTO;
import com.homely.rental.notification.entity.Notification;
import com.homely.rental.notification.entity.NotificationType;
import com.homely.rental.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Notification service (NOTIF01–NOTIF03).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserResolver userResolver;
    private final org.springframework.context.ApplicationEventPublisher events;

    // NOTIF01: Get my notifications
    @Transactional(readOnly = true)
    public PageResponse<NotificationDTO> getMyNotifications(Pageable pageable) throws IdInvalidException {
        User user = getCurrentUser();
        if (pageable.isUnpaged() || pageable.getPageSize() > 50) throw new IllegalArgumentException("Invalid page size");
        Page<Notification> page = notificationRepository.findByUserIdOrderByCreatedAtDescIdDesc(user.getId(),
                org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()));
        return PageResponse.of(page, page.getContent().stream().map(this::toDTO).toList());
    }

    // NOTIF02: Get unread count
    @Transactional(readOnly = true)
    public long getUnreadCount() throws IdInvalidException {
        User user = getCurrentUser();
        return notificationRepository.countByUserIdAndIsReadFalse(user.getId());
    }

    // NOTIF03: Mark all as read
    @Transactional
    public int markAllAsRead() throws IdInvalidException {
        User user = getCurrentUser();
        return notificationRepository.markAllAsRead(user.getId());
    }

    // Mark single as read
    @Transactional
    public NotificationDTO markAsRead(Long notificationId) throws IdInvalidException {
        User user = getCurrentUser();
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));

        notification.setRead(true);
        return toDTO(notificationRepository.save(notification));
    }

    // Internal: Create notification (called by other services / event listeners)
    @Transactional
    public Notification createNotification(User targetUser, NotificationType type,
                                            String title, String body,
                                            String refType, Long refId) {
        Notification notification = new Notification();
        notification.setUser(targetUser);
        notification.setType(type);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setRefType(refType);
        notification.setRefId(refId);

        Notification saved = notificationRepository.save(notification);
        log.info("Notification created: type={}, user={}, ref={}:{}", type, targetUser.getId(), refType, refId);
        events.publishEvent(new NotificationCreated(saved.getId()));
        return saved;
    }

    private NotificationDTO toDTO(Notification n) {
        return NotificationDTO.builder()
                .id(n.getId())
                .type(n.getType().name())
                .title(n.getTitle())
                .body(n.getBody())
                .refType(n.getRefType())
                .refId(n.getRefId())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }

    private User getCurrentUser() throws IdInvalidException {
        return userResolver.requireCurrent();
    }
}
