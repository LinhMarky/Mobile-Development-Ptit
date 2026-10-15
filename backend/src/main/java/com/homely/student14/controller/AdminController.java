package com.homely.student14.controller;

import static com.homely.student14.dto.ApiResponse.ok;
import static com.homely.student14.repository.DatabaseRepository.page;

import com.homely.student14.dto.ApiResponse;
import com.homely.student14.dto.request.admin.UpdateAccountStatusDTO;
import com.homely.student14.dto.request.catalog.ListingActionDTO;
import com.homely.student14.service.AdminService;
import com.homely.student14.service.CatalogService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;
    private final CatalogService catalogService;

    @GetMapping("/admin/summary")
    public ApiResponse<?> summary(@AuthenticationPrincipal Jwt jwt) {
        return ok(adminService.summary(jwt));
    }

    @GetMapping("/admin/listings")
    public ApiResponse<?> listings(@AuthenticationPrincipal Jwt jwt) {
        return ok(page(adminService.listings(jwt)));
    }

    @PostMapping("/admin/listings/{id}/{action:approve|reject|suspend}")
    public ApiResponse<?> listingAction(
            @PathVariable long id,
            @PathVariable String action,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ListingActionDTO request) {
        return ok(catalogService.listingAction(id, jwt, action, request));
    }

    @GetMapping("/admin/users")
    public ApiResponse<?> users(@AuthenticationPrincipal Jwt jwt) {
        return ok(page(adminService.users(jwt)));
    }

    @PostMapping("/admin/users/{id}/{action:suspend|activate}")
    public ApiResponse<?> updateUser(
            @PathVariable long id,
            @PathVariable String action,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateAccountStatusDTO request) {
        adminService.updateUser(id, jwt, action, request);
        return ok(null);
    }
}
