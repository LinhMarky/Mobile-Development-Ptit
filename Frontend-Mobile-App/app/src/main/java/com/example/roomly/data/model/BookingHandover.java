package com.example.roomly.data.model;

public final class BookingHandover {

    private final String bookingId;
    private final Long hostConfirmedAtMillis;
    private final Long tenantConfirmedAtMillis;
    private final boolean demo;

    public BookingHandover(
            String bookingId,
            Long hostConfirmedAtMillis,
            Long tenantConfirmedAtMillis,
            boolean demo
    ) {
        String normalizedId = bookingId == null
                ? ""
                : bookingId.trim();

        if (normalizedId.isEmpty()) {
            throw new IllegalArgumentException(
                    "Mã yêu cầu thuê không được để trống."
            );
        }

        validateTime(hostConfirmedAtMillis);
        validateTime(tenantConfirmedAtMillis);

        this.bookingId = normalizedId;
        this.hostConfirmedAtMillis = hostConfirmedAtMillis;
        this.tenantConfirmedAtMillis = tenantConfirmedAtMillis;
        this.demo = demo;
    }

    public String getBookingId() {
        return bookingId;
    }

    public Long getHostConfirmedAtMillis() {
        return hostConfirmedAtMillis;
    }

    public Long getTenantConfirmedAtMillis() {
        return tenantConfirmedAtMillis;
    }

    public boolean isDemo() {
        return demo;
    }

    public boolean isHostConfirmed() {
        return hostConfirmedAtMillis != null;
    }

    public boolean isTenantConfirmed() {
        return tenantConfirmedAtMillis != null;
    }

    public boolean isCompleted() {
        return isHostConfirmed() && isTenantConfirmed();
    }

    public boolean canHostConfirmDemo() {
        return demo && !isHostConfirmed();
    }

    public boolean canTenantConfirmDemo() {
        return demo && !isTenantConfirmed();
    }

    public BookingHandover confirmHostDemo() {
        if (!demo) {
            throw new IllegalStateException(
                    "Chức năng này chỉ áp dụng cho bàn giao demo."
            );
        }

        if (isHostConfirmed()) {
            return this;
        }

        return new BookingHandover(
                bookingId,
                System.currentTimeMillis(),
                tenantConfirmedAtMillis,
                true
        );
    }

    public BookingHandover confirmTenantDemo() {
        if (!demo) {
            throw new IllegalStateException(
                    "Chức năng này chỉ áp dụng cho bàn giao demo."
            );
        }

        if (isTenantConfirmed()) {
            return this;
        }

        return new BookingHandover(
                bookingId,
                hostConfirmedAtMillis,
                System.currentTimeMillis(),
                true
        );
    }

    public String getStatusLabel() {
        if (isCompleted()) {
            return "Đã hoàn tất bàn giao";
        }

        if (isHostConfirmed()) {
            return "Chờ người thuê xác nhận";
        }

        if (isTenantConfirmed()) {
            return "Chờ chủ trọ xác nhận";
        }

        return "Chờ bàn giao";
    }

    public String getStatusDescription() {
        if (isCompleted()) {
            return "Cả chủ trọ và người thuê đã xác nhận "
                    + "bàn giao demo.";
        }

        if (isHostConfirmed()) {
            return "Chủ trọ đã xác nhận bàn giao demo. "
                    + "Đang chờ người thuê xác nhận nhận phòng.";
        }

        if (isTenantConfirmed()) {
            return "Người thuê đã xác nhận nhận phòng demo. "
                    + "Đang chờ chủ trọ xác nhận bàn giao.";
        }

        return "Chưa bên nào xác nhận bàn giao demo. "
                + "Mỗi bên xác nhận theo vai trò đang sử dụng.";
    }

    private static void validateTime(Long timeMillis) {
        if (timeMillis != null && timeMillis <= 0) {
            throw new IllegalArgumentException(
                    "Thời gian xác nhận phải hợp lệ."
            );
        }
    }
}