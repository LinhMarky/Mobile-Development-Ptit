package com.homely.rental.notification.controller;

import com.homely.rental.common.annotation.ApiMessage;
import com.homely.rental.common.dto.PageResponse;
import com.homely.rental.common.exception.IdInvalidException;
import com.homely.rental.notification.dto.NotificationDTO;
import com.homely.rental.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Notification controller (NOTIF01–NOTIF03).
 */
@RestController
@RequestMapping(path = "${apiPrefix}/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // NOTIF01: Get my notifications
    @GetMapping
    @ApiMessage("Get my notifications")
    public ResponseEntity<PageResponse<NotificationDTO>> getMyNotifications(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) throws IdInvalidException {
        return ResponseEntity.ok(notificationService.getMyNotifications(PageRequest.of(page, size)));
    }

    // NOTIF02: Get unread count
    @GetMapping("/unread-count")
    @ApiMessage("Get unread notification count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() throws IdInvalidException {
        long count = notificationService.getUnreadCount();
        return ResponseEntity.ok(Map.of("unread_count", count));
    }

    // NOTIF03: Mark all as read
    @PostMapping({"/read-all", "/mark-all-read"})
    @ApiMessage("Mark all notifications as read")
    public ResponseEntity<Map<String, Integer>> markAllAsRead() throws IdInvalidException {
        int updated = notificationService.markAllAsRead();
        return ResponseEntity.ok(Map.of("updated", updated));
    }

    // Mark single as read
    @PostMapping("/{id}/read")
    @ApiMessage("Mark notification as read")
    public ResponseEntity<NotificationDTO> markAsRead(@PathVariable @Positive Long id) throws IdInvalidException {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }
}
