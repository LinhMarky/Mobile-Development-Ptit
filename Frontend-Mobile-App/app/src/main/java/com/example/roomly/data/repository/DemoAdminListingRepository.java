package com.example.roomly.data.repository;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;

import com.example.roomly.BuildConfig;
import com.example.roomly.data.model.AdminListing;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;

import java.util.ArrayList;
import java.util.List;

/**
 * Quản lý bài đăng mẫu để thử giao diện admin.
 * Các thao tác chỉ thay đổi dữ liệu trong bộ nhớ.
 */
public final class DemoAdminListingRepository {

    private static final DemoAdminListingRepository INSTANCE =
            new DemoAdminListingRepository();

    private final List<AdminListing> listings = new ArrayList<>();

    private boolean demoDataInitialized;

    /**
     * Khởi tạo repository, chưa tạo dữ liệu khi chưa truy cập.
     */
    private DemoAdminListingRepository() {
    }

    /**
     * Trả về repository dùng chung trong ứng dụng.
     */
    public static DemoAdminListingRepository getInstance() {
        return INSTANCE;
    }

    /**
     * Lấy danh sách bài đăng theo trạng thái.
     * Truyền null để lấy tất cả.
     * Trả về danh sách trống nếu tài khoản không có quyền ADMIN.
     */
    @MainThread
    public List<AdminListing> getListings(
            @Nullable AdminListing.Status status
    ) {
        List<AdminListing> results = new ArrayList<>();

        if (!hasAdminAccess()) {
            return results;
        }

        initializeDemoData();

        for (AdminListing listing : listings) {
            if (status == null || listing.getStatus() == status) {
                results.add(listing);
            }
        }

        return results;
    }

    /**
     * Lấy bài đăng theo mã sau khi kiểm tra quyền quản trị.
     */
    @Nullable
    @MainThread
    public AdminListing getListingById(String listingId) {
        if (!hasAdminAccess()
                || listingId == null
                || listingId.trim().isEmpty()) {
            return null;
        }

        initializeDemoData();

        for (AdminListing listing : listings) {
            if (listing.getId().equals(listingId)) {
                return listing;
            }
        }

        return null;
    }

    /**
     * Duyệt bài đăng đang chờ duyệt trong dữ liệu mẫu.
     */
    @MainThread
    public AdminListing approveListing(String listingId) {
        return changeStatus(
                listingId,
                AdminListing.Status.PENDING,
                AdminListing.Status.PUBLISHED,
                ""
        );
    }

    /**
     * Từ chối bài đăng đang chờ duyệt và yêu cầu nhập lý do.
     */
    @MainThread
    public AdminListing rejectListing(
            String listingId,
            String reason
    ) {
        return changeStatus(
                listingId,
                AdminListing.Status.PENDING,
                AdminListing.Status.REJECTED,
                requireReason(reason)
        );
    }

    /**
     * Ẩn bài đăng đang hiển thị và yêu cầu nhập lý do.
     */
    @MainThread
    public AdminListing hideListing(
            String listingId,
            String reason
    ) {
        return changeStatus(
                listingId,
                AdminListing.Status.PUBLISHED,
                AdminListing.Status.HIDDEN,
                requireReason(reason)
        );
    }

    /**
     * Khôi phục hiển thị bài đăng đã ẩn và yêu cầu nhập lý do.
     */
    @MainThread
    public AdminListing restoreListing(
            String listingId,
            String reason
    ) {
        return changeStatus(
                listingId,
                AdminListing.Status.HIDDEN,
                AdminListing.Status.PUBLISHED,
                requireReason(reason)
        );
    }

    /**
     * Kiểm tra quyền và trạng thái hiện tại trước khi cập nhật.
     * Không thay đổi thông tin phòng hoặc chủ sở hữu bài đăng.
     */
    private AdminListing changeStatus(
            String listingId,
            AdminListing.Status expectedStatus,
            AdminListing.Status newStatus,
            String reason
    ) {
        if (!hasAdminAccess()) {
            throw new IllegalStateException(
                    "Tài khoản không có quyền quản trị."
            );
        }

        AdminListing currentListing = getListingById(listingId);

        if (currentListing == null) {
            throw new IllegalStateException(
                    "Không tìm thấy bài đăng."
            );
        }

        if (currentListing.getStatus() != expectedStatus) {
            throw new IllegalStateException(
                    "Trạng thái bài đăng đã thay đổi. "
                            + "Hãy tải lại nội dung trước khi thao tác."
            );
        }

        AdminListing updatedListing =
                currentListing.withModeration(newStatus, reason);

        for (int index = 0; index < listings.size(); index++) {
            if (listings.get(index).getId().equals(listingId)) {
                listings.set(index, updatedListing);
                return updatedListing;
            }
        }

        throw new IllegalStateException(
                "Bài đăng không còn trong dữ liệu mẫu."
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
     * Kiểm tra lý do kiểm duyệt không được để trống.
     */
    private String requireReason(@Nullable String reason) {
        String cleanedReason = reason == null
                ? ""
                : reason.trim();

        if (cleanedReason.isEmpty()) {
            throw new IllegalArgumentException(
                    "Bạn hãy nhập lý do kiểm duyệt."
            );
        }

        return cleanedReason;
    }

    /**
     * Tạo dữ liệu thử một lần trong bản debug.
     * Tất cả tên, tài khoản và bài đăng bên dưới đều là dữ liệu mẫu.
     */
    private void initializeDemoData() {
        if (!BuildConfig.DEBUG || demoDataInitialized) {
            return;
        }

        listings.add(new AdminListing(
                "admin-demo-listing-01",
                "sample-host-01",
                "sample-room-01",
                "[Mẫu] Studio có ban công gần trường",
                3500000L,
                "Bài đăng mẫu để thử duyệt hoặc từ chối.\n"
                        + "Phòng có ban công, khu bếp và nội thất cơ bản.",
                "Chủ trọ mẫu 01",
                "DEMO_01",
                "Mộ Lao, Hà Đông, Hà Nội",
                null,
                AdminListing.Status.PENDING,
                ""
        ));

        listings.add(new AdminListing(
                "admin-demo-listing-02",
                "sample-host-02",
                "sample-room-02",
                "[Mẫu] Phòng trọ đầy đủ nội thất",
                2800000L,
                "Bài đăng mẫu đang hiển thị để thử thao tác ẩn.\n"
                        + "Có điều hòa, chỗ để xe và khu vực giặt đồ.",
                "Chủ trọ mẫu 02",
                "DEMO_02",
                "Trần Phú, Hà Đông, Hà Nội",
                null,
                AdminListing.Status.PUBLISHED,
                ""
        ));

        listings.add(new AdminListing(
                "admin-demo-listing-03",
                "sample-host-03",
                "sample-room-03",
                "[Mẫu] Căn hộ mini có bếp riêng",
                4200000L,
                "Bài đăng mẫu đã ẩn để thử khôi phục hiển thị.\n"
                        + "Có bếp riêng và khu vực sinh hoạt.",
                "Chủ trọ mẫu 03",
                "DEMO_03",
                "Thanh Xuân, Hà Nội",
                null,
                AdminListing.Status.HIDDEN,
                "Lý do mẫu: cần kiểm tra lại nội dung bài đăng."
        ));

        demoDataInitialized = true;
    }
}