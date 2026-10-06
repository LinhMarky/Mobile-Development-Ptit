package com.example.roomly.data.repository;

import androidx.annotation.MainThread;

import com.example.roomly.data.model.HostListing;
import com.example.roomly.data.model.HostRoom;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;

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
                result.add(listing);
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
     * Loại bỏ khoảng trắng ở hai đầu và xử lý giá trị null.
     */
    private String cleanText(String text) {
        return text == null ? "" : text.trim();
    }
}