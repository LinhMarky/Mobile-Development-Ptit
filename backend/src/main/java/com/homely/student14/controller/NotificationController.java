package com.homely.student14.controller;

import static com.homely.student14.dto.ApiResponse.ok;
import static com.homely.student14.repository.DatabaseRepository.page;

import com.homely.student14.dto.ApiResponse;
import com.homely.student14.service.NotificationService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping("/notifications")
    public ApiResponse<?> list(@AuthenticationPrincipal Jwt jwt) {
        return ok(page(notificationService.list(jwt)));
    }

    @PostMapping("/notifications/{id}/read")
    public ApiResponse<?> read(@PathVariable long id, @AuthenticationPrincipal Jwt jwt) {
        notificationService.read(id, jwt);
        return ok(null);
    }

    @PostMapping("/notifications/read-all")
    public ApiResponse<?> readAll(@AuthenticationPrincipal Jwt jwt) {
        notificationService.readAll(jwt);
        return ok(null);
    }
}
