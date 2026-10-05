package com.homely.rental.interaction.controller;

import com.homely.rental.common.annotation.ApiMessage;
import com.homely.rental.common.dto.PageResponse;
import com.homely.rental.common.exception.IdInvalidException;
import com.homely.rental.interaction.dto.ReportCreateRequest;
import com.homely.rental.interaction.dto.ReportDTO;
import com.homely.rental.interaction.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Report controller (RPT01–RPT02).
 */
@RestController
@RequestMapping(path = "${apiPrefix}/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // RPT01: Create report
    @PostMapping
    @ApiMessage("Create report")
    public ResponseEntity<ReportDTO> createReport(
            @Valid @RequestBody ReportCreateRequest request) throws IdInvalidException {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.createReport(request));
    }

    // RPT02: My reports
    @GetMapping("/my")
    @ApiMessage("Get my reports")
    public ResponseEntity<PageResponse<ReportDTO>> getMyReports(Pageable pageable) throws IdInvalidException {
        return ResponseEntity.ok(reportService.getMyReports(pageable));
    }
}
