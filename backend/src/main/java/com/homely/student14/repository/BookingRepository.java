package com.homely.student14.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homely.student14.entity.Booking;
import com.homely.student14.entity.Listing;
import com.homely.student14.entity.Payment;
import com.homely.student14.entity.Room;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class BookingRepository {
    private final DatabaseRepository databaseRepository;
    private final ObjectMapper objectMapper;

    private static final String BOOKING_DETAILS =
            """
            SELECT b.*, l.title AS room_title, r.address_line AS room_address,
                   r.max_occupants, tenant.full_name AS tenant_name,
                   host.full_name AS host_name
            FROM bookings b
            JOIN rooms r ON r.id = b.room_id
            JOIN listings l ON l.id = b.listing_id
            JOIN users tenant ON tenant.id = b.tenant_id
            JOIN users host ON host.id = b.host_id
            """;

    public List<Map<String, Object>> findByUserId(long userId, boolean host) {
        String userColumn = host ? "b.host_id" : "b.tenant_id";
        return databaseRepository.all(
                BOOKING_DETAILS + "WHERE " + userColumn + " = ? ORDER BY b.id DESC", userId);
    }

    public Map<String, Object> findDetailsById(long bookingId) {
        Map<String, Object> booking =
                databaseRepository.one(BOOKING_DETAILS + "WHERE b.id = ?", bookingId);
        booking.put("terms_snapshot", databaseRepository.decode(booking.get("terms_snapshot")));
        return booking;
    }

    public Booking toBooking(Map<String, Object> row) {
        return objectMapper.convertValue(row, Booking.class);
    }

    public Booking findById(long bookingId) {
        return toBooking(databaseRepository.one("SELECT * FROM bookings WHERE id = ?", bookingId));
    }

    public Booking findByIdForUpdate(long bookingId) {
        // Always lock room before booking, including approve, payment and expiry.
        Map<String, Object> booking =
                databaseRepository.one("SELECT room_id FROM bookings WHERE id = ?", bookingId);
        databaseRepository.one(
                "SELECT id FROM rooms WHERE id = ? FOR UPDATE", booking.get("room_id"));
        return toBooking(
                databaseRepository.one(
                        "SELECT * FROM bookings WHERE id = ? FOR UPDATE", bookingId));
    }

    public Room findRoomForUpdate(long roomId) {
        Map<String, Object> room =
                databaseRepository.one("SELECT * FROM rooms WHERE id = ? FOR UPDATE", roomId);
        return objectMapper.convertValue(room, Room.class);
    }

    public Listing findPublishedListing(long roomId) {
        Map<String, Object> listing =
                databaseRepository.one(
                        """
                        SELECT * FROM listings
                        WHERE room_id = ? AND status = 'PUBLISHED'
                          AND (expires_at IS NULL OR expires_at > UTC_TIMESTAMP(3))
                        """,
                        roomId);
        return objectMapper.convertValue(listing, Listing.class);
    }

    public long create(Booking booking, Object termsSnapshot) {
        return databaseRepository.insert(
                """
                INSERT INTO bookings (
                    tenant_id, host_id, room_id, listing_id, occupant_count,
                    rent_vnd, deposit_vnd, terms_snapshot, request_expires_at,
                    desired_move_in, note
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?,
                          DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 24 HOUR), ?, ?)
                """,
                booking.getTenantId(),
                booking.getHostId(),
                booking.getRoomId(),
                booking.getListingId(),
                booking.getOccupantCount(),
                booking.getRentVnd(),
                booking.getDepositVnd(),
                databaseRepository.encode(termsSnapshot),
                Date.valueOf(booking.getDesiredMoveIn()),
                booking.getNote());
    }

    public String findRoomAvailability(long roomId) {
        return databaseRepository
                .one("SELECT availability FROM rooms WHERE id = ?", roomId)
                .get("availability")
                .toString();
    }

    public void approve(long bookingId) {
        databaseRepository.update(
                """
                UPDATE bookings
                SET status = 'APPROVED',
                    hold_expires_at = DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 24 HOUR),
                    version = version + 1
                WHERE id = ?
                """,
                bookingId);
    }

    public void updateRoomAvailability(long roomId, String availability) {
        databaseRepository.update(
                """
                UPDATE rooms SET availability = ?, version = version + 1 WHERE id = ?
                """,
                availability,
                roomId);
    }

    public void cancel(long bookingId, String reason, String reasonCode) {
        databaseRepository.update(
                """
                UPDATE bookings
                SET status = 'CANCELLED', cancelled_at = UTC_TIMESTAMP(3),
                    last_reason = ?, cancel_reason_code = ?, version = version + 1
                WHERE id = ?
                """,
                reason,
                reasonCode,
                bookingId);
        databaseRepository.update(
                """
                UPDATE payments SET status = 'FAILED', failure_code = 'BOOKING_CANCELLED'
                WHERE booking_id = ? AND status = 'PENDING'
                """,
                bookingId);
    }

    public void confirmDeposit(long bookingId) {
        databaseRepository.update(
                """
                UPDATE bookings
                SET status = 'CONFIRMED', tenant_terms_accepted_at = UTC_TIMESTAMP(3),
                    handover_due_at = DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 7 DAY),
                    version = version + 1
                WHERE id = ?
                """,
                bookingId);
    }

    public void confirmHandover(long bookingId, boolean host) {
        String confirmedAt = host ? "host_handover_confirmed_at" : "tenant_handover_confirmed_at";
        databaseRepository.update(
                "UPDATE bookings SET "
                        + confirmedAt
                        + " = COALESCE("
                        + confirmedAt
                        + ", UTC_TIMESTAMP(3)), version = version + 1 WHERE id = ?",
                bookingId);
    }

    public void complete(long bookingId) {
        databaseRepository.update(
                """
                UPDATE bookings SET status = 'COMPLETED', completed_at = UTC_TIMESTAMP(3)
                WHERE id = ?
                """,
                bookingId);
    }

    public Map<String, Object> findPaymentDetails(long paymentId) {
        return databaseRepository.one("SELECT * FROM payments WHERE id = ?", paymentId);
    }

    public Payment findPayment(long paymentId, boolean forUpdate) {
        String query = "SELECT * FROM payments WHERE id = ?" + (forUpdate ? " FOR UPDATE" : "");
        return objectMapper.convertValue(databaseRepository.one(query, paymentId), Payment.class);
    }

    public void expirePendingPayments(long bookingId) {
        databaseRepository.update(
                """
                UPDATE payments SET status = 'EXPIRED', failure_code = 'PAYMENT_TIMEOUT'
                WHERE booking_id = ? AND status = 'PENDING' AND expires_at <= UTC_TIMESTAMP(3)
                """,
                bookingId);
    }

    public List<Map<String, Object>> findReusablePayments(long bookingId) {
        return databaseRepository.all(
                """
SELECT * FROM payments WHERE booking_id = ?
  AND (status = 'SUCCEEDED' OR (status = 'PENDING' AND expires_at > UTC_TIMESTAMP(3)))
ORDER BY id DESC
""",
                bookingId);
    }

    public long createPayment(Booking booking) {
        databaseRepository.update(
                """
                UPDATE bookings SET tenant_terms_accepted_at = UTC_TIMESTAMP(3) WHERE id = ?
                """,
                booking.getId());
        return databaseRepository.insert(
                """
INSERT INTO payments (booking_id, payer_id, amount_vnd, expires_at, provider_payment_id)
SELECT id, tenant_id, deposit_vnd,
       LEAST(DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 30 MINUTE), hold_expires_at), ?
FROM bookings WHERE id = ?
""",
                "MOCK_" + UUID.randomUUID(),
                booking.getId());
    }

    public void updatePaymentResult(long paymentId, boolean succeeded) {
        databaseRepository.update(
                """
                UPDATE payments
                SET status = ?, succeeded_at = IF(?, UTC_TIMESTAMP(3), NULL), failure_code = ?
                WHERE id = ?
                """,
                succeeded ? "SUCCEEDED" : "FAILED",
                succeeded,
                succeeded ? null : "MOCK_FAILED",
                paymentId);
    }

    public void allocatePayment(long bookingId, long paymentId) {
        databaseRepository.update(
                """
UPDATE bookings
SET allocated_payment_id = ?, status = 'CONFIRMED',
    handover_due_at = DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 7 DAY), version = version + 1
WHERE id = ?
""",
                paymentId,
                bookingId);
    }

    public List<Map<String, Object>> findPaymentsByBookingId(long bookingId) {
        return databaseRepository.all(
                "SELECT * FROM payments WHERE booking_id = ? ORDER BY id DESC", bookingId);
    }

    public Map<String, Object> createReview(Booking booking, int rating, String comment) {
        long reviewId =
                databaseRepository.insert(
                        """
                        INSERT INTO reviews (booking_id, reviewer_id, room_id, rating, comment)
                        VALUES (?, ?, ?, ?, ?)
                        """,
                        booking.getId(),
                        booking.getTenantId(),
                        booking.getRoomId(),
                        rating,
                        comment);
        return databaseRepository.one("SELECT * FROM reviews WHERE id = ?", reviewId);
    }

    public Object readFees(Listing listing) {
        return databaseRepository.decode(listing.getFees());
    }

    public void createNotification(
            long userId, String type, String title, String message, long bookingId) {
        databaseRepository.notify(userId, type, title, message, "booking", bookingId);
    }
}
