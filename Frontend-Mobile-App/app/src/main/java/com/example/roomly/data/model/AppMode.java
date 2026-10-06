package com.example.roomly.data.model;

/**
 * Chế độ giao diện người dùng đang sử dụng.
 * Chế độ không thay đổi vai trò hoặc quyền trong phiên đăng nhập.
 */
public enum AppMode {

    // Giao diện tìm phòng, lưu phòng và quản lý yêu cầu của người thuê.
    TENANT,

    // Giao diện quản lý phòng, bài đăng và yêu cầu của chủ trọ.
    HOST
}