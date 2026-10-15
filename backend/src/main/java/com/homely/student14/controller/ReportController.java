package com.homely.student14.controller;

import com.homely.student14.dto.ApiResponse;
import com.homely.student14.dto.request.report.CreateReportDTO;
import com.homely.student14.dto.request.report.ResolveReportDTO;
import com.homely.student14.repository.DatabaseRepository;
import com.homely.student14.service.ReportService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    @GetMapping("/bookings/{id}/cases")
    public ApiResponse<?> bookingReports(@PathVariable long id, @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(DatabaseRepository.page(reportService.listBookingReports(id, jwt)));
    }

    @PostMapping("/reports")
    public ApiResponse<?> create(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateReportDTO request) {
        return ApiResponse.ok(reportService.create(jwt, request));
    }

    @GetMapping("/reports/my")
    public ApiResponse<?> myReports(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(DatabaseRepository.page(reportService.listMyReports(jwt)));
    }

    @GetMapping("/admin/reports")
    public ApiResponse<?> adminReports(
            @AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "false") boolean cases) {
        return ApiResponse.ok(DatabaseRepository.page(reportService.listAdminReports(jwt, cases)));
    }

    @PostMapping("/admin/reports/{id}/resolve")
    public ApiResponse<?> resolve(
            @PathVariable long id,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ResolveReportDTO request) {
        return ApiResponse.ok(reportService.resolve(id, jwt, request));
    }
}
