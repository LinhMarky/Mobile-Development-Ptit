package com.homely.rental.interaction.service;

import com.homely.rental.auth.entity.User;
import com.homely.rental.auth.security.UserResolver;
import com.homely.rental.common.dto.PageResponse;
import com.homely.rental.common.exception.IdInvalidException;
import com.homely.rental.interaction.dto.ReportCreateRequest;
import com.homely.rental.interaction.dto.ReportDTO;
import com.homely.rental.interaction.entity.Report;
import com.homely.rental.interaction.entity.ReportStatus;
import com.homely.rental.interaction.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Report service (RPT01–RPT02, BR-14).
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserResolver userResolver;

    private static final Set<String> VALID_TARGET_TYPES = Set.of("listing", "review", "message", "user");
    private static final Set<String> VALID_REASON_CODES = Set.of("SPAM", "SCAM", "INAPPROPRIATE", "DUPLICATE", "OTHER");

    // RPT01: Create report (Auth+Verified)
    @Transactional
    public ReportDTO createReport(ReportCreateRequest dto) throws IdInvalidException {
        User reporter = getVerifiedUser();

        if (!VALID_TARGET_TYPES.contains(dto.getTargetType())) {
            throw new IdInvalidException("Invalid target_type. Allowed: " + VALID_TARGET_TYPES);
        }
        if (!VALID_REASON_CODES.contains(dto.getReasonCode())) {
            throw new IdInvalidException("Invalid reason_code. Allowed: " + VALID_REASON_CODES);
        }

        Report report = new Report();
        report.setReporter(reporter);
        report.setTargetType(dto.getTargetType());
        report.setTargetId(dto.getTargetId());
        report.setReasonCode(dto.getReasonCode());
        report.setDescription(dto.getDescription());
        report.setStatus(ReportStatus.PENDING);

        return toDTO(reportRepository.save(report));
    }

    // RPT02: My reports (Auth)
    @Transactional(readOnly = true)
    public PageResponse<ReportDTO> getMyReports(Pageable pageable) throws IdInvalidException {
        User user = userResolver.requireCurrent();
        Page<Report> page = reportRepository.findByReporterIdOrderByCreatedAtDesc(user.getId(), pageable);
        return PageResponse.of(page, page.getContent().stream().map(this::toDTO).toList());
    }

    private ReportDTO toDTO(Report r) {
        return ReportDTO.builder()
                .id(r.getId())
                .reporterId(r.getReporter().getId())
                .targetType(r.getTargetType())
                .targetId(r.getTargetId())
                .reasonCode(r.getReasonCode())
                .description(r.getDescription())
                .status(r.getStatus().name())
                .createdAt(r.getCreatedAt())
                .build();
    }


    private User getVerifiedUser() throws IdInvalidException {
        User user = userResolver.requireCurrent();
        if (!user.isEmailVerified()) {
            throw new IdInvalidException("Email must be verified to submit a report");
        }
        return user;
    }
}
