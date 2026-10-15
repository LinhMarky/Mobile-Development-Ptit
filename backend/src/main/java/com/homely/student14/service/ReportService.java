package com.homely.student14.service;

import com.homely.student14.dto.request.report.CreateReportDTO;
import com.homely.student14.dto.request.report.ResolveReportDTO;
import com.homely.student14.entity.Booking;
import com.homely.student14.entity.Payment;
import com.homely.student14.repository.DatabaseRepository;
import com.homely.student14.repository.ReportRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReportService {
    private static final Set<String> BOOKING_REASON_CODES =
            Set.of("DEPOSIT_DISPUTE", "HANDOVER_OVERDUE", "NO_SHOW", "OTHER");
    private static final Set<String> SETTLEMENT_DECISIONS =
            Set.of("REFUND_TENANT", "FORFEIT_DEPOSIT", "SPLIT", "NO_ACTION");

    private final ReportRepository reportRepository;
    private final AuthService authService;
    private final BookingService bookingService;

    public List<Map<String, Object>> listBookingReports(long bookingId, Jwt jwt) {
        bookingService.get(bookingId, jwt);
        return reportRepository.findByBookingId(bookingId);
    }

    public List<Map<String, Object>> listMyReports(Jwt jwt) {
        Map<String, Object> currentUser = authService.user(jwt);
        long reporterId = DatabaseRepository.id(currentUser.get("id"));
        return reportRepository.findContentReportsByReporterId(reporterId);
    }

    public List<Map<String, Object>> listAdminReports(Jwt jwt, boolean bookingCases) {
        authService.role(jwt, "admin");
        if (bookingCases) {
            return reportRepository.findAllBookingCases();
        }
        return reportRepository.findAllContentReports();
    }

    @Transactional
    public Map<String, Object> create(Jwt jwt, CreateReportDTO request) {
        Map<String, Object> currentUser = authService.user(jwt);
        if (!DatabaseRepository.flag(currentUser.get("email_verified"))) {
            throw DatabaseRepository.fail(403, "Cần xác minh email để gửi báo cáo.");
        }

        String targetType = readText(request.getTargetType(), "target_type", 50, true);
        if (!"listing".equals(targetType) && !"booking".equals(targetType)) {
            throw DatabaseRepository.fail(400, "Chọn tin đăng hoặc yêu cầu thuê cần báo cáo.");
        }

        long reporterId = DatabaseRepository.id(currentUser.get("id"));
        long targetId = DatabaseRepository.id(request.getTargetId());
        String reasonCode = readText(request.getReasonCode(), "reason_code", 50, true);
        boolean bookingCase = "booking".equals(targetType);
        String description = readText(request.getDescription(), "description", 3000, bookingCase);
        if (bookingCase) {
            checkBookingCase(targetId, reporterId, reasonCode, description, jwt);
        } else {
            checkListingReport(targetId, reporterId);
        }

        long reportId =
                reportRepository.create(reporterId, targetType, targetId, reasonCode, description);
        return reportRepository.findDetailsById(reportId);
    }

    @Transactional
    public Map<String, Object> resolve(long reportId, Jwt jwt, ResolveReportDTO request) {
        Map<String, Object> currentAdmin = authService.role(jwt, "admin");
        long adminId = DatabaseRepository.id(currentAdmin.get("id"));
        Map<String, Object> initialReport = reportRepository.findById(reportId);

        // Giữ cùng thứ tự khóa với thanh toán/bàn giao: phòng → yêu cầu thuê → báo cáo.
        Booking booking = null;
        if (initialReport.get("booking_id") != null) {
            long bookingId = DatabaseRepository.id(initialReport.get("booking_id"));
            booking = reportRepository.findBookingForUpdate(bookingId);
        }
        Map<String, Object> currentReport = reportRepository.findByIdForUpdate(reportId);
        String reportStatus = Objects.toString(currentReport.get("status"), "");
        if (!"OPEN".equals(reportStatus) && !"IN_REVIEW".equals(reportStatus)) {
            throw DatabaseRepository.fail(409, "Báo cáo đã được xử lý.");
        }

        String resolutionNote =
                readText(request.getResolutionNote(), "resolution_note", 3000, true);
        if (booking == null) {
            String status = "CLOSED".equals(request.getStatus()) ? "CLOSED" : "RESOLVED";
            reportRepository.resolveContentReport(reportId, status, resolutionNote, adminId);
        } else {
            resolveBookingCase(reportId, booking, request, resolutionNote, adminId);
        }

        if (currentReport.get("reporter_id") != null) {
            long reporterId = DatabaseRepository.id(currentReport.get("reporter_id"));
            reportRepository.notifyReporter(reporterId, reportId, resolutionNote);
        }
        return reportRepository.findDetailsById(reportId);
    }

    private void checkListingReport(long listingId, long reporterId) {
        long hostId = reportRepository.findPublishedListingHostId(listingId);
        if (hostId == reporterId) {
            throw DatabaseRepository.fail(400, "Không báo cáo tin của chính mình.");
        }
    }

    private void checkBookingCase(
            long bookingId, long reporterId, String reasonCode, String description, Jwt jwt) {
        if (description.codePointCount(0, description.length()) < 10) {
            throw DatabaseRepository.fail(400, "Mô tả khiếu nại cần ít nhất 10 ký tự.");
        }

        Map<String, Object> bookingDetails = bookingService.get(bookingId, jwt);
        Booking booking = reportRepository.toBooking(bookingDetails);
        boolean participant =
                booking.getTenantId() == reporterId || booking.getHostId() == reporterId;
        if (!participant) {
            throw DatabaseRepository.fail(403, "Bạn không thuộc yêu cầu thuê này.");
        }
        if (!"CONFIRMED".equals(booking.getStatus()) && !"COMPLETED".equals(booking.getStatus())) {
            throw DatabaseRepository.fail(409, "Chỉ mở khiếu nại sau khi xác nhận thuê.");
        }
        if (!BOOKING_REASON_CODES.contains(reasonCode)) {
            throw DatabaseRepository.fail(400, "Loại khiếu nại không hợp lệ.");
        }
    }

    private void resolveBookingCase(
            long reportId,
            Booking booking,
            ResolveReportDTO request,
            String resolutionNote,
            long adminId) {
        String decision = readText(request.getDecision(), "decision", 30, true);
        if (!SETTLEMENT_DECISIONS.contains(decision)) {
            throw DatabaseRepository.fail(400, "Quyết định không hợp lệ.");
        }

        BigDecimal tenantRefund = null;
        BigDecimal hostRetained = null;
        if (!"NO_ACTION".equals(decision)) {
            Payment payment = checkDepositForSettlement(booking);
            BigDecimal deposit = booking.getDepositVnd();
            tenantRefund = calculateTenantRefund(decision, deposit, request.getTenantRefundVnd());
            hostRetained = deposit.subtract(tenantRefund);

            if (tenantRefund.signum() > 0) {
                reportRepository.refundPayment(payment.getId(), tenantRefund);
            }
            if ("CONFIRMED".equals(booking.getStatus())) {
                reportRepository.cancelBookingAfterSettlement(booking.getId(), resolutionNote);
                reportRepository.releaseRoom(booking.getRoomId());
            }
        }

        reportRepository.resolveBookingCase(
                reportId, decision, tenantRefund, hostRetained, resolutionNote, adminId);
    }

    private Payment checkDepositForSettlement(Booking booking) {
        if (reportRepository.hasFinancialDecision(booking.getId())) {
            throw DatabaseRepository.fail(409, "Tiền cọc của yêu cầu này đã có quyết định xử lý.");
        }
        if (booking.getAllocatedPaymentId() == null) {
            throw DatabaseRepository.fail(409, "Yêu cầu chưa có tiền cọc để xử lý.");
        }

        Payment payment = reportRepository.findPaymentForUpdate(booking.getAllocatedPaymentId());
        if (!payment.isTest()
                || !"MOCK_SANDBOX".equals(payment.getProvider())
                || !"SUCCEEDED".equals(payment.getStatus())
                || payment.getRefundStatus() != null) {
            throw DatabaseRepository.fail(409, "Chỉ xử lý cọc sandbox thành công, chưa được hoàn.");
        }
        if (payment.getAmountVnd().compareTo(booking.getDepositVnd()) != 0
                || !payment.getBookingId().equals(booking.getId())) {
            throw DatabaseRepository.fail(409, "Giao dịch không khớp yêu cầu thuê.");
        }
        return payment;
    }

    private BigDecimal calculateTenantRefund(
            String decision, BigDecimal deposit, BigDecimal requestedRefund) {
        if ("REFUND_TENANT".equals(decision)) {
            return deposit;
        }
        if ("FORFEIT_DEPOSIT".equals(decision)) {
            return BigDecimal.ZERO;
        }
        if (requestedRefund == null) {
            throw DatabaseRepository.fail(
                    400, "Nhập tiền hoàn lớn hơn 0 và nhỏ hơn tiền cọc khi chia cọc.");
        }

        BigDecimal tenantRefund;
        try {
            tenantRefund = requestedRefund.setScale(0, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw DatabaseRepository.fail(400, "Tiền phải là số nguyên VND.");
        }
        if (tenantRefund.signum() < 0 || tenantRefund.compareTo(deposit) > 0) {
            throw DatabaseRepository.fail(400, "Số tiền hoàn không được vượt tiền cọc.");
        }
        BigDecimal hostRetained = deposit.subtract(tenantRefund);
        if (tenantRefund.signum() == 0 || hostRetained.signum() == 0) {
            throw DatabaseRepository.fail(
                    400, "Nhập tiền hoàn lớn hơn 0 và nhỏ hơn tiền cọc khi chia cọc.");
        }
        return tenantRefund;
    }

    private String readText(String value, String fieldName, int maxLength, boolean required) {
        String normalized = Objects.toString(value, "").trim();
        if ((required && normalized.isEmpty())
                || normalized.codePointCount(0, normalized.length()) > maxLength) {
            throw DatabaseRepository.fail(
                    400, "Trường " + fieldName + " không hợp lệ (tối đa " + maxLength + " ký tự).");
        }
        return normalized;
    }
}
