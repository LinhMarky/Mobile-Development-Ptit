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
        return createDraft(
                roomId,
                title,
                monthlyRent,
                null,
                description
        );
    }

    @MainThread
    public HostListing createDraft(
            String roomId,
            String title,
            long monthlyRent,
            Long depositVnd,
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

        for (HostListing existing : listings) {
            if (room.getId().equals(existing.getRoomId())) {
                throw new IllegalStateException(
                        "Phòng này đã có bài đăng. "
                                + "Bạn hãy mở bài đăng hiện có để xem hoặc chỉnh sửa."
                );
            }
        }

        String cleanTitle = cleanText(title);
        String cleanDescription = cleanText(description);

        if (cleanTitle.isEmpty()) {
            throw new IllegalArgumentException(
                    "Vui lòng nhập tiêu đề bài đăng."
            );
        }

        if (cleanTitle.length() > 150) {
            throw new IllegalArgumentException(
                    "Tiêu đề bài đăng không được vượt quá 150 ký tự."
            );
        }

        if (monthlyRent <= 0) {
            throw new IllegalArgumentException(
                    "Giá thuê phải lớn hơn 0."
            );
        }

        if (depositVnd != null && depositVnd < 0) {
            throw new IllegalArgumentException(
                    "Tiền cọc không được nhỏ hơn 0."
            );
        }

        if (cleanDescription.isEmpty()) {
            throw new IllegalArgumentException(
                    "Vui lòng nhập nội dung bài đăng."
            );
        }

        if (cleanDescription.length() > 5000) {
            throw new IllegalArgumentException(
                    "Nội dung bài đăng không được vượt quá 5000 ký tự."
            );
        }

        HostListing listing = new HostListing(
                UUID.randomUUID().toString(),
                session.getUserId(),
                room.getId(),
                cleanTitle,
                monthlyRent,
                depositVnd,
                cleanDescription,
                System.currentTimeMillis()
        );

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
            case PENDING_REVIEW:
                status = HostListing.Status.PENDING_REVIEW;
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

    @MainThread
    public HostListing reopenRejectedListing(String listingId) {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()
                || !session.hasRole(UserRole.HOST)
                || !session.isEmailVerified()) {
            throw new IllegalStateException(
                    "Bạn cần quyền chủ trọ và email đã xác minh "
                            + "để chỉnh sửa bài bị từ chối."
            );
        }

        HostListing listing = getMyListingById(listingId);

        if (listing == null) {
            throw new IllegalStateException(
                    "Không tìm thấy bài đăng thuộc tài khoản của bạn."
            );
        }

        if (listing.getStatus() != HostListing.Status.REJECTED) {
            throw new IllegalStateException(
                    "Bài đăng không còn ở trạng thái bị từ chối."
            );
        }

        DemoAdminListingRepository.getInstance()
                .returnRejectedListingToDraft(listingId);

        return getMyListingById(listingId);
    }

    /**
     * Gửi bản nháp của tài khoản hiện tại sang hàng chờ admin.
     * Trả về bài đăng với trạng thái đã cập nhật.
     */
    @MainThread
    public HostListing submitDraft(String listingId) {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()
                || !session.hasRole(UserRole.HOST)
                || !session.isEmailVerified()) {
            throw new IllegalStateException(
                    "Bạn cần đăng nhập bằng tài khoản chủ trọ "
                            + "và xác minh email để gửi duyệt."
            );
        }

        HostListing listing = getMyListingById(listingId);

        if (listing == null) {
            throw new IllegalStateException(
                    "Không tìm thấy bài đăng thuộc tài khoản của bạn."
            );
        }

        if (listing.getStatus() != HostListing.Status.DRAFT) {
            throw new IllegalStateException(
                    "Chỉ có thể gửi duyệt bài đang ở trạng thái Bản nháp."
            );
        }

        String title = cleanText(listing.getTitle());
        String description = cleanText(listing.getDescription());

        if (title.isEmpty() || title.length() > 150) {
            throw new IllegalArgumentException(
                    "Tiêu đề phải có từ 1 đến 150 ký tự."
            );
        }

        if (listing.getMonthlyRent() <= 0) {
            throw new IllegalArgumentException(
                    "Giá thuê phải lớn hơn 0."
            );
        }

        if (description.isEmpty() || description.length() > 5000) {
            throw new IllegalArgumentException(
                    "Nội dung bài đăng phải có từ 1 đến 5000 ký tự."
            );
        }

        DemoAdminListingRepository.getInstance()
                .submitHostDraft(listingId);

        return getMyListingById(listingId);
    }
    /**
     * Cập nhật nội dung bản nháp thuộc tài khoản hiện tại.
     * Giữ nguyên mã bài đăng, chủ sở hữu, phòng liên kết và thời điểm tạo.
     * Dữ liệu hiện chỉ được lưu trong bộ nhớ để thử giao diện.
     */
    @MainThread
    public HostListing updateDraft(
            String listingId,
            String title,
            long monthlyRent,
            String description
    ) {
        HostListing currentListing = getMyListingById(listingId);

        if (currentListing == null) {
            throw new IllegalStateException(
                    "Không tìm thấy bài đăng thuộc tài khoản của bạn."
            );
        }

        // Giao diện cũ chưa truyền tiền cọc: giữ nguyên giá trị hiện có.
        return updateDraft(
                listingId,
                title,
                monthlyRent,
                currentListing.getDepositVnd(),
                description
        );
    }

    @MainThread
    public HostListing updateDraft(
            String listingId,
            String title,
            long monthlyRent,
            Long depositVnd,
            String description
    ) {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()
                || !session.hasRole(UserRole.HOST)) {
            throw new IllegalStateException(
                    "Bạn cần đăng nhập bằng tài khoản chủ trọ "
                            + "để chỉnh sửa bản nháp."
            );
        }

        if (!session.isEmailVerified()) {
            throw new IllegalStateException(
                    "Bạn cần xác minh email để chỉnh sửa bản nháp."
            );
        }

        HostListing currentListing = getMyListingById(listingId);

        if (currentListing == null) {
            throw new IllegalStateException(
                    "Không tìm thấy bài đăng thuộc tài khoản của bạn."
            );
        }

        if (currentListing.getStatus() != HostListing.Status.DRAFT) {
            throw new IllegalStateException(
                    "Chỉ có thể chỉnh sửa bài đang ở trạng thái Bản nháp."
            );
        }

        HostRoom room = DemoHostRoomRepository.getInstance()
                .getMyRoomById(currentListing.getRoomId());

        if (room == null) {
            throw new IllegalStateException(
                    "Không tìm thấy phòng liên kết với bản nháp."
            );
        }

        String cleanTitle = cleanText(title);
        String cleanDescription = cleanText(description);

        if (cleanTitle.isEmpty()) {
            throw new IllegalArgumentException(
                    "Vui lòng nhập tiêu đề bài đăng."
            );
        }

        if (cleanTitle.length() > 150) {
            throw new IllegalArgumentException(
                    "Tiêu đề bài đăng không được vượt quá 150 ký tự."
            );
        }

        if (monthlyRent <= 0) {
            throw new IllegalArgumentException(
                    "Giá thuê phải lớn hơn 0."
            );
        }

        if (depositVnd != null && depositVnd < 0) {
            throw new IllegalArgumentException(
                    "Tiền cọc không được nhỏ hơn 0."
            );
        }

        if (cleanDescription.isEmpty()) {
            throw new IllegalArgumentException(
                    "Vui lòng nhập nội dung bài đăng."
            );
        }

        if (cleanDescription.length() > 5000) {
            throw new IllegalArgumentException(
                    "Nội dung bài đăng không được vượt quá 5000 ký tự."
            );
        }

        HostListing updatedListing = new HostListing(
                currentListing.getId(),
                currentListing.getOwnerId(),
                currentListing.getRoomId(),
                cleanTitle,
                monthlyRent,
                depositVnd,
                cleanDescription,
                currentListing.getCreatedAtMillis()
        );

        for (int index = 0; index < listings.size(); index++) {
            if (currentListing.getId().equals(listings.get(index).getId())) {
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