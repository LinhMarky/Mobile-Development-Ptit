package com.example.roomly.data.repository;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;

import com.example.roomly.BuildConfig;
import com.example.roomly.data.model.AdminAccount;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

/**
 * Cung cấp tài khoản mẫu cho giao diện quản trị.
 * Chỉ tạo dữ liệu mẫu trong bản debug.
 * Mọi thao tác đều kiểm tra quyền admin hiện tại.
 */
public final class DemoAdminAccountRepository {

    private static final DemoAdminAccountRepository INSTANCE =
            new DemoAdminAccountRepository();

    private final List<AdminAccount> accounts = new ArrayList<>();
    private boolean initialized;

    /** Ngăn tạo repository từ bên ngoài. */
    private DemoAdminAccountRepository() {
    }

    /** Trả về repository dùng chung. */
    public static DemoAdminAccountRepository getInstance() {
        return INSTANCE;
    }

    /**
     * Lấy danh sách tài khoản theo trạng thái và từ khóa tên/email.
     * Trạng thái null nghĩa là lấy tất cả.
     * Người không có quyền admin nhận danh sách trống.
     */
    @MainThread
    public List<AdminAccount> getAccounts(
            @Nullable AdminAccount.Status status,
            @Nullable String keyword
    ) {
        List<AdminAccount> results = new ArrayList<>();

        if (!hasAdminAccess()) {
            return results;
        }

        initializeDemoData();

        String query = normalizeText(keyword);

        for (AdminAccount account : accounts) {
            boolean matchesStatus =
                    status == null || account.getStatus() == status;

            boolean matchesKeyword =
                    query.isEmpty()
                            || normalizeText(account.getFullName())
                            .contains(query)
                            || normalizeText(account.getEmail())
                            .contains(query);

            if (matchesStatus && matchesKeyword) {
                results.add(account);
            }
        }

        return results;
    }

    /**
     * Tìm tài khoản theo mã sau khi kiểm tra quyền admin.
     * Trả về null nếu tài khoản không tồn tại hoặc không có quyền.
     */
    @MainThread
    @Nullable
    public AdminAccount getAccountById(@Nullable String accountId) {
        if (!hasAdminAccess() || accountId == null) {
            return null;
        }

        initializeDemoData();

        for (AdminAccount account : accounts) {
            if (account.getId().equals(accountId)) {
                return account;
            }
        }

        return null;
    }

    /**
     * Khóa tài khoản mẫu đang hoạt động với lý do bắt buộc.
     * Giao diện thử không cho khóa chính mình hoặc tài khoản admin.
     * Thao tác này không gọi API và không thay đổi phiên đăng nhập.
     */
    @MainThread
    public void suspendAccount(String accountId, String reason) {
        if (!hasAdminAccess()) {
            throw new IllegalStateException(
                    "Bạn không có quyền quản lý tài khoản."
            );
        }

        AdminAccount account = getAccountById(accountId);

        if (account == null) {
            throw new IllegalStateException(
                    "Tài khoản không còn tồn tại."
            );
        }

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (account.getId().equals(session.getUserId())) {
            throw new IllegalStateException(
                    "Không thể khóa chính tài khoản đang sử dụng."
            );
        }

        if (account.hasRole(UserRole.ADMIN)) {
            throw new IllegalStateException(
                    "Giao diện thử chưa hỗ trợ khóa tài khoản admin."
            );
        }

        if (account.getStatus() != AdminAccount.Status.ACTIVE) {
            throw new IllegalStateException(
                    "Tài khoản này đã bị khóa."
            );
        }

        String trimmedReason = reason == null ? "" : reason.trim();

        if (trimmedReason.isEmpty()) {
            throw new IllegalArgumentException(
                    "Bạn cần nhập lý do khóa tài khoản."
            );
        }

        for (int index = 0; index < accounts.size(); index++) {
            if (accounts.get(index).getId().equals(accountId)) {
                accounts.set(
                        index,
                        account.withSuspension(trimmedReason)
                );
                return;
            }
        }

        throw new IllegalStateException(
                "Không thể cập nhật tài khoản."
        );
    }

    /** Kiểm tra phiên hiện tại có quyền admin hay không. */
    private boolean hasAdminAccess() {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        return session.isLoggedIn()
                && session.hasRole(UserRole.ADMIN);
    }

    /**
     * Chuẩn hóa chữ thường, dấu tiếng Việt và khoảng trắng
     * để có thể tìm tên có dấu hoặc không dấu.
     */
    private String normalizeText(@Nullable String text) {
        if (text == null) {
            return "";
        }

        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replace('đ', 'd')
                .trim()
                .replaceAll("\\s+", " ");
    }

    /** Tạo các tài khoản minh họa một lần trong bản debug. */
    private void initializeDemoData() {
        if (initialized) {
            return;
        }

        initialized = true;

        if (!BuildConfig.DEBUG) {
            return;
        }

        accounts.add(new AdminAccount(
                "sample-tenant-01",
                "[Mẫu] Nguyễn Minh Anh",
                "tenant01@example.com",
                EnumSet.of(UserRole.TENANT),
                true,
                AdminAccount.Status.ACTIVE,
                ""
        ));

        accounts.add(new AdminAccount(
                "sample-host-01",
                "[Mẫu] Trần Hoàng Nam",
                "host01@example.com",
                EnumSet.of(UserRole.TENANT, UserRole.HOST),
                true,
                AdminAccount.Status.ACTIVE,
                ""
        ));

        accounts.add(new AdminAccount(
                "sample-tenant-02",
                "[Mẫu] Lê Thu Hà",
                "tenant02@example.com",
                EnumSet.of(UserRole.TENANT),
                false,
                AdminAccount.Status.SUSPENDED,
                "Lý do khóa mẫu để kiểm tra giao diện."
        ));

        accounts.add(new AdminAccount(
                "sample-tenant-03",
                "[Mẫu] Phạm Đức Huy",
                "tenant03@example.com",
                EnumSet.of(UserRole.TENANT),
                false,
                AdminAccount.Status.ACTIVE,
                ""
        ));
    }
}