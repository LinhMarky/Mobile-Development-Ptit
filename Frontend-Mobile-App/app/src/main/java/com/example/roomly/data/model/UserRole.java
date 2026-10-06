package com.example.roomly.data.model;

/**
 * Các vai trò tài khoản nhận từ backend.
 * Một tài khoản có thể đồng thời có TENANT và HOST.
 */
public enum UserRole {

    // Người tìm và thuê phòng.
    TENANT,

    // Chủ trọ, quản lý phòng và tin đăng của mình.
    HOST,

    // Quản trị viên, sử dụng các chức năng quản trị được cấp.
    ADMIN
}