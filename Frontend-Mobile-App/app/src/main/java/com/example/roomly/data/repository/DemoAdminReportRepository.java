package com.example.roomly.data.repository;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;

import com.example.roomly.BuildConfig;
import com.example.roomly.data.model.AdminReport;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Quản lý báo cáo vi phạm mẫu cho giao diện admin.
 * Không thay đổi nội dung thật hoặc gửi dữ liệu đến backend.
 */
public final class DemoAdminReportRepository {

    private static final DemoAdminReportRepository INSTANCE =
            new DemoAdminReportRepository();

    private final List<AdminReport> reports = new ArrayList<>();

    private boolean initialized;

    /**
     * Khởi tạo repository, chưa tạo dữ liệu khi chưa truy cập.
     */
    private DemoAdminReportRepository() {
    }

    /**
     * Trả về repository dùng chung.
     */
    public static DemoAdminReportRepository getInstance() {
        return INSTANCE;
    }

    /**
     * Lấy báo cáo theo nhóm trạng thái.
     * Null: tất cả; false: chưa xử lý; true: đã xử lý.
     */
    @MainThread
    public List<AdminReport> getReports(@Nullable Boolean closed) {
        List<AdminReport> results = new ArrayList<>();

        if (!hasAdminAccess()) {
            return results;
        }

        initializeDemoData();

        for (AdminReport report : reports) {
            boolean isClosed = report.getStatus()
                    != AdminReport.Status.OPEN;

            if (closed == null || closed.booleanValue() == isClosed) {
                results.add(report);
            }
        }

        results.sort(
                Comparator.comparingLong(
                        AdminReport::getCreatedAtMillis
                ).reversed()
        );

        return results;
    }

    /**
     * Lấy báo cáo theo mã sau khi kiểm tra quyền ADMIN.
     */
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

    /**
     * Ghi nhận kết quả xử lý báo cáo mẫu kèm ghi chú bắt buộc.
     * Việc đóng báo cáo không tự động ẩn bài đăng, đánh giá hoặc tin nhắn.
     */
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

        String cleanedNote = note == null ? "" : note.trim();

        if (cleanedNote.isEmpty()) {
            throw new IllegalArgumentException(
                    "Bạn hãy nhập ghi chú kết quả xử lý."
            );
        }

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

    /**
     * Kiểm tra phiên mới nhất có quyền ADMIN hay không.
     */
    private boolean hasAdminAccess() {
        SessionState session =
                SessionRepository.getInstance().getCurrentSession();

        return session.isLoggedIn()
                && session.hasRole(UserRole.ADMIN);
    }

    /**
     * Tạo báo cáo thử một lần trong bản debug.
     * Nội dung và người gửi đều là dữ liệu mẫu.
     */
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
}