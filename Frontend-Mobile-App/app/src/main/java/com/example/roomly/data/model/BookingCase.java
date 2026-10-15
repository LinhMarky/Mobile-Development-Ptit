package com.example.roomly.data.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class BookingCase {

    public enum Type {
        DEPOSIT_DISPUTE,
        HANDOVER_OVERDUE,
        NO_SHOW,
        OTHER
    }

    public enum Status {
        OPEN,
        IN_REVIEW,
        RESOLVED,
        CLOSED
    }

    public enum Decision {
        REFUND_TENANT,
        FORFEIT_DEPOSIT,
        SPLIT,
        NO_ACTION
    }

    private final String id;
    private final String bookingId;
    private final String openedByUserId;
    private final Type type;
    private final String description;
    private final Status status;
    private final Decision decision;
    private final String resolutionNote;
    private final String resolvedByUserId;
    private final Long resolvedAtMillis;
    private final long createdAtMillis;

    public BookingCase(
            String id,
            String bookingId,
            String openedByUserId,
            Type type,
            String description,
            Status status,
            String resolutionNote,
            long createdAtMillis) {
        this(
                id,
                bookingId,
                openedByUserId,
                type,
                description,
                status,
                null,
                resolutionNote,
                "",
                null,
                createdAtMillis);
    }

    public BookingCase(
            String id,
            String bookingId,
            String openedByUserId,
            Type type,
            String description,
            Status status,
            Decision decision,
            String resolutionNote,
            String resolvedByUserId,
            Long resolvedAtMillis,
            long createdAtMillis) {
        this.id = requireText(id, "Mã yêu cầu hỗ trợ");
        this.bookingId = requireText(bookingId, "Mã yêu cầu thuê");
        this.openedByUserId = requireText(openedByUserId, "Mã người gửi");

        if (type == null || status == null) {
            throw new IllegalArgumentException("Yêu cầu hỗ trợ cần có loại vấn đề và trạng thái.");
        }

        String cleanedDescription = normalize(description);
        int length = cleanedDescription.codePointCount(0, cleanedDescription.length());

        if (length < 10 || length > 5000) {
            throw new IllegalArgumentException("Mô tả phải có từ 10 đến 5000 ký tự.");
        }

        if (createdAtMillis <= 0) {
            throw new IllegalArgumentException("Thời gian tạo yêu cầu không hợp lệ.");
        }

        if (resolvedAtMillis != null && resolvedAtMillis < createdAtMillis) {
            throw new IllegalArgumentException("Thời gian xử lý không hợp lệ.");
        }

        this.type = type;
        this.description = cleanedDescription;
        this.status = status;
        this.decision = decision;
        this.resolutionNote = normalize(resolutionNote);
        this.resolvedByUserId = normalize(resolvedByUserId);
        this.resolvedAtMillis = resolvedAtMillis;
        this.createdAtMillis = createdAtMillis;
    }

    public String getId() {
        return id;
    }

    public String getBookingId() {
        return bookingId;
    }

    public String getOpenedByUserId() {
        return openedByUserId;
    }

    public Type getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public Status getStatus() {
        return status;
    }

    public Decision getDecision() {
        return decision;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public String getResolvedByUserId() {
        return resolvedByUserId;
    }

    public Long getResolvedAtMillis() {
        return resolvedAtMillis;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public boolean isOpen() {
        return status == Status.OPEN || status == Status.IN_REVIEW;
    }

    public String getTypeLabel() {
        switch (type) {
            case DEPOSIT_DISPUTE:
                return "Tranh chấp tiền cọc";

            case HANDOVER_OVERDUE:
                return "Bàn giao quá hạn";

            case NO_SHOW:
                return "Không đến hẹn";

            case OTHER:
            default:
                return "Vấn đề khác";
        }
    }

    public String getStatusLabel() {
        switch (status) {
            case OPEN:
                return "Đã tiếp nhận";

            case IN_REVIEW:
                return "Đang xem xét";

            case RESOLVED:
                return "Đã giải quyết";

            case CLOSED:
            default:
                return "Đã đóng";
        }
    }

    public String getDecisionLabel() {
        if (decision == null) {
            return "Chưa có kết luận";
        }

        switch (decision) {
            case REFUND_TENANT:
                return "Hoàn cọc cho Người thuê";

            case FORFEIT_DEPOSIT:
                return "Người thuê mất cọc";

            case SPLIT:
                return "Chia tiền cọc giữa hai bên";

            case NO_ACTION:
            default:
                return "Không áp dụng xử lý tiền cọc";
        }
    }

    public String getCreatedAtLabel() {
        return formatTime(createdAtMillis);
    }

    public String getResolvedAtLabel() {
        return resolvedAtMillis == null ? "" : formatTime(resolvedAtMillis);
    }

    public BookingCase resolve(Decision decision, String note, String adminUserId) {
        if (!isOpen()) {
            throw new IllegalStateException("Yêu cầu hỗ trợ đã được xử lý.");
        }

        if (decision == null) {
            throw new IllegalArgumentException("Bạn hãy chọn kết luận xử lý.");
        }

        String cleanedNote = requireText(note, "Ghi chú xử lý");

        if (cleanedNote.codePointCount(0, cleanedNote.length()) > 2000) {
            throw new IllegalArgumentException("Ghi chú xử lý không được vượt quá 2000 ký tự.");
        }

        return new BookingCase(
                id,
                bookingId,
                openedByUserId,
                type,
                description,
                Status.RESOLVED,
                decision,
                cleanedNote,
                requireText(adminUserId, "Mã Admin"),
                Math.max(System.currentTimeMillis(), createdAtMillis),
                createdAtMillis);
    }

    private static String formatTime(long millis) {
        SimpleDateFormat formatter =
                new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("vi", "VN"));

        return formatter.format(new Date(millis));
    }

    private static String requireText(String value, String fieldName) {
        String cleaned = normalize(value);

        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " không được để trống.");
        }

        return cleaned;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
