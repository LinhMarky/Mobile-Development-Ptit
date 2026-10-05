package com.example.roomly.data.repository;

/**
 * Giữ dữ liệu hồ sơ mẫu dùng chung trong ứng dụng.
 * Dữ liệu được giữ trong bộ nhớ và chưa kết nối backend.
 */
public class DemoProfileRepository {

    // Một đối tượng dùng chung cho các màn hình.
    private static final DemoProfileRepository INSTANCE =
            new DemoProfileRepository();

    // Thông tin tài khoản mẫu.
    private String fullName = "Nguyễn Minh An";
    private final String email = "minhan@example.com";
    private String phone = "0901234567";

    /**
     * Chỉ tạo hồ sơ mẫu bên trong lớp này.
     */
    private DemoProfileRepository() {
    }

    /**
     * Trả về repository dùng chung trong ứng dụng.
     */
    public static DemoProfileRepository getInstance() {
        return INSTANCE;
    }

    /**
     * Trả về họ tên đang được lưu.
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Trả về email của tài khoản mẫu.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Trả về số điện thoại đang được lưu.
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Cập nhật họ tên và số điện thoại đã được kiểm tra
     * hợp lệ tại màn hình chỉnh sửa hồ sơ.
     */
    public void updateProfile(String fullName, String phone) {
        this.fullName = fullName;
        this.phone = phone;
    }
}