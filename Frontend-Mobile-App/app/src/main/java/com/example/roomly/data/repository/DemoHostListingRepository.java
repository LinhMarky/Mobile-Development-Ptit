package com.example.roomly.data.repository;

import androidx.annotation.MainThread;

import com.example.roomly.data.model.HostListing;
import com.example.roomly.data.model.HostRoom;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;
import com.example.roomly.data.model.AdminListing;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Lưu bản nháp bài đăng mẫu trong bộ nhớ.
 * Dữ liệu được tách theo chủ sở hữu và mất khi tiến trình kết thúc.
 */
public class DemoHostListingRepository {

    private static final DemoHostListingRepository INSTANCE =
            new DemoHostListingRepository();

    private final List<HostListing> listings = new ArrayList<>();

    /**
     * Chỉ cho phép sử dụng repository dùng chung.
     */
    private DemoHostListingRepository() {
    }

    /**
     * Trả về repository dùng chung trong ứng dụng.
     */
    public static DemoHostListingRepository getInstance() {
        return INSTANCE;
    }

    /**
     * Trả về các bản nháp thuộc tài khoản chủ trọ hiện tại.
     * Khách hoặc tài khoản không có quyền HOST nhận danh sách trống.
     */
    @MainThread
    public List<HostListing> getMyListings() {
        List<HostListing> result = new ArrayList<>();

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()
                || !session.hasRole(UserRole.HOST)) {
            return result;
        }

        for (HostListing listing : listings) {
            if (session.getUserId().equals(listing.getOwnerId())) {
                result.add(withCurrentModeration(listing));
            }
        }

        return result;
    }

    /**
     * Tìm bản nháp theo ID và kiểm tra chủ sở hữu.
     * Trả về null nếu không tìm thấy hoặc không có quyền truy cập.
     */
    @MainThread
    public HostListing getMyListingById(String listingId) {
        if (listingId == null || listingId.trim().isEmpty()) {
            return null;
        }

        for (HostListing listing : getMyListings()) {
            if (listingId.equals(listing.getId())) {
                return listing;
            }
        }

        return null;
    }

    /**
     * Tạo bản nháp cho phòng thuộc tài khoản hiện tại.
     * Kiểm tra quyền HOST, xác minh email và quyền sở hữu phòng.
     */
    @MainThread
    public HostListing createDraft(
            String roomId,
            String title,
            long monthlyRent,
            String description
    ) {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()
                || !session.hasRole(UserRole.HOST)
                || !session.isEmailVerified()) {
            throw new IllegalStateException(
                    "Bạn cần đăng nhập bằng tài khoản chủ trọ "
                            + "và xác minh email để tạo bài đăng."
            );
        }

        HostRoom room = DemoHostRoomRepository.getInstance()
                .getMyRoomById(roomId);

        if (room == null) {
            throw new IllegalStateException(
                    "Không tìm thấy phòng thuộc tài khoản của bạn."
            );
        }

        String cleanTitle = cleanText(title);
        String cleanDescription = cleanText(description);

        if (cleanTitle.isEmpty()) {
            throw new IllegalArgumentException(
                    "Vui lòng nhập tiêu đề bài đăng."
            );
        }

        if (monthlyRent <= 0) {
            throw new IllegalArgumentException(
                    "Giá thuê phải lớn hơn 0."
            );
        }

        if (cleanDescription.isEmpty()) {
            throw new IllegalArgumentException(
                    "Vui lòng nhập nội dung bài đăng."
            );
        }

        HostListing listing = new HostListing(
                UUID.randomUUID().toString(),
                session.getUserId(),
                room.getId(),
                cleanTitle,
                monthlyRent,
                cleanDescription,
                System.currentTimeMillis()
        );

        // Thêm đầu danh sách để bản nháp mới xuất hiện trước.
        listings.add(0, listing);

        return listing;
    }

    /**
     * Lấy trạng thái kiểm duyệt mới nhất cho bài của chủ trọ.
     * Khi chưa gửi duyệt, giữ nguyên bản nháp.
     */
    private HostListing withCurrentModeration(HostListing listing) {
        AdminListing submitted =
                DemoAdminListingRepository.getInstance()
                        .getMySubmittedListing(listing.getId());

        if (submitted == null) {
            return listing;
        }

        HostListing.Status status;

        switch (submitted.getStatus()) {
            case PENDING:
                status = HostListing.Status.PENDING;
                break;

            case PUBLISHED:
                status = HostListing.Status.PUBLISHED;
                break;

            case HIDDEN:
                status = HostListing.Status.HIDDEN;
                break;

            case REJECTED:
                status = HostListing.Status.REJECTED;
                break;

            default:
                throw new IllegalStateException(
                        "Trạng thái kiểm duyệt chưa được hỗ trợ."
                );
        }

        return listing.withModeration(
                status,
                submitted.getModerationReason()
        );
    }

    /**
     * Gửi bản nháp của tài khoản hiện tại sang hàng chờ admin.
     * Trả về bài đăng với trạng thái đã cập nhật.
     */
    @MainThread
    public HostListing submitDraft(String listingId) {
        DemoAdminListingRepository.getInstance()
                .submitHostDraft(listingId);

        return getMyListingById(listingId);
    }

    /**
     * Cập nhật nội dung bản nháp thuộc tài khoản hiện tại.
     * Giữ nguyên mã bài đăng, chủ sở hữu, phòng liên kết và thời điểm tạo.
     * Dữ liệu hiện chỉ được lưu trong bộ nhớ để thử giao diện.
     */
    @androidx.annotation.MainThread
    public HostListing updateDraft(
            String listingId,
            String title,
            long monthlyRent,
            String description
    ) {
        com.example.roomly.data.model.SessionState session =
                SessionRepository.getInstance().getCurrentSession();

        if (!session.isLoggedIn()) {
            throw new IllegalStateException(
                    "Bạn cần đăng nhập để chỉnh sửa bản nháp."
            );
        }

        if (!session.isEmailVerified()) {
            throw new IllegalStateException(
                    "Bạn cần xác minh email để chỉnh sửa bản nháp."
            );
        }

        // Hàm này chỉ trả về bài đăng thuộc tài khoản có quyền HOST.
        HostListing currentListing = getMyListingById(listingId);

        if (currentListing == null) {
            throw new IllegalStateException(
                    "Không tìm thấy bản nháp thuộc tài khoản của bạn."
            );
        }

        // Không sửa nội dung đã gửi duyệt bằng hàm cập nhật bản nháp.
        if (currentListing.getStatus() != HostListing.Status.DRAFT) {
            throw new IllegalStateException(
                    "Chỉ có thể chỉnh sửa bài đang ở trạng thái Bản nháp."
            );
        }

        // Kiểm tra phòng liên kết vẫn thuộc tài khoản hiện tại.
        if (DemoHostRoomRepository.getInstance().getMyRoomById(
                currentListing.getRoomId()
        ) == null) {
            throw new IllegalStateException(
                    "Không tìm thấy phòng liên kết với bản nháp."
            );
        }

        String cleanedTitle = cleanText(title);
        String cleanedDescription = cleanText(description);

        if (cleanedTitle.isEmpty()) {
            throw new IllegalArgumentException(
                    "Bạn hãy nhập tiêu đề bài đăng."
            );
        }

        if (monthlyRent <= 0) {
            throw new IllegalArgumentException(
                    "Giá thuê phải lớn hơn 0."
            );
        }

        if (cleanedDescription.isEmpty()) {
            throw new IllegalArgumentException(
                    "Bạn hãy nhập nội dung bài đăng."
            );
        }

        HostListing updatedListing = new HostListing(
                currentListing.getId(),
                currentListing.getOwnerId(),
                currentListing.getRoomId(),
                cleanedTitle,
                monthlyRent,
                cleanedDescription,
                currentListing.getCreatedAtMillis()
        );

        // Thay thế bản nháp tại vị trí cũ trong danh sách.
        for (int index = 0; index < listings.size(); index++) {
            if (listings.get(index).getId().equals(currentListing.getId())) {
                listings.set(index, updatedListing);
                return updatedListing;
            }
        }

        throw new IllegalStateException(
                "Bản nháp không còn trong dữ liệu mẫu."
        );
    }

    /**
     * Loại bỏ khoảng trắng ở hai đầu và xử lý giá trị null.
     */
    private String cleanText(String text) {
        return text == null ? "" : text.trim();
    }
}