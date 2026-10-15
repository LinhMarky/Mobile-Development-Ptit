package com.homely.student14.repository;

import com.homely.student14.entity.Booking;
import com.homely.student14.entity.Payment;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class ReportRepository {
    private final DatabaseRepository databaseRepository;
    private final BookingRepository bookingRepository;

    private static final String REPORT_DETAILS =
            """
SELECT report.*, reporter.full_name AS reporter_name,
       COALESCE(listing.title, CONCAT('Yêu cầu thuê #', report.booking_id)) AS target_title,
       listing.description AS content_preview, booking.deposit_vnd
FROM reports report
LEFT JOIN users reporter ON reporter.id = report.reporter_id
LEFT JOIN listings listing ON report.target_type = 'listing' AND listing.id = report.target_id
LEFT JOIN bookings booking ON booking.id = report.booking_id
""";

    public Map<String, Object> findById(long reportId) {
        return databaseRepository.one("SELECT * FROM reports WHERE id = ?", reportId);
    }

    public Map<String, Object> findByIdForUpdate(long reportId) {
        return databaseRepository.one("SELECT * FROM reports WHERE id = ? FOR UPDATE", reportId);
    }

    public Map<String, Object> findDetailsById(long reportId) {
        return databaseRepository.one(REPORT_DETAILS + "WHERE report.id = ?", reportId);
    }

    public List<Map<String, Object>> findByBookingId(long bookingId) {
        return databaseRepository.all(
                REPORT_DETAILS + "WHERE report.booking_id = ? ORDER BY report.id DESC", bookingId);
    }

    public List<Map<String, Object>> findContentReportsByReporterId(long reporterId) {
        return databaseRepository.all(
                REPORT_DETAILS
                        + "WHERE report.target_type <> 'booking' AND report.reporter_id = ? ORDER"
                        + " BY report.id DESC",
                reporterId);
    }

    public List<Map<String, Object>> findAllContentReports() {
        return databaseRepository.all(
                REPORT_DETAILS + "WHERE report.target_type <> 'booking' ORDER BY report.id DESC");
    }

    public List<Map<String, Object>> findAllBookingCases() {
        return databaseRepository.all(
                REPORT_DETAILS + "WHERE report.target_type = 'booking' ORDER BY report.id DESC");
    }

    public long findPublishedListingHostId(long listingId) {
        Map<String, Object> listing =
                databaseRepository.one(
                        """
                        SELECT room.host_id
                        FROM listings listing JOIN rooms room ON room.id = listing.room_id
                        WHERE listing.id = ? AND listing.status = 'PUBLISHED'
                        """,
                        listingId);
        return DatabaseRepository.id(listing.get("host_id"));
    }

    public long create(
            long reporterId,
            String targetType,
            long targetId,
            String reasonCode,
            String description) {
        Long bookingId = "booking".equals(targetType) ? targetId : null;
        return databaseRepository.insert(
                """
INSERT INTO reports(reporter_id, target_type, target_id, booking_id, reason_code, description)
VALUES (?, ?, ?, ?, ?, ?)
""",
                reporterId,
                targetType,
                targetId,
                bookingId,
                reasonCode,
                description);
    }

    public Booking toBooking(Map<String, Object> bookingDetails) {
        return bookingRepository.toBooking(bookingDetails);
    }

    public Booking findBookingForUpdate(long bookingId) {
        // BookingRepository khóa phòng trước, sau đó mới khóa yêu cầu thuê.
        return bookingRepository.findByIdForUpdate(bookingId);
    }

    public Payment findPaymentForUpdate(long paymentId) {
        return bookingRepository.findPayment(paymentId, true);
    }

    public boolean hasFinancialDecision(long bookingId) {
        List<Map<String, Object>> resolvedCases =
                databaseRepository.all(
                        """
                        SELECT id FROM reports
                        WHERE booking_id = ? AND status = 'RESOLVED' AND decision <> 'NO_ACTION'
                        """,
                        bookingId);
        return !resolvedCases.isEmpty();
    }

    public void resolveContentReport(long reportId, String status, String note, long adminId) {
        databaseRepository.update(
                """
                UPDATE reports
                SET status = ?, resolution_note = ?, resolved_by = ?,
                    resolved_at = UTC_TIMESTAMP(3), version = version + 1
                WHERE id = ?
                """,
                status,
                note,
                adminId,
                reportId);
    }

    public void resolveBookingCase(
            long reportId,
            String decision,
            BigDecimal tenantRefund,
            BigDecimal hostRetained,
            String note,
            long adminId) {
        databaseRepository.update(
                """
UPDATE reports
SET status = 'RESOLVED', decision = ?, tenant_refund_vnd = ?, host_retained_vnd = ?,
    resolution_note = ?, resolved_by = ?, resolved_at = UTC_TIMESTAMP(3),
    version = version + 1
WHERE id = ?
""",
                decision,
                tenantRefund,
                hostRetained,
                note,
                adminId,
                reportId);
    }

    public void refundPayment(long paymentId, BigDecimal refundAmount) {
        databaseRepository.update(
                """
                UPDATE payments
                SET refund_amount_vnd = ?, refund_status = 'SUCCEEDED',
                    refund_reason = 'BOOKING_CASE', refunded_at = UTC_TIMESTAMP(3)
                WHERE id = ?
                """,
                refundAmount,
                paymentId);
    }

    public void cancelBookingAfterSettlement(long bookingId, String note) {
        databaseRepository.update(
                """
                UPDATE bookings
                SET status = 'CANCELLED', cancelled_at = UTC_TIMESTAMP(3),
                    cancel_reason_code = 'ADMIN_RESOLVED', last_reason = ?
                WHERE id = ?
                """,
                note,
                bookingId);
    }

    public void releaseRoom(long roomId) {
        databaseRepository.update(
                "UPDATE rooms SET availability = 'AVAILABLE' WHERE id = ?", roomId);
    }

    public void notifyReporter(long reporterId, long reportId, String note) {
        databaseRepository.notify(
                reporterId, "SYSTEM", "Báo cáo đã được xử lý", note, "report", reportId);
    }
}
