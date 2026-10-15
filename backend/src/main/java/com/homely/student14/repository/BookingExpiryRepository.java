package com.homely.student14.repository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class BookingExpiryRepository {
    private final DatabaseRepository databaseRepository;

    public List<Map<String, Object>> findExpiredBookingCandidates() {
        return databaseRepository.all(
                """
                SELECT id, room_id FROM bookings
                WHERE (status = 'PENDING' AND request_expires_at <= UTC_TIMESTAMP(3))
                   OR (status = 'APPROVED' AND hold_expires_at <= UTC_TIMESTAMP(3))
                ORDER BY room_id, id
                """);
    }

    public void lockRoom(long roomId) {
        databaseRepository.one(
                """
                SELECT id FROM rooms WHERE id = ? FOR UPDATE
                """,
                roomId);
    }

    public Map<String, Object> lockBooking(long bookingId) {
        return databaseRepository.one(
                """
                SELECT * FROM bookings WHERE id = ? FOR UPDATE
                """,
                bookingId);
    }

    public void markBookingExpired(long bookingId, String reasonCode) {
        databaseRepository.update(
                """
                UPDATE bookings
                SET status = 'EXPIRED', cancel_reason_code = ?, version = version + 1
                WHERE id = ?
                """,
                reasonCode,
                bookingId);
    }

    public void releaseRoom(long roomId) {
        databaseRepository.update(
                """
                UPDATE rooms SET availability = 'AVAILABLE' WHERE id = ?
                """,
                roomId);
    }

    public void expirePendingPaymentsForBooking(long bookingId) {
        databaseRepository.update(
                """
                UPDATE payments SET status = 'EXPIRED', failure_code = 'BOOKING_EXPIRED'
                WHERE booking_id = ? AND status = 'PENDING'
                """,
                bookingId);
    }

    public void expirePaymentSessions() {
        databaseRepository.update(
                """
                UPDATE payments SET status = 'EXPIRED', failure_code = 'SESSION_EXPIRED'
                WHERE status = 'PENDING' AND expires_at <= UTC_TIMESTAMP(3)
                """);
    }
}
