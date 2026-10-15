package com.example.roomly.data.repository;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;

import com.example.roomly.BuildConfig;
import com.example.roomly.data.model.AdminReport;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DemoAdminReportRepository {

    private static final DemoAdminReportRepository INSTANCE =
            new DemoAdminReportRepository();

    private final List<AdminReport> reports = new ArrayList<>();

    private final Map<String, String> reporterIds = new HashMap<>();

    private boolean initialized;

    private DemoAdminReportRepository() {
    }

    public static DemoAdminReportRepository getInstance() {
        return INSTANCE;
    }

    @MainThread
    public AdminReport createListingReport(
            String expectedUserId,
            String listingId,
            String listingTitle,
            String contentPreview,
            String reason,
            String description
    ) {
        if (!BuildConfig.DEBUG) {
            throw new IllegalStateException(
                    "Chức năng báo cáo demo chỉ dùng trong bản debug."
            );
        }

        SessionState session =
                SessionRepository.getInstance().getCurrentSession();

        if (!session.isLoggedIn()
                || !normalize(session.getUserId()).equals(
                requireText(expectedUserId, "Mã người gửi")
        )) {
            throw new IllegalStateException(
                    "Phiên đăng nhập đã thay đổi. Vui lòng mở lại báo cáo."
            );
        }

        if (!session.isEmailVerified()) {
            throw new IllegalStateException(
                    "Bạn cần xác minh email trước khi gửi báo cáo."
            );
        }

        String targetId = requireText(listingId, "Mã bài đăng");
        String title = requireText(listingTitle, "Tiêu đề bài đăng");
        String cleanedReason = requireText(reason, "Lý do báo cáo");
        String cleanedDescription = normalize(description);

        if (cleanedDescription.codePointCount(
                0, cleanedDescription.length()
        ) > 2000) {
            throw new IllegalArgumentException(
                    "Mô tả không được vượt quá 2000 ký tự."
            );
        }

        initializeDemoData();

        String reportReason = cleanedReason;

        if (!cleanedDescription.isEmpty()) {
            reportReason += "\n\nMô tả: " + cleanedDescription;
        }

        AdminReport report = new AdminReport(
                "DEMO-REPORT-" + UUID.randomUUID(),
                AdminReport.ContentType.LISTING,
                targetId,
                title,
                normalize(contentPreview),
                session.getFullName(),
                reportReason,
                System.currentTimeMillis(),
                AdminReport.Status.OPEN,
                ""
        );

        reports.add(report);
        reporterIds.put(report.getId(), session.getUserId());

        return report;
    }

    @MainThread
    public List<AdminReport> getMyReports() {
        List<AdminReport> results = new ArrayList<>();

        SessionState session =
                SessionRepository.getInstance().getCurrentSession();

        if (!session.isLoggedIn()) {
            return results;
        }

        for (AdminReport report : reports) {
            String reporterId = reporterIds.get(report.getId());

            if (session.getUserId().equals(reporterId)) {
                results.add(report);
            }
        }

        sortNewestFirst(results);
        return results;
    }

    // closed: null = tất cả, false = chưa xử lý, true = đã xử lý.
    @MainThread
    public List<AdminReport> getReports(@Nullable Boolean closed) {
        List<AdminReport> results = new ArrayList<>();

        if (!hasAdminAccess()) {
            return results;
        }

        initializeDemoData();

        for (AdminReport report : reports) {
            boolean isClosed =
                    report.getStatus() != AdminReport.Status.OPEN;

            if (closed == null || closed.booleanValue() == isClosed) {
                results.add(report);
            }
        }

        sortNewestFirst(results);
        return results;
    }

    @Nullable
    @MainThread
    public AdminReport getReportById(String reportId) {
        if (!hasAdminAccess() || reportId == null) {
            return null;
        }

        initializeDemoData();

        for (AdminReport report : reports) {
            if (report.getId().equals(reportId)) {
                return report;
            }
        }

        return null;
    }

    @MainThread
    public AdminReport resolveReport(
            String reportId,
            boolean violationFound,
            String note
    ) {
        if (!hasAdminAccess()) {
            throw new IllegalStateException(
                    "Tài khoản không có quyền quản trị."
            );
        }

        AdminReport currentReport = getReportById(reportId);

        if (currentReport == null) {
            throw new IllegalStateException(
                    "Không tìm thấy báo cáo."
            );
        }

        if (currentReport.getStatus() != AdminReport.Status.OPEN) {
            throw new IllegalStateException(
                    "Báo cáo đã được xử lý. Hãy tải lại nội dung."
            );
        }

        String cleanedNote = requireText(
                note,
                "Ghi chú kết quả xử lý"
        );

        AdminReport.Status status = violationFound
                ? AdminReport.Status.RESOLVED
                : AdminReport.Status.DISMISSED;

        AdminReport updatedReport =
                currentReport.withResolution(status, cleanedNote);

        for (int index = 0; index < reports.size(); index++) {
            if (reports.get(index).getId().equals(reportId)) {
                reports.set(index, updatedReport);
                return updatedReport;
            }
        }

        throw new IllegalStateException(
                "Báo cáo không còn trong dữ liệu mẫu."
        );
    }

    private boolean hasAdminAccess() {
        SessionState session =
                SessionRepository.getInstance().getCurrentSession();

        return session.isLoggedIn()
                && session.hasRole(UserRole.ADMIN);
    }

    private void sortNewestFirst(List<AdminReport> items) {
        items.sort(
                Comparator.comparingLong(
                        AdminReport::getCreatedAtMillis
                ).reversed()
        );
    }

    private void initializeDemoData() {
        if (!BuildConfig.DEBUG || initialized) {
            return;
        }

        long now = System.currentTimeMillis();

        reports.add(new AdminReport(
                "admin-demo-report-01",
                AdminReport.ContentType.LISTING,
                "admin-demo-listing-02",
                "[Mẫu] Báo cáo bài đăng phòng trọ",
                "Nội dung mẫu: phòng đầy đủ nội thất, "
                        + "giá thuê 2.800.000 ₫/tháng.",
                "Người gửi mẫu 01",
                "Thông tin giá thuê có thể chưa chính xác.",
                now - 30 * 60 * 1000L,
                AdminReport.Status.OPEN,
                ""
        ));

        reports.add(new AdminReport(
                "admin-demo-report-02",
                AdminReport.ContentType.REVIEW,
                "sample-review-01",
                "[Mẫu] Báo cáo đánh giá",
                "Nội dung đánh giá mẫu cần được admin xem xét.",
                "Người gửi mẫu 02",
                "Đánh giá có dấu hiệu sử dụng ngôn từ không phù hợp.",
                now - 2 * 60 * 60 * 1000L,
                AdminReport.Status.OPEN,
                ""
        ));

        reports.add(new AdminReport(
                "admin-demo-report-03",
                AdminReport.ContentType.MESSAGE,
                "sample-message-01",
                "[Mẫu] Báo cáo tin nhắn",
                "Nội dung tin nhắn mẫu: lời mời quảng cáo "
                        + "không liên quan đến việc thuê phòng.",
                "Người gửi mẫu 03",
                "Tin nhắn quảng cáo không mong muốn.",
                now - 24 * 60 * 60 * 1000L,
                AdminReport.Status.RESOLVED,
                "Kết quả mẫu: ghi nhận nội dung quảng cáo không phù hợp."
        ));

        initialized = true;
    }

    private static String requireText(String value, String fieldName) {
        String cleaned = normalize(value);

        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException(
                    fieldName + " không được để trống."
            );
        }

        return cleaned;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}