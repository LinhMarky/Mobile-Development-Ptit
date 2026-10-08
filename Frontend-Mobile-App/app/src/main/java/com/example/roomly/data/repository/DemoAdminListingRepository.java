package com.example.roomly.data.repository;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;

import com.example.roomly.BuildConfig;
import com.example.roomly.data.model.AdminListing;
import com.example.roomly.data.model.HostListing;
import com.example.roomly.data.model.HostRoom;
import com.example.roomly.data.model.RoomCard;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Quản lý kiểm duyệt bài đăng mẫu trong bộ nhớ.
 * Tách dữ liệu công khai khỏi dữ liệu kiểm duyệt riêng tư.
 */
public final class DemoAdminListingRepository {

    private static final DemoAdminListingRepository INSTANCE =
            new DemoAdminListingRepository();

    private final List<AdminListing> listings = new ArrayList<>();

    // Giữ thông tin phòng tại thời điểm bài đăng được gửi duyệt.
    private final Map<String, HostRoom> submittedRooms = new HashMap<>();

    private boolean demoDataInitialized;

    /** Chỉ cho phép sử dụng repository dùng chung. */
    private DemoAdminListingRepository() {
    }

    /** Trả về repository dùng chung. */
    public static DemoAdminListingRepository getInstance() {
        return INSTANCE;
    }

    /** Lấy danh sách kiểm duyệt theo trạng thái, chỉ dành cho admin. */
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

    /** Lấy chi tiết bài đăng sau khi kiểm tra quyền admin. */
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

    /** Duyệt bài đang chờ và chuyển sang trạng thái công khai. */
    @MainThread
    public AdminListing approveListing(String listingId) {
        return changeStatus(
                listingId,
                AdminListing.Status.PENDING,
                AdminListing.Status.PUBLISHED,
                ""
        );
    }

    /** Từ chối bài đang chờ, bắt buộc nhập lý do. */
    @MainThread
    public AdminListing rejectListing(String listingId, String reason) {
        return changeStatus(
                listingId,
                AdminListing.Status.PENDING,
                AdminListing.Status.REJECTED,
                requireReason(reason)
        );
    }

    /** Ẩn bài đang công khai, bắt buộc nhập lý do. */
    @MainThread
    public AdminListing hideListing(String listingId, String reason) {
        return changeStatus(
                listingId,
                AdminListing.Status.PUBLISHED,
                AdminListing.Status.HIDDEN,
                requireReason(reason)
        );
    }

    /** Khôi phục bài đã ẩn, bắt buộc nhập lý do. */
    @MainThread
    public AdminListing restoreListing(String listingId, String reason) {
        return changeStatus(
                listingId,
                AdminListing.Status.HIDDEN,
                AdminListing.Status.PUBLISHED,
                requireReason(reason)
        );
    }

    /** Kiểm tra quyền và trạng thái mới nhất trước khi kiểm duyệt. */
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

        AdminListing current = getListingById(listingId);

        if (current == null) {
            throw new IllegalStateException("Không tìm thấy bài đăng.");
        }

        if (current.getStatus() != expectedStatus) {
            throw new IllegalStateException(
                    "Trạng thái bài đăng đã thay đổi. "
                            + "Hãy tải lại nội dung trước khi thao tác."
            );
        }

        AdminListing updated = current.withModeration(newStatus, reason);

        for (int index = 0; index < listings.size(); index++) {
            if (listings.get(index).getId().equals(listingId)) {
                listings.set(index, updated);
                return updated;
            }
        }

        throw new IllegalStateException(
                "Bài đăng không còn trong dữ liệu mẫu."
        );
    }

    /**
     * Nhận bản nháp của đúng chủ trọ vào hàng chờ.
     * Giữ thông tin phòng để tạo dữ liệu công khai sau khi được duyệt.
     */
    @MainThread
    public AdminListing submitHostDraft(String listingId) {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()
                || !session.hasRole(UserRole.HOST)
                || !session.isEmailVerified()) {
            throw new IllegalStateException(
                    "Bạn cần quyền chủ trọ và email đã xác minh để gửi duyệt."
            );
        }

        HostListing draft = DemoHostListingRepository.getInstance()
                .getMyListingById(listingId);

        if (draft == null) {
            throw new IllegalStateException(
                    "Không tìm thấy bản nháp thuộc tài khoản của bạn."
            );
        }

        if (draft.getStatus() != HostListing.Status.DRAFT) {
            throw new IllegalStateException(
                    "Chỉ bản nháp mới có thể gửi duyệt."
            );
        }

        HostRoom room = DemoHostRoomRepository.getInstance()
                .getMyRoomById(draft.getRoomId());

        if (room == null) {
            throw new IllegalStateException(
                    "Không tìm thấy phòng thuộc tài khoản của bạn."
            );
        }

        if (!session.getUserId().equals(draft.getOwnerId())
                || !draft.getOwnerId().equals(room.getOwnerId())) {
            throw new IllegalStateException(
                    "Chủ sở hữu bài đăng và phòng không khớp."
            );
        }

        if (draft.getTitle() == null
                || draft.getTitle().trim().isEmpty()
                || draft.getMonthlyRent() <= 0
                || draft.getDescription() == null
                || draft.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Bạn cần hoàn thiện tiêu đề, giá thuê và nội dung."
            );
        }

        initializeDemoData();

        for (AdminListing existing : listings) {
            if (existing.getId().equals(draft.getId())) {
                throw new IllegalStateException(
                        "Bài đăng này đã được gửi duyệt."
                );
            }
        }

        AdminListing submitted = new AdminListing(
                draft.getId(),
                draft.getOwnerId(),
                draft.getRoomId(),
                draft.getTitle(),
                draft.getMonthlyRent(),
                draft.getDescription(),
                session.getFullName(),
                room.getUnitCode(),
                room.getAddress(),
                room.getImageUri(),
                AdminListing.Status.PENDING,
                ""
        );

        submittedRooms.put(submitted.getId(), room);
        listings.add(0, submitted);

        return submitted;
    }

    /** Cho chủ trọ đọc kết quả kiểm duyệt của chính bài đăng mình. */
    @Nullable
    @MainThread
    public AdminListing getMySubmittedListing(String listingId) {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()
                || !session.hasRole(UserRole.HOST)
                || listingId == null) {
            return null;
        }

        for (AdminListing listing : listings) {
            if (listingId.equals(listing.getId())
                    && session.getUserId().equals(listing.getHostId())) {
                return listing;
            }
        }

        return null;
    }

    /**
     * Trả về thông tin của các bài đang công khai.
     * Không đưa lý do kiểm duyệt vào dữ liệu thẻ phòng.
     */
    @MainThread
    public List<RoomCard> getPublishedRoomCards() {
        initializeDemoData();

        List<RoomCard> results = new ArrayList<>();

        for (AdminListing listing : listings) {
            if (listing.getStatus() == AdminListing.Status.PUBLISHED) {
                results.add(toPublicRoomCard(listing));
            }
        }

        return results;
    }

    /** Lấy một bài công khai theo ID mà không yêu cầu đăng nhập. */
    @Nullable
    @MainThread
    public RoomCard getPublishedRoomCardById(String listingId) {
        if (listingId == null || listingId.trim().isEmpty()) {
            return null;
        }

        initializeDemoData();

        for (AdminListing listing : listings) {
            if (listingId.equals(listing.getId())
                    && listing.getStatus()
                    == AdminListing.Status.PUBLISHED) {
                return toPublicRoomCard(listing);
            }
        }

        return null;
    }

    /**
     * Chuyển bài đã duyệt thành dữ liệu công khai.
     * Diện tích lấy từ phòng đã lưu lúc gửi duyệt.
     * Loại phòng sẽ được nối với biểu mẫu ở nhóm tiếp theo.
     */
    private RoomCard toPublicRoomCard(AdminListing listing) {
        HostRoom room = submittedRooms.get(listing.getId());

        String summary = room != null && room.getArea() != null
                ? room.getFormattedArea()
                : "Diện tích chưa cung cấp";

        RoomCard card = new RoomCard(
                listing.getTitle(),
                listing.getFormattedPrice(),
                listing.getAddress(),
                summary,
                0,
                false,
                RoomCard.RoomType.ROOM,
                listing.getRoomId(),
                listing.getHostId(),
                listing.getUnitCode()
        );

        card.setListingId(listing.getId());
        card.setImageUri(listing.getImageUri());
        card.setMonthlyRent(listing.getMonthlyRent());
        card.setDescription(listing.getDescription());
        card.setHostName(listing.getHostName());

        if (room != null) {
            card.setArea(room.getArea());
        }

        return card;
    }

    /** Kiểm tra quyền admin từ phiên hiện tại. */
    private boolean hasAdminAccess() {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        return session.isLoggedIn()
                && session.hasRole(UserRole.ADMIN);
    }

    /** Kiểm tra lý do kiểm duyệt không được để trống. */
    private String requireReason(@Nullable String reason) {
        String cleaned = reason == null ? "" : reason.trim();

        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException(
                    "Bạn hãy nhập lý do kiểm duyệt."
            );
        }

        return cleaned;
    }

    /** Tạo các bài minh họa một lần trong bản debug. */
    private void initializeDemoData() {
        if (!BuildConfig.DEBUG || demoDataInitialized) {
            return;
        }

        listings.add(new AdminListing(
                "admin-demo-listing-01",
                "sample-host-01",
                "sample-room-01",
                "[Mẫu] Studio có ban công gần trường",
                3_500_000L,
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
                2_800_000L,
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
                4_200_000L,
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