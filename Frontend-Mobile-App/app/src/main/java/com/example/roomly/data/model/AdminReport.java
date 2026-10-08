package com.example.roomly.data.model;

/**
 * Dữ liệu báo cáo vi phạm dùng để thử giao diện quản trị.
 * Không phải DTO gửi trực tiếp đến backend.
 */
public class AdminReport {

    public enum ContentType {
        LISTING,
        REVIEW,
        MESSAGE
    }

    public enum Status {
        OPEN,
        RESOLVED,
        DISMISSED
    }

    private final String id;
    private final ContentType contentType;
    private final String targetId;
    private final String targetTitle;
    private final String contentPreview;
    private final String reporterName;
    private final String reportReason;
    private final long createdAtMillis;
    private final Status status;
    private final String resolutionNote;

    /**
     * Khởi tạo báo cáo và thông tin nội dung được báo cáo.
     */
    public AdminReport(
            String id,
            ContentType contentType,
            String targetId,
            String targetTitle,
            String contentPreview,
            String reporterName,
            String reportReason,
            long createdAtMillis,
            Status status,
            String resolutionNote
    ) {
        if (contentType == null || status == null) {
            throw new IllegalArgumentException(
                    "Báo cáo cần có loại nội dung và trạng thái."
            );
        }

        this.id = id;
        this.contentType = contentType;
        this.targetId = targetId;
        this.targetTitle = targetTitle;
        this.contentPreview = contentPreview;
        this.reporterName = reporterName;
        this.reportReason = reportReason;
        this.createdAtMillis = createdAtMillis;
        this.status = status;
        this.resolutionNote = resolutionNote == null
                ? ""
                : resolutionNote;
    }

    /** Trả về mã báo cáo. */
    public String getId() {
        return id;
    }

    /** Trả về loại nội dung bị báo cáo. */
    public ContentType getContentType() {
        return contentType;
    }

    /** Trả về mã nội dung bị báo cáo. */
    public String getTargetId() {
        return targetId;
    }

    /** Trả về tiêu đề mô tả nội dung bị báo cáo. */
    public String getTargetTitle() {
        return targetTitle;
    }

    /** Trả về nội dung để admin xem xét trong dữ liệu mẫu. */
    public String getContentPreview() {
        return contentPreview;
    }

    /** Trả về tên người gửi báo cáo. */
    public String getReporterName() {
        return reporterName;
    }

    /** Trả về lý do người dùng gửi báo cáo. */
    public String getReportReason() {
        return reportReason;
    }

    /** Trả về thời điểm tạo báo cáo. */
    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    /** Trả về trạng thái xử lý báo cáo. */
    public Status getStatus() {
        return status;
    }

    /** Trả về ghi chú kết quả xử lý của admin. */
    public String getResolutionNote() {
        return resolutionNote;
    }

    /** Trả về tên loại nội dung để hiển thị. */
    public String getContentTypeLabel() {
        switch (contentType) {
            case LISTING:
                return "Bài đăng";

            case REVIEW:
                return "Đánh giá";

            case MESSAGE:
                return "Tin nhắn";

            default:
                return "";
        }
    }

    /** Trả về tên trạng thái xử lý để hiển thị. */
    public String getStatusLabel() {
        switch (status) {
            case OPEN:
                return "Chưa xử lý";

            case RESOLVED:
                return "Đã xử lý • Ghi nhận vi phạm";

            case DISMISSED:
                return "Đã xử lý • Không ghi nhận vi phạm";

            default:
                return "";
        }
    }

    /**
     * Tạo bản sao chứa kết quả xử lý mới.
     * Repository sẽ kiểm tra quyền và điều kiện cập nhật.
     */
    public AdminReport withResolution(
            Status newStatus,
            String note
    ) {
        return new AdminReport(
                id,
                contentType,
                targetId,
                targetTitle,
                contentPreview,
                reporterName,
                reportReason,
                createdAtMillis,
                newStatus,
                note
        );
    }
}