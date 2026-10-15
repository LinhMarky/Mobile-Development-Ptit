package com.homely.student14.entity;

/** Trạng thái lưu trong listings; FE hiển thị cùng giá trị từ API. */
public enum ListingStatus {
    DRAFT, PENDING_REVIEW, PUBLISHED, HIDDEN, REJECTED, SUSPENDED, EXPIRED, ARCHIVED;

    public boolean canHostEdit() {
        return this == DRAFT || this == REJECTED || this == HIDDEN
                || this == EXPIRED || this == PUBLISHED;
    }

    public boolean canHostSubmit() {
        return this == DRAFT || this == REJECTED || this == HIDDEN || this == EXPIRED;
    }

    public boolean canHostHide() {
        return this == PUBLISHED;
    }

    public boolean canAdminSuspend() {
        return this == PUBLISHED || this == HIDDEN;
    }
}
