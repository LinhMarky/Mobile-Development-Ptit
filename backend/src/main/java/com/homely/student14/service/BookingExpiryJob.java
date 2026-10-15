package com.homely.student14.service;

import com.homely.student14.repository.BookingExpiryRepository;
import com.homely.student14.repository.CatalogRepository;
import com.homely.student14.repository.DatabaseRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class BookingExpiryJob {
    private final BookingExpiryRepository bookingExpiryRepository;
    private final CatalogRepository catalogRepository;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void expire() {
        List<Map<String, Object>> candidates =
                bookingExpiryRepository.findExpiredBookingCandidates();
        for (Map<String, Object> candidate : candidates) {
            long roomId = DatabaseRepository.id(candidate.get("room_id"));
            long bookingId = DatabaseRepository.id(candidate.get("id"));

            // Khóa phòng trước booking, cùng thứ tự với duyệt, hủy và thanh toán.
            bookingExpiryRepository.lockRoom(roomId);
            Map<String, Object> booking = bookingExpiryRepository.lockBooking(bookingId);
            String status = booking.get("status").toString();

            // Kiểm tra lại sau khi lấy khóa vì giao dịch khác có thể vừa xử lý booking.
            boolean requestExpired =
                    "PENDING".equals(status) && isDeadlineExpired(booking, "request_expires_at");
            boolean holdExpired =
                    "APPROVED".equals(status) && isDeadlineExpired(booking, "hold_expires_at");
            if (!requestExpired && !holdExpired) {
                continue;
            }

            String reasonCode = requestExpired ? "REQUEST_EXPIRED" : "HOLD_EXPIRED";
            bookingExpiryRepository.markBookingExpired(bookingId, reasonCode);
            if (holdExpired) {
                bookingExpiryRepository.releaseRoom(DatabaseRepository.id(booking.get("room_id")));
            }
            bookingExpiryRepository.expirePendingPaymentsForBooking(bookingId);
        }
        bookingExpiryRepository.expirePaymentSessions();
        catalogRepository.expirePublishedListings();
    }

    private boolean isDeadlineExpired(Map<String, Object> booking, String fieldName) {
        Object deadline = booking.get(fieldName);
        return deadline == null || !Instant.parse(deadline.toString()).isAfter(Instant.now());
    }
}
