package com.homely.student14.repository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class ViewingRepository {
    private static final String SELECT_VIEWING =
            """
            SELECT v.*, r.unit_code, r.name AS room_name, r.address_line, r.room_type,
                   r.area_m2, r.availability, COALESCE(l.title, r.name) AS title,
                   l.id AS listing_id, COALESCE(l.rent_vnd, 1) AS rent_vnd,
                   COALESCE(l.deposit_vnd, 0) AS deposit_vnd, u.full_name AS tenant_name
            FROM viewings v
            JOIN rooms r ON r.id = v.room_id
            LEFT JOIN listings l ON l.room_id = r.id
            LEFT JOIN users u ON u.id = v.tenant_id
            """;

    private final DatabaseRepository databaseRepository;

    public List<Map<String, Object>> findAppointmentsByHost(long hostId) {
        return databaseRepository.all(
                SELECT_VIEWING
                        + """
                          WHERE v.host_id = ? AND v.tenant_id IS NOT NULL ORDER BY v.start_at DESC
                          """,
                hostId);
    }

    public List<Map<String, Object>> findAppointmentsByTenant(long tenantId) {
        return databaseRepository.all(
                SELECT_VIEWING
                        + """
WHERE v.tenant_id = ? AND v.tenant_id IS NOT NULL ORDER BY v.start_at DESC
""",
                tenantId);
    }

    public List<Map<String, Object>> findHostSlots(long roomId) {
        return databaseRepository.all(
                SELECT_VIEWING
                        + """
                          WHERE v.room_id = ? AND v.tenant_id IS NULL ORDER BY v.start_at
                          """,
                roomId);
    }

    public List<Map<String, Object>> findAvailableSlots(long roomId) {
        return databaseRepository.all(
                SELECT_VIEWING
                        + """
WHERE v.room_id = ? AND v.status = 'OPEN' AND v.start_at > UTC_TIMESTAMP(3)
  AND l.status = 'PUBLISHED' AND r.availability = 'AVAILABLE'
  AND (l.expires_at IS NULL OR l.expires_at > UTC_TIMESTAMP(3))
ORDER BY v.start_at
""",
                roomId);
    }

    public boolean hasOverlappingViewing(long roomId, Instant startAt, Instant endAt) {
        List<Map<String, Object>> overlappingViewings =
                databaseRepository.all(
                        """
                        SELECT id FROM viewings
                        WHERE room_id = ? AND status IN ('OPEN', 'REQUESTED', 'CONFIRMED')
                          AND start_at < ? AND end_at > ?
                        """,
                        roomId,
                        Timestamp.from(endAt),
                        Timestamp.from(startAt));
        return !overlappingViewings.isEmpty();
    }

    public long createViewing(long roomId, long hostId, Instant startAt, Instant endAt) {
        return databaseRepository.insert(
                """
                INSERT INTO viewings(room_id, host_id, start_at, end_at) VALUES (?, ?, ?, ?)
                """,
                roomId,
                hostId,
                Timestamp.from(startAt),
                Timestamp.from(endAt));
    }

    public Map<String, Object> findViewingById(long viewingId) {
        return databaseRepository.one(
                SELECT_VIEWING
                        + """
                          WHERE v.id = ?
                          """,
                viewingId);
    }

    public long findRoomIdByViewingId(long viewingId) {
        Map<String, Object> viewing =
                databaseRepository.one(
                        """
                        SELECT room_id FROM viewings WHERE id = ?
                        """,
                        viewingId);
        return DatabaseRepository.id(viewing.get("room_id"));
    }

    public Map<String, Object> lockRoom(long roomId) {
        return databaseRepository.one(
                """
                SELECT * FROM rooms WHERE id = ? FOR UPDATE
                """,
                roomId);
    }

    public Map<String, Object> lockViewing(long viewingId) {
        return databaseRepository.one(
                """
                SELECT * FROM viewings WHERE id = ? FOR UPDATE
                """,
                viewingId);
    }

    public void checkPublishedListing(long roomId) {
        databaseRepository.one(
                """
                SELECT id FROM listings
                WHERE room_id = ? AND status = 'PUBLISHED'
                  AND (expires_at IS NULL OR expires_at > UTC_TIMESTAMP(3))
                """,
                roomId);
    }

    public void requestViewing(long viewingId, long tenantId, String note) {
        databaseRepository.update(
                """
                UPDATE viewings
                SET tenant_id = ?, status = 'REQUESTED', requested_at = UTC_TIMESTAMP(3),
                    note = ?, version = version + 1
                WHERE id = ?
                """,
                tenantId,
                note,
                viewingId);
    }

    public void reopenViewing(long viewingId) {
        databaseRepository.update(
                """
UPDATE viewings
SET status = 'OPEN', cancelled_reason = NULL, cancel_reason_code = NULL, version = version + 1
WHERE id = ?
""",
                viewingId);
    }

    public void updateStatus(long viewingId, String status, String reason, String reasonCode) {
        databaseRepository.update(
                """
                UPDATE viewings
                SET status = ?, cancelled_reason = ?, cancel_reason_code = ?, version = version + 1
                WHERE id = ?
                """,
                status,
                reason,
                reasonCode,
                viewingId);
    }

    public void notifyStatusChange(long recipientId, String status, long viewingId) {
        databaseRepository.notify(
                recipientId,
                "VIEWING_STATUS",
                "Cập nhật lịch xem phòng",
                status,
                "viewing",
                viewingId);
    }
}
